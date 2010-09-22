package edu.stanford.smi.protege.query.indexer;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.query.querytypes.impl.LuceneBrowserTextSearch;
import edu.stanford.smi.protege.query.querytypes.impl.LuceneOwnSlotValueQuery;


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
  
  public String relativeIndexLocation() {
      return "Standard";
  }

}
