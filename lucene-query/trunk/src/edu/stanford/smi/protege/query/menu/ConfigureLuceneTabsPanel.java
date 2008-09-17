package edu.stanford.smi.protege.query.menu;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.NumberFormat;
import java.util.EnumMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.menu.QueryUIConfiguration.BooleanConfigItem;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.ui.DisplayUtilities;
import edu.stanford.smi.protege.util.AllowableAction;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.Log;



public class ConfigureLuceneTabsPanel extends JComponent {
    private static final long serialVersionUID = 1835328900385886439L;
    
    private QueryUIConfiguration configuration;
    private JFormattedTextField maxResultsDisplayedField;
    private EnumMap<BooleanConfigItem, String> titleMap = new EnumMap<BooleanConfigItem, String>(BooleanConfigItem.class);

    
    public ConfigureLuceneTabsPanel(QueryUIConfiguration configuration) {
        this.configuration = configuration;
        initTitles();
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        addBooleanConfigurationItems();
        add(Box.createRigidArea(new Dimension(0, 10)));
        addDefaultSearchSlot();
        add(Box.createRigidArea(new Dimension(0, 10)));
        addMaxDisplayed();
    }
    
    private void initTitles() {
        titleMap.put(BooleanConfigItem.SEARCH_FOR_CLASSES, "Include Classes in search results");
        titleMap.put(BooleanConfigItem.SEARCH_FOR_PROPERTIES, 
                     configuration.isOwl() ? "Include Properties in search results" : "Include Slots in search Results");
    }
    
    private void addBooleanConfigurationItems() {
        for (final BooleanConfigItem configItem : BooleanConfigItem.values()) {
            boolean enabled = configuration.getBooleanConfiguration(configItem);
            final JCheckBox box = new JCheckBox(titleMap.get(configItem));
            box.setSelected(enabled);
            box.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    ConfigureLuceneTabsPanel.this.configuration.setBooleanConfiguration(configItem, box.isSelected());
                }
                
            });
            add(box);
        }
    }
    
    private void addDefaultSearchSlot() {
        final JTextField text = new JTextField();
        text.setText(configuration.getDefaultSlot().getBrowserText());
        text.enableInputMethods(false);
        LabeledComponent lc = new LabeledComponent("Default Search Slot", text);
        lc.addHeaderButton(new AllowableAction("Change Default Slot", Icons.getAddSlotIcon(), null) {
            private static final long serialVersionUID = 9122206203462592559L;

            public void actionPerformed(ActionEvent e) {
                Slot newSlot = DisplayUtilities.pickSlot(ConfigureLuceneTabsPanel.this, configuration.getAllSlots());
                if (newSlot != null) {
                    configuration.setDefaultSlot(newSlot);
                    text.setText(newSlot.getBrowserText());
                }
            }
        });
        add(lc);
    }
    
    private void addMaxDisplayed() {
        maxResultsDisplayedField = new JFormattedTextField(NumberFormat.getIntegerInstance());
        maxResultsDisplayedField.setValue(configuration.getMaxResultsDisplayed());
        LabeledComponent lc = new LabeledComponent("Max results displayed (-1=all)", maxResultsDisplayedField);
        add(lc);
    }
    
    public int getMaxResultsDisplayed() {
        return ((Long) maxResultsDisplayedField.getValue()).intValue();
    }
}
