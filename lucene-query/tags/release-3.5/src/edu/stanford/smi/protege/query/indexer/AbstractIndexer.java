package edu.stanford.smi.protege.query.indexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Model;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.query.menu.QueryUIConfiguration.BooleanConfigItem;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLAnonymousClass;
import edu.stanford.smi.protegex.owl.model.RDFResource;

public abstract class AbstractIndexer implements Indexer {
    private static final long serialVersionUID = 1964188756235468692L;

private transient static final Logger log = Log.getLogger(AbstractIndexer.class);

  public enum Status {
    INDEXING, READY, DOWN
  };

  protected Object kbLock;

  private String baseIndexPath;

  private Analyzer analyzer;
  private NarrowFrameStore delegate;
  private Status status = Status.INDEXING;
  private Future<Boolean> optimizationTask;

  private Set<Slot> searchableSlots;
  private Set<String> indexableFrameTypes;

  boolean owlMode = false;

  private Slot nameSlot;

    public static final String FRAME_NAME = "frameName";
    public static final String SLOT_NAME = "slotName";
    public static final String BROWSER_TEXT = "browserText";
    public static final String BROWSER_TEXT_PRESENT = "browserTextPresent";
    public static final String CONTENTS_FIELD = "contents";
    public static final String LITERAL_CONTENTS = "literalContents";

    public static final String FRAME_TYPE = "frameType";
    public static final String FRAME_TYPE_CLS = "c";
    public static final String FRAME_TYPE_SLOT = "s";
    public static final String FRAME_TYPE_INSTANCE = "i";

  private transient ExecutorService indexRunner = Executors.newSingleThreadExecutor(new ThreadFactory() {
      public Thread newThread(Runnable r) {
          Thread thread = new Thread(r, "Lucene Query Thread");
          thread.setDaemon(true);
          return thread;
      }
  });


  public AbstractIndexer() {
      analyzer = createAnalyzer();
  }

  public void dispose() {
      indexRunner.shutdown();
      boolean done = false;
      while (!done) {
          try {
              done = indexRunner.awaitTermination(10, TimeUnit.SECONDS);
          }
          catch (InterruptedException ie) {
              log.warning("Strange interrupt");
          }
          if (!done) {
              log.info("Dispose of Lucene Query Search Index waiting for query tasks to complete");
          }
      }
  }

  public void setSearchableSlots(Set<Slot> searchableSlots) {
      this.searchableSlots = searchableSlots;
  }

  public void setBaseIndexPath(String baseIndexPath) {
      this.baseIndexPath = baseIndexPath;
  }

  public String getFullIndexPath() {
      return baseIndexPath + File.separator + relativeIndexLocation();
  }

  public void setIndexedFrameTypes(Set<String> indexableFrameTypes) {
    this.indexableFrameTypes = indexableFrameTypes;
  }

  public void setOWLMode(boolean owlMode) {
      this.owlMode = owlMode;
  }

  public void setNarrowFrameStoreDelegate(NarrowFrameStore delegate) {
      this.delegate = delegate;
      nameSlot = (Slot) delegate.getFrame(Model.SlotID.NAME);
  }

  public void setKnowledgeBase(KnowledgeBase kb) {
      kbLock = kb;
  }

  protected Analyzer getAnalyzer() {
      return analyzer;
  }

  protected ExecutorService getIndexRunner() {
      return indexRunner;
  }

  public Status getStatus() {
      return status;
  }

  protected abstract Analyzer createAnalyzer();

  protected IndexWriter openWriter(boolean create) throws IOException {
    return new IndexWriter(getFullIndexPath(), analyzer, create);
  }



  public void indexOntologies() throws ProtegeException {
      Future<Boolean> indexTask = indexRunner.submit(new IndexOntologiesRunner(), true);
      try {
          indexTask.get();
      } catch (ExecutionException ee) {
          throw new RuntimeException(ee);
      } catch (InterruptedException interrupt) {
          throw new RuntimeException(interrupt);
      }
  }

