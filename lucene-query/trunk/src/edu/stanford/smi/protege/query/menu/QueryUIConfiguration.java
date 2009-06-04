package edu.stanford.smi.protege.query.menu;

import java.io.Serializable;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Localizable;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.indexer.IndexMechanism;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.metaproject.Operation;
import edu.stanford.smi.protege.server.metaproject.impl.UnbackedOperationImpl;
import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protege.util.LocalizeUtils;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.impl.OWLSystemFrames;
import edu.stanford.smi.protegex.owl.model.impl.OWLUtil;

public class QueryUIConfiguration implements Serializable, Localizable {
    public static final Operation INDEX_OPERATION = new UnbackedOperationImpl("Generate Lucene Indices", "");
    public static final String PROTEGE_PROP_KEY_DEFAULT_SLOT = "query_plugin.default.search_slot";
    public static final String LUCENE_MAX_SEARCH_RESULTS = "lucene.max.displayed.results";
    
    private boolean canIndex;
    
    private boolean isOwl;
    
    private Slot defaultSlot;
    
    private Set<IndexMechanism> indexers = new HashSet<IndexMechanism>();
    
    private Set<Slot> luceneSlots = new HashSet<Slot>();
    
    private Set<Slot> allSlots = new HashSet<Slot>();
    
    private int maxResultsDisplayed;
    
    private EnumMap<BooleanConfigItem, Boolean> booleanConfigValue = new EnumMap<BooleanConfigItem, Boolean>(BooleanConfigItem.class);
    
    public enum BooleanConfigItem {
        SEARCH_FOR_CLASSES("lucene.search.classes", true),
        SEARCH_FOR_PROPERTIES("lucene.search.properties", true),
        SEARCH_FOR_INDIVIDUALS("lucene.search.individuals", true);
        
        private String protegeProperty;
        private boolean defaultValue;
        
        private BooleanConfigItem(String protegeProperty, boolean defaultValue) {
            this.protegeProperty = protegeProperty;
            this.defaultValue = defaultValue;
        }
        
        public String getProtegeProperty() {
            return protegeProperty;
        }
        
        public boolean getDefaultValue() {
            return defaultValue;
        }
        
    }


    
    public QueryUIConfiguration(KnowledgeBase kb) {
        canIndex = RemoteClientFrameStore.isOperationAllowed(kb, INDEX_OPERATION);
        isOwl = (kb instanceof OWLModel);
        
        initSlots(kb);
        initBooleans();
        initOther();
    }
    
    public QueryUIConfiguration(QueryUIConfiguration original) {
        canIndex = original.canIndex();
        isOwl = original.isOwl();
        
        defaultSlot = original.getDefaultSlot();
        allSlots = original.allSlots;
        
        maxResultsDisplayed = original.getMaxResultsDisplayed();
        
        for (BooleanConfigItem configItem : BooleanConfigItem.values()) {
            boolean originalValue = original.getBooleanConfiguration(configItem);
            booleanConfigValue.put(configItem, originalValue);
        }
    }
    
    public void save() {
        ApplicationProperties.setString(PROTEGE_PROP_KEY_DEFAULT_SLOT, defaultSlot.getName());
        ApplicationProperties.setInt(LUCENE_MAX_SEARCH_RESULTS, getMaxResultsDisplayed());
        for (BooleanConfigItem configItem : BooleanConfigItem.values()) {
            String protegeProperty = configItem.getProtegeProperty();
            boolean value = booleanConfigValue.get(configItem);
            ApplicationProperties.setBoolean(protegeProperty, value);
        }
    }
    
    /* ------------------------------------
     * Initialization
     */
    
    private void initSlots(KnowledgeBase kb) {
        String defaultSlotName = ApplicationProperties.getString(PROTEGE_PROP_KEY_DEFAULT_SLOT);
        if (defaultSlotName != null) {
            if (isOwl) {
                defaultSlotName = OWLUtil.getInternalFullName((OWLModel) kb, defaultSlotName);
            }
            defaultSlot = kb.getSlot(defaultSlotName);
        }
        if (defaultSlot == null) {
            defaultSlot = kb.getNameSlot();
        }
        if (kb instanceof OWLModel) {
            allSlots = collectOWLProperties((OWLModel) kb);
        }
        else {
            allSlots = new HashSet<Slot>(kb.getSlots());
        }
    }
    
