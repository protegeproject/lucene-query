package edu.stanford.smi.protege.query.menu;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import edu.stanford.smi.protege.query.menu.QueryUIConfiguration.BooleanConfigItem;
import edu.stanford.smi.protege.ui.CheckBoxRenderer;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.LabeledComponent;



public class ConfigureLuceneTabsPanel extends JComponent {
    private static final long serialVersionUID = 1835328900385886439L;
    
    private QueryUIConfiguration configuration;
    private JTable table;
    private EnumMap<BooleanConfigItem, String> titleMap = new EnumMap<BooleanConfigItem, String>(BooleanConfigItem.class);

    
    public ConfigureLuceneTabsPanel(QueryUIConfiguration configuration) {
        this.configuration = configuration;
        initTitles();
        
        setLayout(new BorderLayout());
        table = ComponentFactory.createTable(null);
        table.setModel(createTableModel());
        table.addMouseListener(new ClickListener());
        ComponentUtilities.addColumn(table, new DefaultTableCellRenderer());
        ComponentUtilities.addColumn(table, new CheckBoxRenderer());
        
        JScrollPane pane = ComponentFactory.createScrollPane(table);
        // pane.setColumnHeaderView(table.getTableHeader());
        // pane.setBackground(table.getBackground());
        LabeledComponent c = new LabeledComponent("Lucene Configuration", pane);
        c.add(pane);
        add(c);
    }
    
    private void initTitles() {
        titleMap.put(BooleanConfigItem.SEARCH_FOR_CLASSES, "Include Classes in search results");
        titleMap.put(BooleanConfigItem.SEARCH_FOR_PROPERTIES, 
                     configuration.isOwl() ? "Include Properties in search results" : "Include Slots in search Results");
    }
    
    private TableModel createTableModel() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Configuration Property");
        model.addColumn("Value");
        
        for (BooleanConfigItem configItem : BooleanConfigItem.values()) {
            model.addRow(new Object[] {titleMap.get(configItem), configuration.getBooleanConfiguration(configItem) });
        }    
        return model;
    }

    class ClickListener extends MouseAdapter {
        public void mousePressed(MouseEvent event) {
            Point p = event.getPoint();
            int col = table.columnAtPoint(p);
            if (col == 1) {
                int row = table.rowAtPoint(p);                
                boolean b = getValue(row);
                setValue(row, !b);                              
            }
        }
    }
    
    private boolean getValue(int row) {
        Boolean b = (Boolean) getTabModel().getValueAt(row, 1);
        return b.booleanValue();
    }

    private void setValue(int row, boolean value) {
        getTabModel().setValueAt(Boolean.valueOf(value), row, 1);
        BooleanConfigItem configItem = BooleanConfigItem.values()[row];
        configuration.setBooleanConfiguration(configItem, value);
    }
    
    private DefaultTableModel getTabModel() {
        return (DefaultTableModel) table.getModel();
    }
}
