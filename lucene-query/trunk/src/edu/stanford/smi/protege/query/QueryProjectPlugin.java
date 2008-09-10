package edu.stanford.smi.protege.query;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.WidgetDescriptor;
import edu.stanford.smi.protege.plugin.ProjectPluginAdapter;
import edu.stanford.smi.protege.query.api.QueryApi;
import edu.stanford.smi.protege.query.menu.LuceneConfiguration;
import edu.stanford.smi.protege.util.ApplicationProperties;

public class QueryProjectPlugin extends ProjectPluginAdapter {
	
	public static final String AUTO_INSTALL_QUERY = "query.lucene.server.autoinstall";
	private static boolean autoInstall = ApplicationProperties.getBooleanProperty(AUTO_INSTALL_QUERY, true);
    
	@Override
    public void afterLoad(Project p) {
        if (autoInstall && 
                p.isMultiUserServer() &&
                isLuceneTabEnabled(p)) {
            installQueryFrameStoreOnServer(p);
        }
	}
    
    private void installQueryFrameStoreOnServer(Project p) {
        KnowledgeBase kb = p.getKnowledgeBase();
        new QueryApi(kb).install(LuceneQueryPlugin.getQueryConfiguration(kb, new LuceneConfiguration(kb)));
    }
    
    private boolean isLuceneTabEnabled(Project p) {
        String pluginClassName = LuceneQueryPlugin.class.getCanonicalName();
        WidgetDescriptor desc = p.getTabWidgetDescriptor(pluginClassName);
        return desc != null && desc.isVisible();
    }
    
}
