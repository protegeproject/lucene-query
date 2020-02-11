package edu.stanford.smi.protege.query.indexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.kb.InstallNarrowFrameStore;
import edu.stanford.smi.protege.query.kb.QueryNarrowFrameStore;
import edu.stanford.smi.protege.util.LocalizeUtils;
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

    public static Collection<Frame> searchLuceneOwnSlots(KnowledgeBase kb, Collection<Slot> searchSlots, 
    		String searchString) throws ProtegeException {
    	return new ExecuteOwnSlotLuceneQuery(kb, searchSlots, searchString).execute();
    	
    }
    
    
    /************** Protege jobs executed on the server ****************/
    
    private static class GetBrowserTextToFrameNameMapJob extends ProtegeJob {
        private static final long serialVersionUID = -8851058740317077279L;
        private String luceneQuery;

        public GetBrowserTextToFrameNameMapJob(KnowledgeBase kb, String luceneQuery) {
            super(kb);
            this.luceneQuery = luceneQuery;
        }

        @Override
        public Object run() throws ProtegeException {
            StdIndexer indexer = getStandardIndexer(getKnowledgeBase());
            if (indexer != null) {
                try {
                    return indexer.queryBrowserText(luceneQuery);
                }
                catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, "Exception caught getting browser text to frame name map for " + luceneQuery, ioe);
                    return new HashMap<String,String>();
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
    
    private static class ExecuteOwnSlotLuceneQuery extends ProtegeJob {
    	
    	private static final long serialVersionUID = 7935544226304612747L;

    	private Collection<Slot> searchSlots;
    	private String searchString;
    	
		public ExecuteOwnSlotLuceneQuery(KnowledgeBase kb, Collection<Slot> searchSlots, String searchString) {
			super(kb);
			this.searchSlots = searchSlots;
			this.searchString = searchString;
		}


		@Override
		public Object run() throws ProtegeException {
			Collection<Frame> resultFrames = new ArrayList<Frame>();
			
			//searching only with the Standard indexer for now
			StdIndexer indexer = IndexUtilities.getStandardIndexer(getKnowledgeBase());
			if (indexer == null) {
				Log.getLogger().warning("Could not find Lucene standard indexer. Will not execute Lucene queries");
				return resultFrames;
			}
			
			try {
				resultFrames = indexer.executeQuery(searchSlots, searchString);
			} catch (IOException e) {
				if (Log.getLogger().getLevel() == Level.FINE) {
					Log.getLogger().log(Level.FINE, "Could not execute Lucene query " + searchString, e);
				}
			}
			
			return resultFrames;
		}
		
		@Override
        public Collection<Frame> execute() throws ProtegeException {
            return (Collection<Frame>) super.execute();
        }
		
		@Override
		public void localize(KnowledgeBase kb) {
			super.localize(kb);
			LocalizeUtils.localize(searchSlots, kb);
		}
    	
    }

}
