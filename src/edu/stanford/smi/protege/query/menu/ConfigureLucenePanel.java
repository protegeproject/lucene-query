package edu.stanford.smi.protege.query.menu;

import edu.stanford.smi.protege.util.ValidatableTabComponent;

public class ConfigureLucenePanel  extends ValidatableTabComponent {
    private static final long serialVersionUID = -4965520422316945190L;
    
    public ConfigureLucenePanel(QueryUIConfiguration configuration) {
        addTab("Lucene Configuration", new ConfigureLuceneTabsPanel(configuration));       
    }
}