  private class IndexOntologiesRunner implements Runnable {
      public void run() {
          boolean errorsFound = false;
          long start = System.currentTimeMillis();
          log.info("Started indexing ontology with " + searchableSlots.size() + " searchable slots");
          IndexWriter myWriter = null;
          try {
              myWriter = openWriter(true);
              Set<Frame> frames;
              synchronized (kbLock) {
                  frames = delegate.getFrames();
              }
              for (Frame frame : frames) {
                  if (indexable(frame)) {
                      //TODO: not ideal. We make a kb call in another thread, but it may be OK for the indexing
                      errorsFound = errorsFound || !addFrame(myWriter, frame, frame.getBrowserText());
                  }
              }
              myWriter.optimize();
              status = Status.READY;
              log.info("Finished indexing ontology ("
                                   + ((System.currentTimeMillis() - start)/1000) + " seconds)");
          } catch (IOException ioe) {
              died(ioe);
              errorsFound = true;
          } finally {
              forceClose(myWriter);
          }
          if (errorsFound) {  // ToDo - do this *much* better
              throw new ProtegeException("Errors Found - see console log for details");
          }
      }
  }

  public boolean indexable(Frame frame) {
      return    !frame.isSystem() &&
                      !(frame instanceof OWLAnonymousClass) &&
                      isIndexableFrameType(frame);
  }

  private boolean isIndexableFrameType(Frame frame) {
      if (frame instanceof Cls) {
          return indexableFrameTypes.contains(AbstractIndexer.FRAME_TYPE_CLS);
      } else if (frame instanceof Slot) {
          return indexableFrameTypes.contains(AbstractIndexer.FRAME_TYPE_SLOT);
      }
      return indexableFrameTypes.contains(AbstractIndexer.FRAME_TYPE_INSTANCE);
  }

  protected boolean addFrameBrowserText(IndexWriter writer, Frame frame, String newBrowserText) {
      if (status == Status.DOWN  || !isIndexableFrameType(frame)) {
          return false;
        }

      boolean errorsFound = false;
      try {
          Document doc = new Document();
          doc.add(new Field(FRAME_NAME, getFrameName(frame), Field.Store.YES, Field.Index.UN_TOKENIZED));
          doc.add(new Field(BROWSER_TEXT, newBrowserText, Field.Store.YES, Field.Index.TOKENIZED));
          doc.add(new Field(BROWSER_TEXT_PRESENT, Boolean.TRUE.toString(), Field.Store.YES, Field.Index.TOKENIZED));
          doc.add(new Field(FRAME_TYPE, getFrameType(frame), Field.Store.YES, Field.Index.UN_TOKENIZED));
          writer.addDocument(doc);
      } catch (Exception e) {
          log.log(Level.WARNING, "Exception caught indexing ontologies", e);
          log.warning("continuing...");
          errorsFound = true;
      }
      if (log.isLoggable(Level.FINE)) {
          log.fine("Adding frame browser text to index: " + frame.getBrowserText() + " " + frame.getName() + " " + errorsFound);
      }
      return errorsFound;
  }

  @SuppressWarnings("unchecked")
  private boolean addFrame(IndexWriter writer, Frame frame, String browserText) {
      if (status == Status.DOWN  || !isIndexableFrameType(frame)) {
          return false;
        }

      if (log.isLoggable(Level.FINE)) {
          log.fine("Adding to lucene index frame: " + frame.getBrowserText() + " " + frame);
      }

      boolean errorsFound = addFrameBrowserText(writer, frame, browserText);
      for (Slot slot : searchableSlots) {
          try {
              List values;
              if (owlMode && slot.equals(nameSlot) && frame instanceof RDFResource) {
                  String name;
                  synchronized (kbLock) {
                      name = ((RDFResource) frame).getLocalName();
                  }
                  values = Collections.singletonList(name);
              }
              else {
                  synchronized (kbLock) {
                      values = delegate.getValues(frame, slot, null, false);
                  }
              }
              for (Object value : values) {
                  if (!(value instanceof String)) {
                      continue;
                  }
                  addUpdate(writer, frame, slot, (String) value);
              }
          } catch (Exception e) {
              log.log(Level.WARNING, "Exception caught indexing ontologies", e);
              log.warning("continuing...");
              errorsFound = true;
          }
      }
      return !errorsFound;
  }

