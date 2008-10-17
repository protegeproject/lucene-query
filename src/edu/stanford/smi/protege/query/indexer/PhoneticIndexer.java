package edu.stanford.smi.protege.query.indexer;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;

import com.tangentum.phonetix.DoubleMetaphone;
import com.tangentum.phonetix.lucene.PhoneticAnalyzer;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.query.querytypes.PhoneticQuery;

public class PhoneticIndexer  extends AbstractIndexer {
  
  public PhoneticIndexer() {
    super();
  }

  @Override
  protected Analyzer createAnalyzer() {
    return new PhoneticAnalyzer(new DoubleMetaphone());
  }
  
  public Collection<Frame> executeQuery(PhoneticQuery pq) throws IOException {
    return executeQuery(pq.getSlot(), pq.getExpr());
  }
  
  public String relativeIndexLocation() {
      return "Phonetic";
  }
 
}
