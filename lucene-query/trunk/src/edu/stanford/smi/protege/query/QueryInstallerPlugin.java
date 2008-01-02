package edu.stanford.smi.protege.query;

import java.util.Collection;

import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.WidgetDescriptor;
import edu.stanford.smi.protege.plugin.ProjectPluginAdapter;
import edu.stanford.smi.protege.util.Log;

public class QueryInstallerPlugin extends ProjectPluginAdapter {
    
    public void afterLoad(Project p) {
        String pluginClassName = AdvancedQueryPlugin.class.getCanonicalName();
        WidgetDescriptor desc = p.getTabWidgetDescriptor(pluginClassName);
        if (desc.isVisible() && !p.isMultiUserServer()) {
            new InstallNarrowFrameStore(p.getKnowledgeBase()).execute();
        }
        Collection descriptors = p.getTabWidgetDescriptors();
        for (Object o : descriptors) {
            WidgetDescriptor d = (WidgetDescriptor) o;
            if (d.isVisible()) {
                Log.getLogger().info("descriptor = " + o + " visible = " + d.isVisible());
            }
        }
    }
}