  protected void addUpdate(IndexWriter writer, Frame frame, Slot slot, String value) throws IOException {
    if (owlMode && value.startsWith("~#")) {
      value = value.substring(5);
    }
    if (status == Status.DOWN || !searchableSlots.contains(slot) || !isIndexableFrameType(frame)) {
      return;
    }
    Document doc = new Document();
    doc.add(new Field(FRAME_NAME, getFrameName(frame), Field.Store.YES, Field.Index.UN_TOKENIZED));
    if (slot != null) {
        doc.add(new Field(SLOT_NAME, getFrameName(slot),
                          Field.Store.YES, Field.Index.UN_TOKENIZED));
    }
    doc.add(new Field(CONTENTS_FIELD, value, Field.Store.YES, Field.Index.TOKENIZED));
    doc.add(new Field(LITERAL_CONTENTS, value, Field.Store.YES, Field.Index.UN_TOKENIZED));
    doc.add(new Field(FRAME_TYPE, getFrameType(frame), Field.Store.YES, Field.Index.UN_TOKENIZED));
    writer.addDocument(doc);

    if (log.isLoggable(Level.FINE)) {
        log.fine("Updating in the Lucene index the values for frame " + frame.getBrowserText() + " name: " + frame.getName() +
                " slot: " + slot + " value: " + value);
    }
  }

  public Collection<Frame> executeQuery(Collection<Slot> slots, String expr) throws IOException {
      return executeQuery(generateLuceneQuery(slots, expr));
  }

