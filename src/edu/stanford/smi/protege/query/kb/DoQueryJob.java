package edu.stanford.smi.protege.query.kb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.util.ProtegeJob;


public class DoQueryJob extends ProtegeJob {
    private static final long serialVersionUID = -2294737751085431510L;
    
    private Query query;
    
    public DoQueryJob(KnowledgeBase kb, Query query) {
        super(kb);
        this.query = query;
    }

    @Override
    public Object run() throws ProtegeException {
        List<Frame> results = new ArrayList<Frame>(getKnowledgeBase().executeQuery(query));
        Collections.sort(results);
        return results;
    }
    
    @SuppressWarnings("unchecked")
    public List<Frame> execute() {
        return (List) super.execute();
    }

}
