package edu.stanford.smi.protege.query.indexer;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
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
import org.apache.lucene.search.WildcardQuery;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.query.querytypes.impl.LuceneBrowserTextSearch;
import edu.stanford.smi.protege.query.querytypes.impl.LuceneOwnSlotValueQuery;
import edu.stanford.smi.protege.query.util.FutureTask;
import edu.stanford.smi.protege.util.Log;


public class StdIndexer extends AbstractIndexer {
  
  public StdIndexer() {
    super();
  }

  @Override
  protected Analyzer createAnalyzer() {
    return new StandardAnalyzer();
  }
  
  public Collection<Frame> executeQuery(LuceneOwnSlotValueQuery query) throws IOException {
    return executeQuery(query.getSlots(), query.getExpr());
  }
  
  public Collection<Frame> executeQuery(LuceneBrowserTextSearch q) throws IOException {
      Query query = null;
      QueryParser parser = new QueryParser(BROWSER_TEXT, getAnalyzer());
      parser.setAllowLeadingWildcard(true);
      try {
          query = parser.parse(q.getText());
      } catch (ParseException e) {
          IOException ioe = new IOException(e.getMessage());
          ioe.initCause(e);
          throw ioe;
      }
      return executeQuery(query);
  }
  
  public void browserTextChanged(final Frame frame){
      getIndexRunner().addTask(new FutureTask() {
          public void run() {
              BooleanQuery query  = new  BooleanQuery();

              Term term;
              term = new Term(FRAME_NAME, frame.getName());
              query.add(new TermQuery(term), BooleanClause.Occur.MUST);
              query.add(new WildcardQuery(new Term(BROWSER_TEXT, "*")), BooleanClause.Occur.MUST);
              try {
                  deleteDocuments(query);
              }
              catch (IOException ioe) {
                  died(ioe);
                  return;
              }
              IndexWriter writer = null;
              try {
                  writer = openWriter(false);
                  addFrameBrowserText(writer, frame);
              } catch (IOException ioe) {
                  died(ioe);
              } catch (Throwable t) {
                  Log.getLogger().warning("Error during indexing" + t);
              }
              finally {
                  forceClose(writer);
              }
          }
      });

  }
  
  public Map<String, String> queryBrowserText(String luceneQuery) throws IOException {
      QueryParser parser = new QueryParser(BROWSER_TEXT, getAnalyzer());
      parser.setAllowLeadingWildcard(true);
      FutureTask queryTask;
      try {
          final Query query = parser.parse(luceneQuery);
          queryTask = new FutureTask() {
              public void run() {
                  if (getStatus() == Status.DOWN) {
                      set(null);
                  }
                  Searcher searcher = null;
                  Map<String, String> results = new TreeMap<String, String>();
                  try {
                      searcher = new IndexSearcher(getFullIndexPath());
                      Hits hits = searcher.search(query);
                      for (int i = 0; i < hits.length(); i++) {
                          Document doc = hits.doc(i);
                          String frameName = doc.get(FRAME_NAME);
                          String browserText = doc.get(BROWSER_TEXT);
                          synchronized (kbLock) {
                              results.put(browserText, frameName);
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

      } catch (ParseException e) {
          IOException ioe = new IOException(e.getMessage());
          ioe.initCause(e);
          throw ioe;
      }

      try {
          getIndexRunner().addTask(queryTask);
          return (Map<String, String>) queryTask.get();
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
  
  public String relativeIndexLocation() {
      return "Standard";
  }

}
