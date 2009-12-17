package edu.stanford.smi.protege.query.menu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.EnumMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;

import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.menu.QueryUIConfiguration.BooleanConfigItem;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.ui.DisplayUtilities;
import edu.stanford.smi.protege.util.AbstractValidatableComponent;
import edu.stanford.smi.protege.util.AllowableAction;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.LabeledComponent;



public class ConfigureLuceneTabsPanel extends AbstractValidatableComponent {
    private static final long serialVersionUID = 1835328900385886439L;
    
    private QueryUIConfiguration configuration;
    private JFormattedTextField maxResultsDisplayedField;
    private JComponent centerComponent;
    private EnumMap<BooleanConfigItem, String> titleMap = new EnumMap<BooleanConfigItem, String>(BooleanConfigItem.class);

    
    public ConfigureLuceneTabsPanel(QueryUIConfiguration configuration) {
        this.configuration = configuration;
        initTitles();
        
        setLayout(new BorderLayout());
        centerComponent = new Box(BoxLayout.Y_AXIS);    
        
        addBooleanConfigurationItems();
        centerComponent.add(Box.createRigidArea(new Dimension(0, 10)));
        addDefaultSearchSlot();
        addMaxDisplayed();        
        add(centerComponent);
    }
    
    private void initTitles() {
    	boolean isOwl = configuration.isOwl();
        titleMap.put(BooleanConfigItem.SEARCH_FOR_CLASSES, "Include Classes in search results");
        titleMap.put(BooleanConfigItem.SEARCH_FOR_PROPERTIES, 
        		     isOwl ? "Include Properties in search results" : 
        		    	      "Include Slots in search Results");
        titleMap.put(BooleanConfigItem.SEARCH_FOR_INDIVIDUALS, 
        			 isOwl ? "Include Individuals in search results" : 
        				     "Include instances in search Results");
        titleMap.put(BooleanConfigItem.ALLOW_METAMODELING, "Allow meta-modelling");
    }
    
    private void addBooleanConfigurationItems() {
        for (final BooleanConfigItem configItem : BooleanConfigItem.values()) {
        	if  (!configuration.isOwl() && configItem  == BooleanConfigItem.ALLOW_METAMODELING) {
        		continue;
        	}
            boolean enabled = configuration.getBooleanConfiguration(configItem);
            final JCheckBox box = ComponentFactory.createCheckBox(titleMap.get(configItem));
            box.setAlignmentX(Component.LEFT_ALIGNMENT);            
            box.setSelected(enabled);
            box.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    ConfigureLuceneTabsPanel.this.configuration.setBooleanConfiguration(configItem, box.isSelected());
                }
                
            });
            centerComponent.add(box);
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
        lc.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerComponent.add(lc);
    }
    
    private void addMaxDisplayed() {
        maxResultsDisplayedField = new JFormattedTextField(NumberFormat.getIntegerInstance());
        maxResultsDisplayedField.setValue(configuration.getMaxResultsDisplayed());
        LabeledComponent lc = new LabeledComponent("Max results displayed per page", maxResultsDisplayedField);
        lc.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerComponent.add(lc);
    }
    
    public int getMaxResultsDisplayed() {
        return ((Number) maxResultsDisplayedField.getValue()).intValue();
    }

    public void saveContents() {                
    }

    public boolean validateContents() {
        return true;
    }
}
