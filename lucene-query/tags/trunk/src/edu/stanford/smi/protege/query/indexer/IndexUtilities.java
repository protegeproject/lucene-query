package edu.stanford.smi.protege.query.indexer;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.query.kb.InstallNarrowFrameStore;
import edu.stanford.smi.protege.query.kb.QueryNarrowFrameStore;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.ProtegeJob;

public class IndexUtilities {
    public static final Logger LOGGER = Log.getLogger(IndexUtilities.class);
    
    public static StdIndexer getStandardIndexer(KnowledgeBase kb) {
        QueryNarrowFrameStore qnfs = InstallNarrowFrameStore.getQueryNarrowFrameStore(kb);
        for (Indexer indexer : qnfs.getIndexers()) {
            if (indexer instanceof StdIndexer) {
                return (StdIndexer) indexer;
            }
        }
        return null;
    }
    
    public static Map<String, String> getBrowserTextToFrameNameMap(KnowledgeBase kb, String luceneQuery) 
    throws ProtegeException {
        return new GetBrowserTextToFrameNameMapJob(kb, luceneQuery).execute();
    }
    
    private static class GetBrowserTextToFrameNameMapJob extends ProtegeJob {
        private static final long serialVersionUID = -8851058740317077279L;
        private String luceneQuery;
        
        public GetBrowserTextToFrameNameMapJob(KnowledgeBase kb, String luceneQuery) {
            super(kb);
            this.luceneQuery = luceneQuery;
        }
        
        public Object run() throws ProtegeException {
            StdIndexer indexer = getStandardIndexer(getKnowledgeBase());
            if (indexer != null) {
                try {
                    return indexer.queryBrowserText(luceneQuery);
                }
                catch (IOException ioe) {
                    throw new ProtegeException(ioe);
                }
            }
            else {
                LOGGER.warning("Browser text search only works when the standard indicies have been created");
                return null;
            }
        }
        
        @Override
        public Map<String, String> execute() throws ProtegeException {
            return (Map<String, String>) super.execute();
        }
    }
    
}
