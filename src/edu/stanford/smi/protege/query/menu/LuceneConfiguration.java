package edu.stanford.smi.protege.query.menu;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.query.indexer.Indexer;
import edu.stanford.smi.protege.query.indexer.PhoneticIndexer;
import edu.stanford.smi.protege.query.indexer.StdIndexer;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.metaproject.Operation;
import edu.stanford.smi.protege.server.metaproject.impl.UnbackedOperationImpl;
import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class LuceneConfiguration {
    public static final Operation INDEX_OPERATION = new UnbackedOperationImpl("Generate Lucene Indices", "");
    
    private boolean canIndex;
    
    private boolean isOwl;
    
    private EnumMap<BooleanConfigItem, Boolean> booleanConfigValue = new EnumMap<BooleanConfigItem, Boolean>(BooleanConfigItem.class);
    
    public enum BooleanConfigItem {
        SEARCH_FOR_CLASSES("lucene.search.classes", true),
        SEARCH_FOR_PROPERTIES("lucene.search.properties", true),
        USE_PHONETIX_INDICIES("lucene.use.phonetix.index", false),
        USE_STANDARD_INDICIES("lucene.use.standard.index", true);
        
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


    
    public LuceneConfiguration(KnowledgeBase kb) {
        canIndex = RemoteClientFrameStore.isOperationAllowed(kb, INDEX_OPERATION);
        isOwl = (kb instanceof OWLModel);
        for (BooleanConfigItem configItem : BooleanConfigItem.values()) {
            String protegeProperty = configItem.getProtegeProperty();
            boolean value = ApplicationProperties.getBooleanProperty(protegeProperty, configItem.getDefaultValue());
            booleanConfigValue.put(configItem, value);
        }
    }
    
    public LuceneConfiguration(LuceneConfiguration original) {
        canIndex = original.canIndex();
        isOwl = original.isOwl();
        for (BooleanConfigItem configItem : BooleanConfigItem.values()) {
            boolean originalValue = original.getBooleanConfiguration(configItem);
            booleanConfigValue.put(configItem, originalValue);
        }
    }
    
    public boolean getBooleanConfiguration(BooleanConfigItem configItem) {
        return booleanConfigValue.get(configItem);
    }
   
    public void setBooleanConfiguration(BooleanConfigItem configItem, boolean value) {
        booleanConfigValue.put(configItem, value);
    }
    
    public Set<Indexer> getIndexers() {
        Set<Indexer> indexers = new  HashSet<Indexer>();
        if (isUsePhoneticIndex()) {
            indexers.add(new PhoneticIndexer());
        }
        if (isUseStandardIndex()) {
            indexers.add(new StdIndexer());
        }
        return indexers;
    }
    
    public void save() {
        for (BooleanConfigItem configItem : BooleanConfigItem.values()) {
            String protegeProperty = configItem.getProtegeProperty();
            boolean value = booleanConfigValue.get(configItem);
            ApplicationProperties.setBoolean(protegeProperty, value);
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

    public boolean isUsePhoneticIndex() {
        return getBooleanConfiguration(BooleanConfigItem.USE_PHONETIX_INDICIES);
    }

    public void setUsePhoneticIndex(boolean usePhoneticIndex) {
        setBooleanConfiguration(BooleanConfigItem.USE_PHONETIX_INDICIES, usePhoneticIndex);
    }

    public boolean isUseStandardIndex() {
        return getBooleanConfiguration(BooleanConfigItem.USE_STANDARD_INDICIES);
    }

    public void setUseStandardIndex(boolean useStandardIndex) {
        setBooleanConfiguration(BooleanConfigItem.USE_STANDARD_INDICIES, useStandardIndex);
    }
}
