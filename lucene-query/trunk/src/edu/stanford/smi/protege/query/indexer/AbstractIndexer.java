package edu.stanford.smi.protege.query.indexer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
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
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Localizable;
import edu.stanford.smi.protege.model.Model;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.query.util.FutureTask;
import edu.stanford.smi.protege.query.util.IndexTaskRunner;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.RDFResource;

public abstract class AbstractIndexer implements Indexer, Localizable, Serializable {
    private static final long serialVersionUID = 1964188756235468692L;

private transient static final Logger log = Log.getLogger(AbstractIndexer.class);
  
  private enum Status {
    INDEXING, READY, DOWN
  };
  
  private Object kbLock;
  
  private String baseIndexPath;
  
  private Analyzer analyzer;
  private NarrowFrameStore delegate;
  private Status status = Status.INDEXING;
  
  private Set<Slot> searchableSlots;
  
  boolean owlMode = false;
  
  private Slot nameSlot;
  
  private static final String FRAME_NAME                = "frameName";
  private static final String SLOT_NAME                 = "slotName";
  private static final String CONTENTS_FIELD            = "contents";
  private static final String LITERAL_CONTENTS          = "literalContents";
  
  private IndexTaskRunner indexRunner = new IndexTaskRunner();

  
  public AbstractIndexer() {
    analyzer = createAnalyzer();
    indexRunner.startBackgroundThread();
  }
  
  public void dispose() {
      indexRunner.dispose();
  }
  
  public void setSearchableSlots(Set<Slot> searchableSlots) {
      this.searchableSlots = searchableSlots;
  }
  
  public void setBaseIndexPath(String baseIndexPath) {
      this.baseIndexPath = baseIndexPath;
  }
  
