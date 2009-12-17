package edu.stanford.smi.protege.query.api;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.kb.IndexOntologies;
import edu.stanford.smi.protege.query.kb.InstallNarrowFrameStore;

public class QueryApi {
    private transient KnowledgeBase kb;
    
    public QueryApi(KnowledgeBase kb) {
        this.kb = kb;
    }
    
    public Collection<Frame> executeQuery(Query q) {
        return kb.executeQuery(q);
    }
    
    public QueryConfiguration install() {
        return install(null);
    }
        
    public QueryConfiguration install(File indexLocation) {
        return (QueryConfiguration) new InstallNarrowFrameStore(kb, indexLocation).execute();
    }

    public QueryConfiguration index() {
        QueryConfiguration qc = new QueryConfiguration(kb);
        index(qc);
        return qc;
    }
    
    public void index(QueryConfiguration qc) { 
        new IndexOntologies(qc).execute();
    }

}
