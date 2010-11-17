package edu.stanford.smi.protege.query.indexer;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
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

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.query.querytypes.impl.LuceneBrowserTextSearch;
import edu.stanford.smi.protege.query.querytypes.impl.LuceneOwnSlotValueQuery;
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
      BooleanQuery query  = new  BooleanQuery();
      BooleanQuery.setMaxClauseCount(100000) ;

      try {
          QueryExpander queryExpander = new QueryExpander(getAnalyzer(), BROWSER_TEXT, getFullIndexPath());
          queryExpander.parsePrefixQuery(query, q.getText());
      } catch (Exception e) {
          IOException ioe = new IOException(e.getMessage());
          ioe.initCause(e);
          throw ioe;
      }

      return executeQuery(query);
  }

  public void browserTextChanged(final Frame frame){
      getIndexRunner().submit(new BrowserTextChangedRunnable(frame) , true);
  }

  private class BrowserTextChangedRunnable implements Runnable {
      private Frame frame;

      public BrowserTextChangedRunnable(Frame frame) {
          this.frame = frame;
      }
      public void run() {
          BooleanQuery query  = new  BooleanQuery();

          Term term;
          term = new Term(FRAME_NAME, frame.getName());
          query.add(new TermQuery(term), BooleanClause.Occur.MUST);
          query.add(new TermQuery(new Term(BROWSER_TEXT_PRESENT, Boolean.TRUE.toString())), BooleanClause.Occur.MUST);
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
  }

  public Map<String, String> queryBrowserText(String luceneQuery) throws IOException {
      QueryParser parser = new QueryParser(BROWSER_TEXT, getAnalyzer());
      parser.setAllowLeadingWildcard(true);
      try {
          final Query query = parser.parse(luceneQuery);
          return getIndexRunner().submit(new QueryBrowserTextCallable(query)).get();
      } catch (ParseException e) {
          IOException ioe = new IOException(e.getMessage());
          ioe.initCause(e);
          throw ioe;
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

  private class QueryBrowserTextCallable implements Callable<Map<String, String>> {
      private Query query;

      public QueryBrowserTextCallable(Query query) {
          this.query = query;
      }

      public Map<String, String> call() throws IOException {
          if (getStatus() == Status.DOWN) {
              return new HashMap<String, String>();
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
          } finally {
              forceClose(searcher);
          }
          return results;
      }
  }

  public String relativeIndexLocation() {
      return "Standard";
  }

}
