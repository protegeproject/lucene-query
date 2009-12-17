package edu.stanford.smi.protege.query.menu;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.query.LuceneQueryPlugin;
import edu.stanford.smi.protege.query.api.QueryApi;
import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.ModalDialog;



public class InstallIndiciesAction extends AbstractAction {
    private static final long serialVersionUID = 7595447181648323136L;
    
    private LuceneQueryPlugin plugin;
    private KnowledgeBase kb;

    public InstallIndiciesAction(LuceneQueryPlugin plugin, KnowledgeBase kb) {
        super("Index Ontologies...");
        this.plugin = plugin;
        this.kb = kb;
    }
    
    public void actionPerformed(ActionEvent buttonPushed) {
        final IndexConfigurer configurer = new IndexConfigurer(kb);
        int ret = ModalDialog.showDialog(plugin, configurer, "Index Ontologies", ModalDialog.MODE_OK_CANCEL);
        if (ret == ModalDialog.OPTION_OK) {
            final JDialog dialog = createProgressBox();
            new Thread(new Runnable() {
                public void run() {
                  try {
                      QueryApi api = new QueryApi(kb);
                      QueryConfiguration qc = configurer.getConfiguration();
                      api.index(qc);
                      plugin.getUIConfiguration().setLuceneSlots(qc.getSearchableSlots());
                      plugin.getUIConfiguration().setIndexers(qc.getIndexers());
                  } finally {
                      dialog.dispose();
                  }
                }
            }).start();
        }
    }
    
    private JDialog createProgressBox() {
        JProgressBar pb = new JProgressBar(0,100);
        pb.setPreferredSize(new Dimension(175,20));
        pb.setString("Indexing Ontologies");
        pb.setStringPainted(true);
        pb.setValue(0); 
        pb.setIndeterminate(true);
        JPanel centerPanel = new JPanel();
        centerPanel.add(pb);
        JDialog dialog = new JDialog(ComponentUtilities.getFrame(plugin), "Indexing Ontologies");
        dialog.add(centerPanel);
        dialog.pack();
        dialog.setVisible(true);
        return dialog;
    }

}
