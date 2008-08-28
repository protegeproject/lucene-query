package edu.stanford.smi.protege.query.api;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Localizable;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.indexer.Indexer;
import edu.stanford.smi.protege.query.indexer.PhoneticIndexer;
import edu.stanford.smi.protege.query.indexer.StdIndexer;
import edu.stanford.smi.protege.util.LocalizeUtils;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class QueryConfiguration implements Localizable, Serializable {
    private static final long serialVersionUID = 720591441102651371L;
    
    private transient KnowledgeBase kb;
    private Set<Slot> searchableSlots;
    private Set<Indexer> indexers;
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
        searchableSlots = new HashSet<Slot>();
        if (kb instanceof OWLModel) {
            OWLModel owl = (OWLModel) kb;
            searchableSlots.addAll(owl.getOWLAnnotationProperties());
            searchableSlots.add(owl.getRDFSLabelProperty());
            searchableSlots.add(owl.getRDFSCommentProperty());
            searchableSlots.add(kb.getSystemFrames().getNameSlot());
            return searchableSlots;
        } 
        else {
            searchableSlots.addAll(kb.getSlots());
        }
        return searchableSlots;
    }
    
    public void setSearchableSlots(Set<Slot> searchableSlots) {
        this.searchableSlots = searchableSlots;
    }
    
    
    public Set<Indexer> getIndexers() {
        if (indexers != null) {
            return indexers;
        }
        indexers = new HashSet<Indexer>();
        indexers.add(new PhoneticIndexer());
        indexers.add(new StdIndexer());
        return indexers;
    }
    
    public void setIndexers(Set<Indexer> indexers) {
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