    /**
     * Gets all the {@link RDFProperty} objects from the {@link OWLModel} and builds a collection
     * of {@link OWLProperty}s which are returned (down-casted to {@link Slot}s).
     */
    @SuppressWarnings("unchecked")
    private static Set<Slot> collectOWLProperties(OWLModel model) {
        Collection rdfProps = model.getRDFProperties();
        Set<Slot> slots = new HashSet<Slot>(rdfProps.size());
        for (Iterator iter = rdfProps.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof OWLProperty || !((Slot) obj).isSystem()) {
                Slot owlProp = (Slot) obj;
                slots.add(owlProp);
            }
        }
        // Hacky - most of the rdf properties are not interesting
        OWLSystemFrames frames = model.getSystemFrames();
        slots.add(frames.getOwlIncompatibleWithProperty());
        slots.add(frames.getOwlBackwardCompatibleWithProperty());
        slots.add(frames.getOwlPriorVersionProperty());
        slots.add(frames.getRdfsIsDefinedByProperty());
        slots.add(frames.getRdfsLabelProperty());
        slots.add(frames.getRdfsSeeAlsoProperty());
        slots.add(frames.getNameSlot());
        return slots;
    }
    
    private void initBooleans() {
        for (BooleanConfigItem configItem : BooleanConfigItem.values()) {
            String protegeProperty = configItem.getProtegeProperty();
            boolean value = ApplicationProperties.getBooleanProperty(protegeProperty, configItem.getDefaultValue());
            booleanConfigValue.put(configItem, value);
        } 
    }
    
    private void initOther() {
        maxResultsDisplayed = ApplicationProperties.getIntegerProperty(LUCENE_MAX_SEARCH_RESULTS, 50);
    }
    

    /* ------------------------------------
     * Utilities
     */
    
    public boolean getBooleanConfiguration(BooleanConfigItem configItem) {
        return booleanConfigValue.get(configItem);
    }
   
    public void setBooleanConfiguration(BooleanConfigItem configItem, boolean value) {
        booleanConfigValue.put(configItem, value);
    }
    
    public void localize(KnowledgeBase kb) {
        for (Slot slot : allSlots) {
            LocalizeUtils.localize(slot, kb);
        }
        LocalizeUtils.localize(defaultSlot, kb);
        for (Slot slot : luceneSlots) {
            LocalizeUtils.localize(slot, kb);
        }
    }
    
    /*
     * Getters and setters.
     */
    
    public boolean canIndex() {
        return canIndex;
    }

    public boolean isOwl() {
        return isOwl;
    }
    
    
    
    public Slot getDefaultSlot() {
        return defaultSlot;
    }

    public void setDefaultSlot(Slot defaultSlot) {
        this.defaultSlot = defaultSlot;
    }

    public boolean isSearchResultsIncludeClasses() {
        return getBooleanConfiguration(BooleanConfigItem.SEARCH_FOR_CLASSES);
    }

    public void setSearchResultsIncludeClasses(boolean searchResultsIncludeClasses) {
        setBooleanConfiguration(BooleanConfigItem.SEARCH_FOR_CLASSES, searchResultsIncludeClasses);
    }

    public boolean isSearchResultsIncludeProperties() {
        return getBooleanConfiguration(BooleanConfigItem.SEARCH_FOR_PROPERTIES);
    }

    public void setSearchResultsIncludeProperties(boolean searchResultsIncludeProperties) {
        setBooleanConfiguration(BooleanConfigItem.SEARCH_FOR_PROPERTIES, searchResultsIncludeProperties);
    }
    
    public boolean isSearchResultsIncludeIndividuals() {
    	return getBooleanConfiguration(BooleanConfigItem.SEARCH_FOR_INDIVIDUALS);
    }
    
    public void setSearchResultsIncludeIndividuals(boolean searchResultsIncludeIndividuals) {
    	setBooleanConfiguration(BooleanConfigItem.SEARCH_FOR_INDIVIDUALS, searchResultsIncludeIndividuals);
    }
    
    public Set<Slot> getLuceneSlots() {
        return luceneSlots;
    }
    
    public void setLuceneSlots(Set<Slot> luceneSlots) {
        allSlots.addAll(luceneSlots);
        this.luceneSlots = luceneSlots;
    }
    
    public Set<Slot> getAllSlots() {
        return allSlots;
    }

    public Set<IndexMechanism> getIndexers() {
        return indexers;
    }

    public void setIndexers(Set<IndexMechanism> indexers) {
        this.indexers = indexers;
    }

    public int getMaxResultsDisplayed() {
        return maxResultsDisplayed;
    }

    public void setMaxResultsDisplayed(int maxResultsDisplayed) {
    	if (maxResultsDisplayed > 0) {
    		this.maxResultsDisplayed = maxResultsDisplayed;
    	}
    }
    

    
}
