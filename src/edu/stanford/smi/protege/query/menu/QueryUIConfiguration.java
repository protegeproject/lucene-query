package edu.stanford.smi.protege.query.menu;

import java.io.Serializable;
import java.util.Collection;
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
import edu.stanford.smi.protegex.util.PagedFrameList.SearchType;

public class QueryUIConfiguration implements Serializable, Localizable {
	private static final long serialVersionUID = -3542931449877194245L;
	public static final Operation INDEX_OPERATION = new UnbackedOperationImpl("Generate Lucene Indices", "");
    public static final String PROTEGE_PROP_KEY_DEFAULT_SLOT = "query_plugin.default.search_slot";
    public static final String LUCENE_MAX_SEARCH_RESULTS = "lucene.max.displayed.results";
    public static final String LUCENE_FILTER_SEARCH_TYPE = "lucene.filter.search.type";
    
    private KnowledgeBase kb;
    
    private boolean canIndex;
    
    private boolean isOwl;
    
    private Slot defaultSlot;
    
    private Set<IndexMechanism> indexers = new HashSet<IndexMechanism>();
    
    private Set<Slot> luceneSlots = new HashSet<Slot>();
    
    private Set<Slot> allSlots = new HashSet<Slot>();
    
    private int maxResultsDisplayed;
    
    private SearchType filterResultsSearchType;
    
    //private EnumMap<BooleanConfigItem, Boolean> booleanConfigValue = new EnumMap<BooleanConfigItem, Boolean>(BooleanConfigItem.class);
    
    public enum BooleanConfigItem {
        SEARCH_FOR_CLASSES("lucene.search.classes", true),
        SEARCH_FOR_PROPERTIES("lucene.search.properties", true),
        SEARCH_FOR_INDIVIDUALS("lucene.search.individuals", true),
        ALLOW_METAMODELING("Allow meta-modeling", false);
        
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
    	this.kb = kb;
        canIndex = RemoteClientFrameStore.isOperationAllowed(kb, INDEX_OPERATION);
        isOwl = (kb instanceof OWLModel);
        
        initSlots(kb);
        initBooleans();
        initOther();
    }
    
    public QueryUIConfiguration(KnowledgeBase kb, QueryUIConfiguration original) {
        this.kb = kb;
    	canIndex = original.canIndex();
        isOwl = original.isOwl();
        
        defaultSlot = original.getDefaultSlot();
        allSlots = original.allSlots;
        
        maxResultsDisplayed = original.getMaxResultsDisplayed();
        filterResultsSearchType = original.getFilterResultsSearchType();
        	
        for (BooleanConfigItem configItem : BooleanConfigItem.values()) {
            boolean originalValue = original.getBooleanConfiguration(configItem);
            //this might be a no-op. Config is stored in the pprj
            setBooleanConfiguration(configItem, originalValue);
        }
    }
    
    public void save() {
        ApplicationProperties.setString(PROTEGE_PROP_KEY_DEFAULT_SLOT, defaultSlot.getName());
        ApplicationProperties.setInt(LUCENE_MAX_SEARCH_RESULTS, getMaxResultsDisplayed());
        ApplicationProperties.setString(LUCENE_FILTER_SEARCH_TYPE, filterResultsSearchType.toString());	
        for (BooleanConfigItem configItem : BooleanConfigItem.values()) {
            String protegeProperty = configItem.getProtegeProperty();
            boolean value = getBooleanConfiguration(configItem);
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
    
    //FIXME: TT: this is obsolete now. Config saved as part of pprj. Might be required by NCI?
    //Tim, please delete, if OK.
    private void initBooleans() { 
        for (BooleanConfigItem configItem : BooleanConfigItem.values()) {
            String protegeProperty = configItem.getProtegeProperty();
            boolean value = ApplicationProperties.getBooleanProperty(protegeProperty, configItem.getDefaultValue());
            setBooleanConfiguration(configItem, value);
        } 
    }
    
    private void initOther() {
        maxResultsDisplayed = ApplicationProperties.getIntegerProperty(LUCENE_MAX_SEARCH_RESULTS, 50);
        filterResultsSearchType = SearchType.CONTAINS;
        String preferredSearchType = ApplicationProperties.getString(LUCENE_FILTER_SEARCH_TYPE, SearchType.CONTAINS.toString());
        for (SearchType possible : SearchType.values()) {
        	if (possible.toString().equals(preferredSearchType)) {
        		filterResultsSearchType = possible;
        		break;
        	}
        }
    }
    

    /* ------------------------------------
     * Utilities
     */
    
    public boolean getBooleanConfiguration(BooleanConfigItem configItem) {
    	Object val = kb.getProject().getClientInformation(configItem.getProtegeProperty());
    	if (val == null) {
    		return true;
    	}
    	return (Boolean)val;
        //return booleanConfigValue.get(configItem);
    }
   
    public void setBooleanConfiguration(BooleanConfigItem configItem, boolean value) {
    	kb.getProject().setClientInformation(configItem.getProtegeProperty(), value);
        //booleanConfigValue.put(configItem, value);
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
    
    public boolean allowMetaModeling() {
    	return getBooleanConfiguration(BooleanConfigItem.ALLOW_METAMODELING);
    }
    
    public void setAllowMetaModeling(boolean allowMetaModeling) {
    	setBooleanConfiguration(BooleanConfigItem.ALLOW_METAMODELING, allowMetaModeling);
    }
    
    public Set<Slot> getLuceneSlots() {
        return luceneSlots;
    }
    
    public Set<Slot> getLuceneSlots(SlotFilterType slotFilter) {
        return slotFilter.filterSlots(this, luceneSlots);
    }
    
    public void setLuceneSlots(Set<Slot> luceneSlots) {
        allSlots.addAll(luceneSlots);
        this.luceneSlots = luceneSlots;
    }
    
    public Set<Slot> getAllSlots() {
        return allSlots;
    }
    
    public Set<Slot> getAllSlots(SlotFilterType slotFilter) {
    	return slotFilter.filterSlots(this, allSlots);
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
    
    public SearchType getFilterResultsSearchType() {
		return filterResultsSearchType;
	}
    
    public void setFilterResultsSearchType(SearchType filterResultsSearchType) {
		this.filterResultsSearchType = filterResultsSearchType;
	}
    
}
