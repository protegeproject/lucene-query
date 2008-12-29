package edu.stanford.smi.protege.query.ui;

import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.text.JTextComponent;

import edu.stanford.smi.protege.util.LabeledComponent;

public class QueryLabeledComponent extends LabeledComponent {

	public QueryLabeledComponent(String label, Component c) {
		super(label, c);
	}

	public QueryLabeledComponent(String label, Component c, boolean verticallyStretchable) {
		super(label, c);
	}
	
    public QueryLabeledComponent(String label, Component c, boolean verticallyStretchable, boolean swappedHeader) {
    	super(label, c, verticallyStretchable, swappedHeader);
    }	
	
    public void focus() {
    	this.requestFocus();
    	Component comp = getCenterComponent();
		if (comp != null) {
			if (comp instanceof JScrollPane) {
				comp = ((JScrollPane) comp).getViewport().getView();;
			}
    		comp.requestFocus();
    		if (comp instanceof JTextComponent) {
				((JTextComponent) comp).selectAll();
    		}
    	}
    }
    
	/**
	 * Resets the center component if it is a {@link JTextComponent}, 
	 * {@link JList}, or {@link JComboBox} (or one of the above contained in a {@link JScrollPane}).
	 * {@link JComboBox} models are <b>not</b> cleared, but the first item is selected.
	 */
	public void reset() {
		Component comp = getCenterComponent();
		if (comp instanceof JScrollPane) {
			JScrollPane scroll = (JScrollPane) comp;
			if (scroll.getViewport() == null) {
				return;
			}
			comp = scroll.getViewport().getView();
		}
		if (comp instanceof JTextComponent) {
			// TODO: discuss with Tim/Tania, presumably we want to rest for other types?
			//((JTextComponent) comp).setText("");			
		}  else if (comp instanceof JList) {
			((JList) comp).setListData(new Object[0]);
		} else if (comp instanceof JComboBox) {
			JComboBox combo = (JComboBox) comp;
			if (combo.getModel().getSize() > 0) {
				combo.setSelectedIndex(0);
			}
		}
	}
	
	/**
	 * Attempts to get the value from the center component (or the component inside the scrollpane).
	 * If the component is a {@link JTextComponent} the text is returned.<br>
	 * If the component is a {@link JList} the first item is returned.<br>
	 * If the component is a {@link JComboBox} the selected item is returned. <br>
	 * @return Object or null
	 */
	public Object getValue() {
		Object obj = null;
		Component comp = getCenterComponent();
		if (comp instanceof JScrollPane) {
			JScrollPane scroll = (JScrollPane) comp;
			if (scroll.getViewport() == null) {
				return null;
			}
			comp = scroll.getViewport().getView();
		}
		if (comp instanceof JTextComponent) {
			obj = ((JTextComponent) comp).getText();			
		}  else if (comp instanceof JList) {
			ListModel model = ((JList) comp).getModel();
			if (model.getSize() > 0) {
				obj = model.getElementAt(0);
			}
		} else if (comp instanceof JComboBox) {
			obj = ((JComboBox) comp).getSelectedItem();
		}
		return obj;
	}

}
