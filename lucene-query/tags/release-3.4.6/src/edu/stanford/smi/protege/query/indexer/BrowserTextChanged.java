package edu.stanford.smi.protege.query.indexer;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.query.kb.InstallNarrowFrameStore;
import edu.stanford.smi.protege.query.kb.QueryNarrowFrameStore;
import edu.stanford.smi.protege.util.LocalizeUtils;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.ProtegeJob;


public class BrowserTextChanged extends ProtegeJob {

    private transient static final Logger log = Log.getLogger(BrowserTextChanged.class);

    private static final long serialVersionUID = 2146019005875667953L;
    private Frame f;

    public static void browserTextChanged(Frame f) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("Browser text changed (not yet in the index): " + f.getBrowserText() + " " + f.getName());
        }
        new BrowserTextChanged(f).execute();
    }

    public BrowserTextChanged(Frame f) {
        super(f.getKnowledgeBase());
        this.f = f;
    }

    @Override
    public Boolean run() throws ProtegeException {
        QueryNarrowFrameStore qnfs = InstallNarrowFrameStore.getQueryNarrowFrameStore(getKnowledgeBase());
        if (qnfs != null) {
            for (Indexer indexer : qnfs.getIndexers()) {
                if (indexer instanceof StdIndexer) {
                    ((StdIndexer) indexer).browserTextChanged(f, f.getBrowserText());
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
