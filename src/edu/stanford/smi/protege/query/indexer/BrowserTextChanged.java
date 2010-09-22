package edu.stanford.smi.protege.query.indexer;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.query.kb.InstallNarrowFrameStore;
import edu.stanford.smi.protege.query.kb.QueryNarrowFrameStore;
import edu.stanford.smi.protege.util.LocalizeUtils;
import edu.stanford.smi.protege.util.ProtegeJob;


public class BrowserTextChanged extends ProtegeJob {
    private static final long serialVersionUID = 2146019005875667953L;
    private Frame f;
    
    public static void browserTextChanged(Frame f) {
        new BrowserTextChanged(f).execute();
    }

    public BrowserTextChanged(Frame f) {
        super(f.getKnowledgeBase());
        this.f = f;
    }
    
    public Boolean run() throws ProtegeException {
        QueryNarrowFrameStore qnfs = InstallNarrowFrameStore.getQueryNarrowFrameStore(getKnowledgeBase());
        if (qnfs != null) {
            for (Indexer indexer : qnfs.getIndexers()) {
                if (indexer instanceof StdIndexer) {
                    ((StdIndexer) indexer).browserTextChanged(f);
                }
            }
        }
        return Boolean.TRUE;
    }
    
    
    
    @Override
    public void localize(KnowledgeBase kb) {
        super.localize(kb);
        LocalizeUtils.localize(f, kb);
    }
}
