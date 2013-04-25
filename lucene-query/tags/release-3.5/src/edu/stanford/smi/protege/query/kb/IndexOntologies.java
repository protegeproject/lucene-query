package edu.stanford.smi.protege.query.kb;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.framestore.FrameStore;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.framestore.SimpleFrameStore;
import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.ProtegeJob;

public class IndexOntologies extends ProtegeJob {
  private static final long serialVersionUID = -7233685687549750920L;
  
  private static Logger log = Log.getLogger(IndexOntologies.class);
  
  private QueryConfiguration qc;
  
  public IndexOntologies(KnowledgeBase kb) {
      this(new QueryConfiguration(kb));
  }
  
  public IndexOntologies(QueryConfiguration qc) {
      super(qc.getKnowledgeBase());
      this.qc = qc;
  }

  @Override
  public Object run() throws ProtegeException {
    new InstallNarrowFrameStore(getKnowledgeBase(), new File(qc.getBaseIndexPath())).execute();
    FrameStore fs = ((DefaultKnowledgeBase) getKnowledgeBase()).getTerminalFrameStore();
    NarrowFrameStore nfs = ((SimpleFrameStore) fs).getHelper();
    do {
      if (nfs instanceof QueryNarrowFrameStore) {
          QueryNarrowFrameStore qnfs = (QueryNarrowFrameStore) nfs;
          qnfs.indexOntologies(qc);
          return Boolean.TRUE;
      }
    } while ((nfs = nfs.getDelegate()) != null);
    if (log.isLoggable(Level.FINE)) {
      log.fine("No query narrow frame store found - indexing not completed.");
    }
    return Boolean.FALSE;
  }
  
  @Override
    public void localize(KnowledgeBase kb) {
        super.localize(kb);
        qc.localize(kb);
    }

}
