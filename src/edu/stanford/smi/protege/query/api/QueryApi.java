package edu.stanford.smi.protege.query.api;

import java.util.Set;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.kb.IndexOntologies;
import edu.stanford.smi.protege.query.kb.InstallNarrowFrameStore;

public class QueryApi {
    private transient KnowledgeBase kb;
    
    public QueryApi(KnowledgeBase kb) {
        this.kb = kb;
    }
    
    public Set<Frame> executeQuery(Query q) {
        return kb.executeQuery(q);
    }
    
    @SuppressWarnings("unchecked")
    public Set<Slot> install(QueryConfiguration qc) {
        return (Set<Slot>) new InstallNarrowFrameStore(qc).execute();
    }
    
    // ToDo add QueryConfiguration argument
    public void index() { 
        new IndexOntologies(kb).execute();
    }

}
