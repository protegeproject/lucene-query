package edu.stanford.smi.protege.query.indexer;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.query.querytypes.LuceneOwnSlotValueQuery;


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
  
  public String relativeIndexLocation() {
      return "Standard";
  }

}
