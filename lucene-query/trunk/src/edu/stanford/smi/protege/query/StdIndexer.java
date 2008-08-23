package edu.stanford.smi.protege.query;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.query.querytypes.LuceneOwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.OwnSlotValueQuery;


public class StdIndexer extends CoreIndexer {
  
  public StdIndexer(Set<Slot> searchableSlots, NarrowFrameStore delegate, String path, Object kbLock) {
    super(searchableSlots, delegate, path, kbLock);
  }

  @Override
  protected Analyzer createAnalyzer() {
    return new StandardAnalyzer();
  }
  
  public Set<Frame> executeQuery(LuceneOwnSlotValueQuery query) throws IOException {
    return executeQuery(query.getSlot(), query.getExpr());
  }

}
