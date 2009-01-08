package edu.stanford.smi.protege.query.kb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.server.RemoteSession;
import edu.stanford.smi.protege.server.framestore.ServerFrameStore;
import edu.stanford.smi.protege.util.FrameWithBrowserText;
import edu.stanford.smi.protege.util.FrameWithBrowserTextComparator;
import edu.stanford.smi.protege.util.ProtegeJob;


public class DoQueryJob extends ProtegeJob {
    private static final long serialVersionUID = -2294737751085431510L;
    
    private Query query;
    
    public DoQueryJob(KnowledgeBase kb, Query query) {
        super(kb);
        this.query = query;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<FrameWithBrowserText> run() throws ProtegeException {
        Collection<Frame> results = getKnowledgeBase().executeQuery(query);
        List<FrameWithBrowserText> wrappedResults = new ArrayList<FrameWithBrowserText>();
        RemoteSession session = null;
        if (getKnowledgeBase().getProject().isMultiUserServer()) { // disable frame calculator stuff
            session = ServerFrameStore.getCurrentSession();
            ServerFrameStore.setCurrentSession(null);
        }
        try {
            for (Frame result : results) {
                wrappedResults.add(new FrameWithBrowserText(result, result.getBrowserText(), ((Instance) result).getDirectTypes()));
            }
            Collections.sort(wrappedResults, new FrameWithBrowserTextComparator());
            results = new ArrayList<Frame>();
        }
        finally {
            if (getKnowledgeBase().getProject().isMultiUserServer()) {
                ServerFrameStore.setCurrentSession(session);
            }
        }
        return wrappedResults;
    }
    
    @SuppressWarnings("unchecked")
    public List<FrameWithBrowserText> execute() {
        return (List<FrameWithBrowserText>) super.execute();
    }
 
}
