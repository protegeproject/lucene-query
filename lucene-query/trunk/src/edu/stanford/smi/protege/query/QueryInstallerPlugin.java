package edu.stanford.smi.protege.query;

import java.util.Collection;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.WidgetDescriptor;
import edu.stanford.smi.protege.plugin.ProjectPluginAdapter;
import edu.stanford.smi.protege.query.api.QueryApi;
import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protege.util.Log;

public class QueryInstallerPlugin extends ProjectPluginAdapter {
	
	public static final String AUTO_INSTALL_QUERY = "query.lucene.server.autoinstall";
	private static boolean autoInstall = ApplicationProperties.getBooleanProperty(AUTO_INSTALL_QUERY, true);
    
    public void afterLoad(Project p) {
    	if (!p.isMultiUserServer()) {
    		return;
    	}
        String pluginClassName = LuceneQueryPlugin.class.getCanonicalName();
        WidgetDescriptor desc = p.getTabWidgetDescriptor(pluginClassName);
        if (desc != null && desc.isVisible()) {
            KnowledgeBase kb = p.getKnowledgeBase();
            new QueryApi(kb).install(new QueryConfiguration(kb));
        }
    }
}
