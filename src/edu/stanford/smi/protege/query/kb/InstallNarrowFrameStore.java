package edu.stanford.smi.protege.query.kb;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.framestore.SimpleFrameStore;
import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.query.indexer.Indexer;
import edu.stanford.smi.protege.query.indexer.PhoneticIndexer;
import edu.stanford.smi.protege.query.indexer.StdIndexer;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.ProtegeJob;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class InstallNarrowFrameStore extends ProtegeJob {
  private static final long serialVersionUID = 8982683075005704375L;
  
  public final static String PHONETIC_SLOT_PROPERTY = "SearchablePhoneticSlot";

  private static final Logger log = Log.getLogger(InstallNarrowFrameStore.class);
  
  public final static String RDF_LABEL = "rdfs:label";
  public final static String RDF_COMMENT = "rdfs:comment";
  
  private QueryConfiguration qc;
  
  public InstallNarrowFrameStore(QueryConfiguration qc) {
    super(qc.getKnowledgeBase());
    this.qc = qc;
  }

  @Override
  public Object run() throws ProtegeException {
    DefaultKnowledgeBase kb = (DefaultKnowledgeBase) getKnowledgeBase();
    SimpleFrameStore fs = (SimpleFrameStore) kb.getTerminalFrameStore();
    NarrowFrameStore nfs = fs.getHelper();

    QueryNarrowFrameStore qnfs = getQueryNarrowFrameStore(nfs);
    if (qnfs == null) {
    	qnfs = new QueryNarrowFrameStore(kb.getName(), nfs, getKnowledgeBase());
    	fs.setHelper(qnfs);
    	qnfs.configure(qc);
    }
	return qnfs.getSearchableSlots();
  }
  
  private QueryNarrowFrameStore getQueryNarrowFrameStore(NarrowFrameStore nfs) {
    do {
      if (nfs instanceof QueryNarrowFrameStore) {
        if (log.isLoggable(Level.FINE)) {
          log.fine("Query Narrow Frame store already found - no install needed");
        }
        return (QueryNarrowFrameStore) nfs;
      }
    } while ((nfs = nfs.getDelegate()) != null);
    return null;
  }

  @Override
    public void localize(KnowledgeBase kb) {
        super.localize(kb);
        qc.localize(kb);
    }

}
