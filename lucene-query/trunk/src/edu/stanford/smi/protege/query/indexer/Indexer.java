package edu.stanford.smi.protege.query.indexer;

import java.util.Collection;
import java.util.Set;

import edu.stanford.smi.protege.exception.ProtegeException;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;

public interface Indexer {
    
    void indexOntologies() throws ProtegeException;
    
    
    void setSearchableSlots(Set<Slot> slots);
    
    void setNarrowFrameStoreDelegate(NarrowFrameStore nfs);
    
    void setKnowledgeBase(KnowledgeBase kb);
    
    void setOWLMode(boolean owlMode);
    
    void setBaseIndexPath(String path);
    
    
    
    void addValues(final Frame frame, final Slot slot, final Collection values);
    
    void removeValue(final Frame frame, final Slot slot, final Object value);
    
    void removeValues(final Frame frame, final Slot slot);
    
    void removeValues(final String fname);
    
    
    String relativeIndexLocation();

}
