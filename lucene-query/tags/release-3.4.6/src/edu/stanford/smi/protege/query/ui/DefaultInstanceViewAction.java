package edu.stanford.smi.protege.query.ui;

import javax.swing.Icon;

import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.util.Selectable;
import edu.stanford.smi.protege.util.ViewAction;

/**
 * Displays the selected {@link Instance} in the default Protege instance viewer (popup dialog).
 * 
 * @author Chris Callendar
 * @date 17-Jan-07
 */
public class DefaultInstanceViewAction extends ViewAction {

	private KnowledgeBase kb;
	
	public DefaultInstanceViewAction(String text, Selectable selectable, Icon icon, KnowledgeBase kb) {
		super(text, selectable, icon);
		this.kb = kb;
	}
	
	@Override
	public void onView(Object o) {
		kb.getProject().show((Instance)o);
	}
	
	@Override
	public void onSelectionChange() {
		if (!getSelection().isEmpty()) {
			setAllowed(getSelection().iterator().next() instanceof Instance);
		}
	}
	
}
