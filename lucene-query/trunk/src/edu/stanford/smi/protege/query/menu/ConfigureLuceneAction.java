package edu.stanford.smi.protege.query.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import edu.stanford.smi.protege.query.LuceneQueryPlugin;
import edu.stanford.smi.protege.util.ModalDialog;
import edu.stanford.smi.protegex.owl.ui.icons.OWLIcons;

public class ConfigureLuceneAction extends AbstractAction {
    private static final long serialVersionUID = -36752085065056828L;
    
    private LuceneQueryPlugin plugin;
    private QueryUIConfiguration newConfiguration;
    
    public ConfigureLuceneAction(LuceneQueryPlugin plugin) {
        super("Configure", OWLIcons.getPreferencesIcon());
        this.plugin = plugin;
        newConfiguration = new QueryUIConfiguration(plugin.getUIConfiguration());
    }

    public void actionPerformed(ActionEvent e) {
        ConfigureLucenePanel panel = new ConfigureLucenePanel(newConfiguration);
        int sel = ModalDialog.showDialog(plugin, panel, "Configure Collaborative Protege", ModalDialog.MODE_OK_CANCEL);
        
        if (sel == ModalDialog.OPTION_OK) {
            plugin.setLuceneConfiguration(newConfiguration);
            newConfiguration.save();
        }   
    }

}
