package edu.stanford.smi.protege.query.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.query.indexer.IndexMechanism;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.ui.DisplayUtilities;
import edu.stanford.smi.protege.ui.FrameRenderer;
import edu.stanford.smi.protege.util.AllowableAction;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.ModalDialog;
import edu.stanford.smi.protege.util.SelectableList;

public class IndexConfigurer extends JPanel {
    private static final long serialVersionUID = -4406193294688623588L;
    
    private QueryConfiguration configuration;
    private List<Slot> allSlots;
    
    public IndexConfigurer(KnowledgeBase kb) {
        configuration = new QueryConfiguration(kb);
        allSlots = new ArrayList<Slot>(configuration.getSearchableSlots());
        Collections.sort(allSlots);
        allSlots = Collections.unmodifiableList(allSlots);
        createGui();
    }
    
    private void createGui() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(getSlotsListComponent());
        for (final IndexMechanism mechanism : IndexMechanism.values()) {
            boolean defaultValue = mechanism.isEnabledByDefault();
            mechanism.setEnabled(configuration, defaultValue);
            final JCheckBox box = new JCheckBox(mechanism.getDescription());
            box.setSelected(defaultValue);
            box.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    mechanism.setEnabled(configuration, box.isSelected());
                }
                
            });
            add(box);
        }
    }

    private LabeledComponent getSlotsListComponent() {
        final SelectableList slotsList = ComponentFactory.createSelectableList(null);
        slotsList.setCellRenderer(FrameRenderer.createInstance());

        List<Slot> searchableSlots = new ArrayList<Slot>(configuration.getSearchableSlots());
        Collections.sort(searchableSlots);
        ComponentUtilities.addListValues(slotsList, searchableSlots);

        final LabeledComponent labeledComp = new LabeledComponent("Slots to export", new JScrollPane(slotsList), true );

        labeledComp.addHeaderButton(new AllowableAction("Add slots", Icons.getAddSlotIcon(), null) {

            public void actionPerformed(ActionEvent e) {
                List<Slot> removeableSlots = new ArrayList<Slot>(allSlots);
                removeableSlots.removeAll(configuration.getSearchableSlots());
                if (removeableSlots.isEmpty()) {
                    ModalDialog.showMessageDialog(IndexConfigurer.this, "All slots already included - no slots to add");
                    return;
                }
                // Show util window for multiple slot selection
                Collection<Slot> newSlots = DisplayUtilities.pickSlots(IndexConfigurer.this, 
                                                                       removeableSlots, 
                                                                       "Select slots to export (multiple selection)");
                Set<Slot> searchableSlots = configuration.getSearchableSlots();
                searchableSlots.addAll(newSlots);
                configuration.setSearchableSlots(searchableSlots);

                ComponentUtilities.clearListValues(slotsList);
                ComponentUtilities.addListValues(slotsList, searchableSlots);
            }

        });

        labeledComp.addHeaderButton(new AllowableAction("Remove slot", Icons.getRemoveSlotIcon(), null) {

            public void actionPerformed(ActionEvent arg0) {
                Collection selection = slotsList.getSelection();

                if (selection != null) {
                    Set<Slot> searchableSlots = configuration.getSearchableSlots();
                    searchableSlots.removeAll(selection);
                    configuration.setSearchableSlots(searchableSlots);
                    ComponentUtilities.clearListValues(slotsList);
                    ComponentUtilities.addListValues(slotsList, searchableSlots);
                }
            }

        });

        return labeledComp;
    }
    
    public QueryConfiguration getConfiguration() {
        return configuration;
    }
    
}
