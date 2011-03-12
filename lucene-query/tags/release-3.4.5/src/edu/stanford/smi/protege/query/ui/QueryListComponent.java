package edu.stanford.smi.protege.query.ui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.JList;

import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.ui.FrameRenderer;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.DoubleClickActionAdapter;
import edu.stanford.smi.protege.util.LabeledComponent;

/**
 * This is a the base class for query {@link LabeledComponent}s that 
 * contains a single item list.
 *
 * @author Chris Callendar
 * @date 13-Sep-06
 */
public class QueryListComponent extends QueryLabeledComponent {

	protected KnowledgeBase kb;
	private JList lst;
	private ArrayList<QueryListComponentListener> listeners;
	
	private Action viewAction;
	private Action selectAction;
	private Action removeAction;
		
	public QueryListComponent(String title, KnowledgeBase kb) {
		super(title, ComponentFactory.createSingleItemList(null), false);
		this.kb = kb;
		this.lst = (JList) getCenterComponent();
		this.listeners = new ArrayList<QueryListComponentListener>(1);
		
		initialize();
	}
	
	public void addListener(QueryListComponentListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(QueryListComponentListener listener) {
		listeners.remove(listener);
	}

	private void initialize() {
		lst.setCellRenderer(new FrameRenderer());
		lst.setPreferredSize(new Dimension(200, 22));
		lst.setMaximumSize(new Dimension(1000, 24));
		setPreferredSize(new Dimension(200, 44));
		setMaximumSize(new Dimension(2000, 48));
	}
	
	/**
	 * Sets the actions and adds them as header buttons.
	 * @param viewAction the action used to view the current object
	 * @param selectAction the action used to add an object
	 * @param removeAction the action used to remove the current object
	 */
	public void setActions(Action viewAction, Action selectAction, Action removeAction) {
		this.viewAction = viewAction;
		addHeaderButton(viewAction);
		if (viewAction != null) {
			lst.addMouseListener(new DoubleClickActionAdapter(viewAction));
		}
		this.selectAction = selectAction;
		addHeaderButton(selectAction);
		this.removeAction = removeAction;
		addHeaderButton(removeAction);
	}
	
	public Action getViewAction() {
		return viewAction;
	}
	
	public Action getSelectAction() {
		return selectAction;
	}

	public Action getRemoveAction() {
		return removeAction;
	}
	
	public void focus() {
		lst.requestFocus();
		if (lst.getModel().getSize() >= 1) {
			lst.setSelectedIndex(0);
		}
	}
	
	protected void viewObject() {
		Object obj = getObject();
		if (obj instanceof Instance) {
    		kb.getProject().show((Instance) obj);
		}
	}

	public Object getObject() {
		Object obj = null;
		if (lst.getModel().getSize() == 1) {
			obj = lst.getModel().getElementAt(0);
		}
		return obj;
	}
	
	/**
	 * Sets the object for the list.  If null the list is cleared.
	 * @param obj
	 */
	public void setObject(Object obj) {
		if (obj != null) {
			lst.setListData(new Object[] { obj });
		} else {
			lst.setListData(new Object[0]);
		}
		fireValueChanged(obj);
		
		// update the view and remove actions
		enableAction(viewAction, (obj != null));
		enableAction(removeAction, (obj != null));
	}
	
	private void enableAction(Action action, boolean enable) {
		if (action != null) {
			action.setEnabled(enable);
		}
	}
	
	protected void fireValueChanged(Object value) {
		for (Iterator<QueryListComponentListener> iter = listeners.iterator(); iter.hasNext(); ){
			iter.next().valueChanged(value);
		}
	}

	/**
	 * Clears the list object.
	 */
	public void clearObject() {
		setObject(null);
	}
	
}