  public Collection<Frame> executeQuery(final Query luceneQuery) throws IOException {
      try {
          return indexRunner.submit(new ExecuteQueryCallable(luceneQuery)).get();
      } catch (InterruptedException e) {
          throw new RuntimeException(e);
      } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause != null && cause instanceof IOException) {
              throw (IOException) cause;
          }
          else {
              throw new RuntimeException(e);
          }
      }
  }

  private class ExecuteQueryCallable implements Callable<Collection<Frame>> {
      private Query luceneQuery;

      public ExecuteQueryCallable(Query luceneQuery) {
          this.luceneQuery = luceneQuery;
      }

      public Collection<Frame> call() throws IOException {
          if (status == Status.DOWN) {
              return new ArrayList<Frame>();
          }
          Searcher searcher = null;
          Collection<Frame> results = new LinkedHashSet<Frame>();
          try {
              searcher = new IndexSearcher(getFullIndexPath());
              Hits hits = searcher.search(luceneQuery);
              for (int i = 0; i < hits.length(); i++) {
                  Document doc = hits.doc(i);
                  String frameName = doc.get(FRAME_NAME);
                  synchronized (kbLock) {
                      Frame frame = getFrameByName(frameName);
                      if (frame != null) {
                          results.add(frame);
                      }
                  }
              }
          } finally {
              forceClose(searcher);
          }
          return results;
      }
  }

  private String getFrameType(Frame frame) {
      if (frame instanceof Cls) {
          return FRAME_TYPE_CLS;
      } else if (frame instanceof Slot) {
          return FRAME_TYPE_SLOT;
      }
      return FRAME_TYPE_INSTANCE;
  }


  /*
   * This is one of the queries that is actually used for searching the lucene index
   * rather than just for maintaining it.  It is a slight variation of the query that
   * just searches for a single slot.
   * The Lucene query will only search the frame types configured in the indexer.
   */
  protected Query generateLuceneQuery(Collection<Slot> slots, String expr) throws IOException {
    BooleanQuery query = new  BooleanQuery();
    QueryParser parser = new QueryParser(CONTENTS_FIELD, analyzer);
    parser.setAllowLeadingWildcard(true);
    try {
        query.add(parser.parse(expr), BooleanClause.Occur.MUST);
    } catch (ParseException e) {
        IOException ioe = new IOException(e.getMessage());
        ioe.initCause(e);
        throw ioe;
    }
    if (slots == null || slots.isEmpty()) {
        ;
    }
    else if (slots.size() == 1)  {
        Slot slot = slots.iterator().next();
        Term term = new Term(SLOT_NAME, getFrameName(slot));
        query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    }
    else {
        BooleanQuery slotQuery = new BooleanQuery();
        for (Slot slot : slots) {
            Term term = new Term(SLOT_NAME, getFrameName(slot));
            slotQuery.add(new TermQuery(term), BooleanClause.Occur.SHOULD);
        }
        query.add(slotQuery, BooleanClause.Occur.MUST);
    }

    query.add(getFrameTypeInnerQuery(), BooleanClause.Occur.MUST);
   return query;
  }

  protected Query getFrameTypeInnerQuery() {
	  BooleanQuery query = new BooleanQuery();
	  if (isSearchFrameType(BooleanConfigItem.SEARCH_FOR_CLASSES.getProtegeProperty())) {
		  query.add(new TermQuery(new Term(FRAME_TYPE, AbstractIndexer.FRAME_TYPE_CLS)),
				  BooleanClause.Occur.SHOULD);
	  }
	  if (isSearchFrameType(BooleanConfigItem.SEARCH_FOR_PROPERTIES.getProtegeProperty())) {
		  query.add(new TermQuery(new Term(FRAME_TYPE, AbstractIndexer.FRAME_TYPE_SLOT)),
				  BooleanClause.Occur.SHOULD);
	  }
	  if (isSearchFrameType(BooleanConfigItem.SEARCH_FOR_INDIVIDUALS.getProtegeProperty())) {
		  query.add(new TermQuery(new Term(FRAME_TYPE, AbstractIndexer.FRAME_TYPE_INSTANCE)),
				  BooleanClause.Occur.SHOULD);
	  }
	  return query;
  }

  private boolean isSearchFrameType(String searchTypeProp) {
	  KnowledgeBase kb = (KnowledgeBase)kbLock; //not nice
	  Object val = kb.getProject().getClientInformation(searchTypeProp);
	  if (val == null) { return true; }
	  return (Boolean) val;
  }

  /*
   * This query already contains the frame name to find.  It is used to find a document and thence delete it.
   */
  protected Query generateLuceneQuery(Frame frame, Slot slot, String literalValue) throws IOException {
      BooleanQuery query = new  BooleanQuery();

      Term term = new Term(LITERAL_CONTENTS, literalValue);
      query.add(new TermQuery(term), BooleanClause.Occur.MUST);

      term = new Term(FRAME_NAME, getFrameName(frame));
      query.add(new TermQuery(term), BooleanClause.Occur.MUST);

      term = new Term(SLOT_NAME, getFrameName(slot));
      query.add(new TermQuery(term), BooleanClause.Occur.MUST);
      return query;
    }

  /*
   * This query already contains the name of the document that it is looking for.
   * It is used to find a document so that it can be deleted.
   */
  protected Query generateLuceneQuery(String fname) throws IOException {
    BooleanQuery query  = new  BooleanQuery();

    Term term;
    term = new Term(FRAME_NAME, fname);
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);

    return query;
  }

  /*
   * This query already contains the frame name of the document it is looking for.
   * It is used to find a document so that it can be deleted.
   */
  protected Query generateLuceneQuery(Frame frame, Slot slot) throws IOException {
    BooleanQuery query  = new  BooleanQuery();

    Term term;
    term = new Term(FRAME_NAME, getFrameName(frame));
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    term = new Term(SLOT_NAME, getFrameName(slot));
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    return query;
  }

  protected void died(IOException ioe) {
    log.warning("Search index update failed " + ioe);
    log.warning("This exception will not interfere with normal (non-query) operations");
    log.warning("suggest reindexing to get the lucene indicies back");
    status = Status.DOWN;
  }

  protected void forceClose(Searcher searcher) {
    try {
      if (searcher != null) {
        searcher.close();
      }
    } catch (IOException ioe) {
      log.log(Level.WARNING, "Exception caught closing files involved during search", ioe);
    }
  }

  private void forceClose(IndexReader reader) {
      try {
          if (reader != null) {
              reader.close();
          }
      } catch (IOException ioe) {
          log.log(Level.WARNING, "Exception caught reading/deleting documents from index", ioe);
      }
  }

  protected void forceClose(IndexWriter writer) {
    try {
      if (writer != null) {
        writer.close();
      }
    } catch (IOException ioe) {
      log.log(Level.WARNING, "Exception caught closing files involved in lucene indicies", ioe);
    }
  }

  private String getFrameName(Frame frame) {
	  return frame.getFrameID().getName();
  }

  private Frame getFrameByName(String name) {
    Set<Frame> frames;
    synchronized (kbLock) {
        frames = delegate.getFrames(nameSlot, null, false, name);
    }
    return frames != null && !frames.isEmpty() ? frames.iterator().next() : null;
  }

  private boolean isAnonymous(Frame frame) {
    if (!owlMode) {
      return false;
    }

    String name = getFrameName(frame);
    if (name == null) {
      return true;
    }
    return name.startsWith("@");
  }

  private void installOptimizeTask() {
      if (optimizationTask != null) {
          optimizationTask.cancel(false);
      }
      optimizationTask = indexRunner.submit(new Runnable() {
          public void run() {
              IndexWriter myWriter = null;
              try {
                  myWriter = openWriter(false);
                  myWriter.optimize();
              } catch (IOException e) {
                  died(e);
              } finally {
                  forceClose(myWriter);
              }
          }
      }, true);
  }


  protected void deleteDocuments(Query q) throws IOException {
      List<Integer> deletions = new ArrayList<Integer>();
      IndexSearcher searcher = null;
      long start = System.currentTimeMillis();
      try {
          searcher = new IndexSearcher(getFullIndexPath());
          Hits hits;
          hits = searcher.search(q);
          for (int i = 0; i < hits.length(); i++) {
              deletions.add(hits.id(i));
              // it is not clear what the right method is to delete documents?
              // searcher.getIndexReader().deleteDocument(hits.id(i));
          }
      } finally {
          forceClose(searcher);
      }
      IndexReader reader = null;
      try {
          reader = IndexReader.open(getFullIndexPath());
          for (Integer i : deletions) {
              reader.deleteDocument(i);
          }

      } catch (Exception e) {
          if (log.isLoggable(Level.FINE)) {
              log.log(Level.FINE, "Exception at deleting document from index " + q, e);
          }
      }  finally {
          forceClose(reader);
      }
      if (log.isLoggable(Level.FINE)) {
          log.fine("Delete lucene document operation for query " + q  + " took " +
                   (System.currentTimeMillis() - start) + "ms");
      }
  }


  /* --------------------------------------------------------------------------
   * Update Utilities for the Narrow Frame Store
   */

  public void addValues(final Frame frame, final Slot slot, final Collection values, final String browserText) {
      if (status == Status.DOWN || !searchableSlots.contains(slot) || isAnonymous(frame)) {
          return;
      }
      indexRunner.submit(new AddValuesRunnable(frame, slot, values, browserText), true);
      installOptimizeTask();
  }

  private class AddValuesRunnable implements Runnable {
      private Frame frame;
      private Slot slot;
      private Collection values;
      private String browserText;

      public AddValuesRunnable(final Frame frame,
                               final Slot slot,
                               final Collection values,
                               final String browserText) {
          this.frame = frame;
          this.slot = slot;
          this.values = values;
          this.browserText = browserText;
      }

      public void run() {
          if (status == Status.DOWN || !searchableSlots.contains(slot) || isAnonymous(frame)) {
              return;
          }
          if (log.isLoggable(Level.FINE)) {
              log.fine("Adding values for frame named " + frame.getName() + " and slot " + slot.getName() + " values: " + values);
          }
          IndexWriter writer = null;
          try {
              long start = System.currentTimeMillis();
              writer = openWriter(false);
              if (slot.equals(nameSlot)) {
                  addFrame(writer, frame, browserText);
                  return;
              }
              for (Object value : values) {
                  if (value instanceof String) {
                      addUpdate(writer, frame, slot, (String) value);
                  }
              }
              if (log.isLoggable(Level.FINE)) {
                  log.fine("Adding " + values.size() + " values in " + (System.currentTimeMillis() - start) + "ms");
              }
          } catch (IOException ioe) {
              died(ioe);
          } catch (Throwable t) {
              log.warning("Error during updating the Lucene index (add values)" + t);
          } finally {
              forceClose(writer);
          }
      }
  }

  public void removeValue(final Frame frame, final Slot slot, final Object value) {
      if (status == Status.DOWN || !searchableSlots.contains(slot) || !(value instanceof String)) {
          return;
      }
      indexRunner.submit(new RemoveValueRunnable(frame, slot, value) , true);
      installOptimizeTask();
  }

  private class RemoveValueRunnable implements Runnable {
      private Frame frame;
      private Slot slot;
      private Object value;

      public RemoveValueRunnable(final Frame frame,
                                 final Slot slot,
                                 final Object value) {
          this.frame = frame;
          this.slot = slot;
          this.value = value;
      }

      public void run() {
          if (status == Status.DOWN || !searchableSlots.contains(slot) || !(value instanceof String)) {
              return;
          }
          if (log.isLoggable(Level.FINE)) {
              log.fine("Removing value " + value + " for frame " + frame.getName() + " and slot " + slot.getName());
          }
          try {
              deleteDocuments(generateLuceneQuery(frame, slot, (String) value));
          } catch (IOException ioe) {
              died(ioe);
          } catch (Throwable t) {
              log.warning("Error during updating the Lucene index (remove value)" + t);
          }
      }
  }


  public void removeValues(final Frame frame, final Slot slot) {
      if (status == Status.DOWN || !searchableSlots.contains(slot)) {
          return;
      }
      indexRunner.submit(new RemoveValuesRunnable(frame, slot) , true);
      installOptimizeTask();
  }

  private class RemoveValuesRunnable implements Runnable {
      private Frame frame;
      private Slot slot;

      public RemoveValuesRunnable(final Frame frame, final Slot slot) {
          this.frame = frame;
          this.slot = slot;
      }
      public void run() {
          if (status == Status.DOWN || !searchableSlots.contains(slot)) {
              return;
          }
          if (log.isLoggable(Level.FINE)) {
              log.fine("Removing all values for frame " + frame.getName() + " and slot " + slot.getName());
          }
          try {
              Query q = null;
              if (slot.equals(nameSlot)) {
                  String fname = getFrameName(frame);
                  q = generateLuceneQuery(fname);
              }
              else {
                  q = generateLuceneQuery(frame, slot);
              }
              deleteDocuments(q);
          } catch (IOException ioe) {
              died(ioe);
          } catch (Throwable t) {
              log.warning("Error during updating Lucene index (removing all frame values)  " + t);
          }
      }
  }

  public void removeValues(final String fname) {
      if (status == Status.DOWN) {
          return;
      }
      indexRunner.submit(new RemoveValuesByNameRunnable(fname));
      installOptimizeTask();
  }

  private class RemoveValuesByNameRunnable implements Runnable {
      final String fname;

      public RemoveValuesByNameRunnable(final String fname) {
          this.fname = fname;
      }

      public void run() {
          if (status == Status.DOWN) {
              return;
          }
          if (log.isLoggable(Level.FINE)) {
              log.fine("Removing all values for frame named " + fname);
          }
          try {
              deleteDocuments(generateLuceneQuery(fname));
          } catch (IOException ioe) {
              died(ioe);
          } catch (Throwable t) {
              log.warning("Error at updating the Lucene index (remove all frame values)  " + t);
          }
      }
  }


  public void localize(KnowledgeBase kb) {
      kbLock = kb;
      analyzer = createAnalyzer();
  }

}