  private String getFullIndexPath() {
      return baseIndexPath + File.separator + relativeIndexLocation();
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
  

  
  protected abstract Analyzer createAnalyzer();

  private IndexWriter openWriter(boolean create) throws IOException {
    return new IndexWriter(getFullIndexPath(), analyzer, create);
  }
  

  
  @SuppressWarnings("unchecked")
  public void indexOntologies() throws ProtegeException {
      FutureTask indexTask = new FutureTask() {
          public void run() {
              boolean errorsFound = false;
              long start = System.currentTimeMillis();
              Log.getLogger().info("Started indexing ontology with " + searchableSlots.size() + " searchable slots");
              IndexWriter myWriter = null;
              try {
                  myWriter = openWriter(true);
                  Set<Frame> frames;
                  synchronized (kbLock) {
                      frames = delegate.getFrames();
                  }
                  for (Frame frame : frames) {
                    errorsFound = errorsFound || !addFrame(myWriter, frame);
                  }
                  myWriter.optimize();
                  status = Status.READY;
                  Log.getLogger().info("Finished indexing ontology (" 
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
      };
      indexRunner.addTask(indexTask);
      try {
          indexTask.get();
      } catch (ExecutionException ee) {
          throw new RuntimeException(ee);
      } catch (InterruptedException interrupt) {
          throw new RuntimeException(interrupt);
      }
  }
  
  @SuppressWarnings("unchecked")
  private boolean addFrame(IndexWriter writer, Frame frame) {
      boolean errorsFound = false;
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
              Log.getLogger().log(Level.WARNING, "Exception caught indexing ontologies", e);
              Log.getLogger().warning("continuing...");
              errorsFound = true;
          }
      }
      return !errorsFound;
  }
  
  protected void addUpdate(IndexWriter writer, Frame frame, Slot slot, String value) throws IOException {
    if (owlMode && value.startsWith("~#")) {
      value = value.substring(5);
    }
    if (status == Status.DOWN || !searchableSlots.contains(slot)) {
      return;
    }
    Document doc = new Document();
    doc.add(new Field(FRAME_NAME, getFrameName(frame), 
                      Field.Store.YES, Field.Index.UN_TOKENIZED));
    doc.add(new Field(SLOT_NAME, getFrameName(slot),
                      Field.Store.YES, Field.Index.UN_TOKENIZED));
    doc.add(new Field(CONTENTS_FIELD, value, Field.Store.YES, Field.Index.TOKENIZED));
    doc.add(new Field(LITERAL_CONTENTS, value, Field.Store.YES, Field.Index.UN_TOKENIZED));
    writer.addDocument(doc);
  }

  @SuppressWarnings("unchecked")
  public Set<Frame> executeQuery(final Slot slot, final String expr) throws IOException {
      FutureTask queryTask = new FutureTask() {
          public void run() {
              if (status == Status.DOWN) {
                  set(null);
              }
              Searcher searcher = null;
              Set<Frame> results = new HashSet<Frame>();
              try {
                  Query luceneQuery = generateLuceneQuery(slot, expr);
                  searcher = new IndexSearcher(getFullIndexPath());
                  Hits hits = searcher.search(luceneQuery);
                  for (int i = 0; i < hits.length(); i++) {
                      Document doc = hits.doc(i);
                      String frameName = doc.get(FRAME_NAME);
                      synchronized (kbLock) {
                          results.add(getFrameByName(frameName));
                      }
                  }
              } catch (IOException ioe) {
                  setException(ioe);
              } finally {
                  forceClose(searcher);
              }
              set(results);
          }
      };

      try {
          indexRunner.addTask(queryTask);
          return (Set<Frame>) queryTask.get();
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
  
  protected Query generateLuceneQuery(Slot slot, String expr) throws IOException {
    String contents    = "" + expr;
    /* alternative implementation at svn revision 11371 */
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
    Term term = new Term(SLOT_NAME, getFrameName(slot));
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    return query;
  }
 
  
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
  
  protected Query generateLuceneQuery(String fname) throws IOException {
    BooleanQuery query  = new  BooleanQuery();
    
    Term term;
    term = new Term(FRAME_NAME, fname);
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    
    return query;
  }
  
  protected Query generateLuceneQuery(Frame frame, Slot slot) throws IOException {
    BooleanQuery query  = new  BooleanQuery();
    
    Term term;
    term = new Term(FRAME_NAME, getFrameName(frame));
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    term = new Term(SLOT_NAME, getFrameName(slot));
    query.add(new TermQuery(term), BooleanClause.Occur.MUST);
    return query;
  }
  
  private void died(IOException ioe) {
    Log.getLogger().warning("Search index update failed " + ioe);
    Log.getLogger().warning("This exception will not interfere with normal (non-query) operations");
    Log.getLogger().warning("suggest reindexing to get the lucene indicies back");
    status = Status.DOWN;
  }
  
  private void forceClose(Searcher searcher) {
    try {
      if (searcher != null) {
        searcher.close();
      }
    } catch (IOException ioe) {
      Log.getLogger().log(Level.WARNING, "Exception caught closing files involved during search", ioe);
    }
  }
  
  private void forceClose(IndexReader reader) {
      try {
          if (reader != null) {
              reader.close();
          }
      } catch (IOException ioe) {
          Log.getLogger().log(Level.WARNING, "Exception caught reading/deleting documents from index", ioe);
      }
  }
  
  private void forceClose(IndexWriter writer) {
    try {
      if (writer != null) {
        writer.close();
      }
    } catch (IOException ioe) {
      Log.getLogger().log(Level.WARNING, "Exception caught closing files involved in lucene indicies", ioe);
    }
  }
  
  private String getFrameName(Frame frame) {
    Collection values = delegate.getValues(frame, nameSlot, null, false);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return (String) values.iterator().next();
  }
  
  private Frame getFrameByName(String name) {
    Set<Frame> frames = delegate.getFrames(nameSlot, null, false, name);
    return frames.iterator().next();
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
      indexRunner.setCleanUpTask(new Runnable() {
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
      });
  }
  
  private void deleteDocuments(Query q) throws IOException {
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
      } finally {
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
  
  public void addValues(final Frame frame, final Slot slot, final Collection values) { 
      if (status == Status.DOWN || !searchableSlots.contains(slot) || isAnonymous(frame)) {
          return;
      }
      indexRunner.addTask(new FutureTask() {
          public void run() {
              if (status == Status.DOWN || !searchableSlots.contains(slot) || isAnonymous(frame)) {
                  return;
              }
              if (log.isLoggable(Level.FINER)) {
                  log.finer("Adding values for frame named " + frame.getName() + " and slot " + slot.getName());
              }
              IndexWriter writer = null;
              try {
                  long start = System.currentTimeMillis();
                  writer = openWriter(false);
                  if (slot.equals(nameSlot)) {
                      addFrame(writer, frame);
                      return;
                  }
                  for (Object value : values) {
                      if (value instanceof String) {
                          addUpdate(writer, frame, slot, (String) value);
                      }
                  }
                  if (log.isLoggable(Level.FINE)) {
                      log.fine("updated " + values.size() + " values in " + (System.currentTimeMillis() - start) + "ms");
                  }
              } catch (IOException ioe) {
                  died(ioe);
              } catch (Throwable t) {
                  Log.getLogger().warning("Error during indexing" + t);
              } finally {
                  forceClose(writer);
              }
          }
      });
      installOptimizeTask();
  }
  
  public void removeValue(final Frame frame, final Slot slot, final Object value) {
      if (status == Status.DOWN || !searchableSlots.contains(slot) || !(value instanceof String)) {
          return;
      }
      indexRunner.addTask(new FutureTask() {
          public void run() {
              if (status == Status.DOWN || !searchableSlots.contains(slot) || !(value instanceof String)) {
                  return;
              }
              if (log.isLoggable(Level.FINER)) {
                  log.finer("Removing value " + value + " for frame " + frame.getName() + " and slot " + slot.getName());
              }
              try {
                  deleteDocuments(generateLuceneQuery(frame, slot, (String) value));
              } catch (IOException ioe) {
                  died(ioe);
              } catch (Throwable t) {
                  Log.getLogger().warning("Exception caught during indexing" + t);
              }
          }
      });
      installOptimizeTask();
  }
  
  public void removeValues(final Frame frame, final Slot slot) {
      if (status == Status.DOWN || !searchableSlots.contains(slot)) {
          return;
      }
      indexRunner.addTask(new FutureTask() {
          public void run() {
              if (status == Status.DOWN || !searchableSlots.contains(slot)) {
                  return;
              }
              if (log.isLoggable(Level.FINER)) {
                  log.finer("Removing all values for frame " + frame.getName() + " and slot " + slot.getName());
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
                  Log.getLogger().warning("Exception caught during indexing " + t);
              } 
          }
      });
      installOptimizeTask();
  }
  
  public void removeValues(final String fname) {
      if (status == Status.DOWN) {
          return;
      }
      indexRunner.addTask(new FutureTask() {
          public void run() {
              if (status == Status.DOWN) {
                  return;
              }
              if (log.isLoggable(Level.FINER)) {
                  log.finer("Removing all values for frame named " + fname);
              }
              try {
                  deleteDocuments(generateLuceneQuery(fname));
              } catch (IOException ioe) {
                  died(ioe);
              } catch (Throwable t) {
                  Log.getLogger().warning("Exception caught during indexing " + t);
              }
          }
      });
      installOptimizeTask();
  }
  
  public void localize(KnowledgeBase kb) {

  }

}
