package edu.stanford.smi.protege.query.indexer;

import java.util.Set;

import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.util.ApplicationProperties;

public enum IndexMechanism {
    PHONETIX_INDICIES("lucene.use.phonetix.index", false, 
                      PhoneticIndexer.class,
                      "Use Phonetic Indexer", "sounds like"),
    STANDARD_INDICIES("lucene.use.standard.index", true, 
                      StdIndexer.class,
                      "Use Standard Indexer", "lucene match");
    
    private String protegeProperty;
    private String description;
    private String command;
    private boolean defaultValue;
    private Class<? extends Indexer> indexerClass;
    
    private IndexMechanism(String protegeProperty, 
                            boolean defaultValue, 
                            Class<? extends Indexer> indexerClass,
                            String description, 
                            String cmd) {
        this.protegeProperty = protegeProperty;
        this.description = description;
        this.command = cmd;
        this.defaultValue = defaultValue;
        this.indexerClass = indexerClass;
    }
    
    public String getProtegeProperty() {
        return protegeProperty;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCommand() {
        return command;
    }
    
    public boolean getDefaultValue() {
        return defaultValue;
    }
    
    public Class<? extends Indexer> getIndexerClass() {
        return indexerClass;
    }
    
    /*
     * Utilities
     */
    
    public boolean isEnabledByDefault() {
        return ApplicationProperties.getBooleanProperty(getProtegeProperty(), getDefaultValue());
    }
    
    public boolean isEnabled(QueryConfiguration configuration) {
        return configuration.getIndexers().contains(this);
    }
    
    public void setEnabled(QueryConfiguration configuration, boolean enabled) {
        Set<IndexMechanism> indexers = configuration.getIndexers();
        if (enabled) {
            indexers.add(this);
        }
        else {
            indexers.remove(this);
        }
        configuration.setIndexers(indexers);
    }

}
