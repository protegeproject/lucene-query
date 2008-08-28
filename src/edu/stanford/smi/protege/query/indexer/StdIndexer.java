package edu.stanford.smi.protege.query.indexer;

import java.io.IOException;
import java.util.Set;

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
  
  public Set<Frame> executeQuery(LuceneOwnSlotValueQuery query) throws IOException {
    return executeQuery(query.getSlot(), query.getExpr());
  }
  
  public String relativeIndexLocation() {
      return "Standard";
  }

}
