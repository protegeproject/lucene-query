package edu.stanford.smi.protege.query;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;

import com.tangentum.phonetix.DoubleMetaphone;
import com.tangentum.phonetix.lucene.PhoneticAnalyzer;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.query.querytypes.PhoneticQuery;

public class PhoneticIndexer  extends CoreIndexer {
  
  public PhoneticIndexer(Set<Slot> searchableSlots, NarrowFrameStore delegate, String path, Object kbLock) {
    super(searchableSlots, delegate, path, kbLock);
  }

  @Override
  protected Analyzer createAnalyzer() {
    return new PhoneticAnalyzer(new DoubleMetaphone());
  }
  
  public Set<Frame> executeQuery(PhoneticQuery pq) throws IOException {
    return executeQuery(pq.getSlot(), pq.getExpr());
  }
 
}
