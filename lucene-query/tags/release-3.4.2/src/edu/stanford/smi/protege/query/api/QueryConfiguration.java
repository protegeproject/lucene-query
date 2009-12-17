package edu.stanford.smi.protege.query.api;

import java.io.File;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Localizable;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.ValueType;
import edu.stanford.smi.protege.query.indexer.IndexMechanism;
import edu.stanford.smi.protege.util.LocalizeUtils;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class QueryConfiguration implements Localizable, Serializable {
    private static final long serialVersionUID = 720591441102651371L;
    
    private static transient final Logger log = Log.getLogger(QueryConfiguration.class);
    
    private transient KnowledgeBase kb;
    private Set<Slot> searchableSlots;
    private Set<IndexMechanism> indexers = EnumSet.allOf(IndexMechanism.class);
    private String baseIndexPath;
    
    
    public QueryConfiguration(KnowledgeBase kb) {
        this.kb = kb;
    }
    
    public KnowledgeBase getKnowledgeBase() {
        return kb;
    }
    
    @SuppressWarnings("unchecked")
    public Set<Slot> getSearchableSlots() {
        if (searchableSlots != null) {
            return searchableSlots;
        }
        Set<Slot> allSlots = new HashSet<Slot>();
        searchableSlots = new HashSet<Slot>();
        if (kb instanceof OWLModel) {
            OWLModel owl = (OWLModel) kb;
            allSlots.addAll(owl.getOWLAnnotationProperties());
            allSlots.add(owl.getRDFSLabelProperty());
            allSlots.add(owl.getRDFSCommentProperty());
            allSlots.add(kb.getSystemFrames().getNameSlot());
        } 
        else {
            allSlots.addAll(kb.getSlots());
        }
        for (Slot slot : allSlots) {
            ValueType vt = slot.getValueType();
            if (vt.equals(ValueType.ANY) || vt.equals(ValueType.STRING)) {
                searchableSlots.add(slot);
            }
        }
        return searchableSlots;
    }
    
    public void setSearchableSlots(Set<Slot> searchableSlots) {
        this.searchableSlots = searchableSlots;
    }
    
    
    public Set<IndexMechanism> getIndexers() {
        return indexers;
    }
    
    public void setIndexers(Set<IndexMechanism> indexers) {
        this.indexers = indexers;
    }
    
    
    public String getBaseIndexPath() {
        if (baseIndexPath != null) {
            return baseIndexPath;
        }
        baseIndexPath = kb.getProject().getProjectDirectoryURI().getPath();
        baseIndexPath = baseIndexPath + File.separator + "lucene" + File.separator + kb.getProject().getName();
        return baseIndexPath;
    }
    
    public void setBaseIndexPath(String baseIndexPath) {
        this.baseIndexPath = baseIndexPath;
    }

    public void localize(KnowledgeBase kb) {
        this.kb = kb;
        if (searchableSlots != null) {
            for (Slot slot : searchableSlots) {
                LocalizeUtils.localize(slot, kb);
            }
        }
    }

    
    
    
}
