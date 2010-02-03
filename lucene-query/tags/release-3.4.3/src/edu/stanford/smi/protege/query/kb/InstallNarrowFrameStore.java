package edu.stanford.smi.protege.query.kb;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.framestore.SimpleFrameStore;
import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.ProtegeJob;

public class InstallNarrowFrameStore extends ProtegeJob {
  private static final long serialVersionUID = 8982683075005704375L;
  
  public final static String PHONETIC_SLOT_PROPERTY = "SearchablePhoneticSlot";

  private static final Logger log = Log.getLogger(InstallNarrowFrameStore.class);
  
  public final static String RDF_LABEL = "rdfs:label";
  public final static String RDF_COMMENT = "rdfs:comment";
  
  private File indexLocation;
  
  public InstallNarrowFrameStore(KnowledgeBase kb, File indexLocation) {
    super(kb);
    this.indexLocation = indexLocation;
  }
  
  @Override
  public QueryConfiguration execute() throws ProtegeException {
        return (QueryConfiguration) super.execute();
  }

  @Override
  public QueryConfiguration run() throws ProtegeException {
    DefaultKnowledgeBase kb = (DefaultKnowledgeBase) getKnowledgeBase();
    if (indexLocation == null) { 
        indexLocation = new File(new QueryConfiguration(kb).getBaseIndexPath());
    }
    
    SimpleFrameStore fs = (SimpleFrameStore) kb.getTerminalFrameStore();
    NarrowFrameStore nfs = fs.getHelper();

    QueryNarrowFrameStore qnfs = getQueryNarrowFrameStore(nfs);
    if (qnfs == null) {
    	qnfs = new QueryNarrowFrameStore(kb.getName(), nfs, getKnowledgeBase(), indexLocation);
    	fs.setHelper(qnfs);
    }
	return qnfs.getConfiguration();
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

}
