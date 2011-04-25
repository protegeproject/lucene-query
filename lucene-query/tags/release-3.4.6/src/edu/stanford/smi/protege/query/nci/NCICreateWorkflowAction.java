/**
 * 
 */
package edu.stanford.smi.protege.query.nci;

import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Logger;

import edu.stanford.smi.protege.plugin.PluginUtilities;
import edu.stanford.smi.protege.ui.ProjectManager;
import edu.stanford.smi.protege.ui.ProjectView;
import edu.stanford.smi.protege.util.AllowableAction;
import edu.stanford.smi.protege.util.FrameWithBrowserText;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.SelectableList;
import edu.stanford.smi.protege.widget.TabWidget;

/**
 * @author bitdiddle
 * 
 */
public class NCICreateWorkflowAction extends AllowableAction {
	
	public static final long serialVersionUID = 123458892L;

	private static transient Logger log = Log.getLogger(NCIEditAction.class);
	
	protected static final String NCITAB = "gov.nih.nci.protegex.edit.NCIEditTab";

	public NCICreateWorkflowAction(String name, SelectableList list) {
		super(name, list);
	}
	
	public static boolean isValid() {
		return PluginUtilities.forName(NCITAB, true) != null;
	}

	public void actionPerformed(ActionEvent e) {
		
		try {
			
			//use reflection to remove dependency on NCI code
			try {
				
				ProjectView projectView = ProjectManager.getProjectManager().getCurrentProjectView();
				TabWidget tab = projectView.getTabByClassName(NCITAB);
				Method getClassPanelMethod = tab.getClass().getMethod("showSuggestionDialog", new Class[] {String.class, Collection.class});
				getClassPanelMethod.invoke(tab, new Object[] {"Concepts Need Updating", getSelection()});
				
			} catch (Throwable t) {
				log.warning("Warning - couldn't invoke showSuggestionDialog in NCIEditTab");
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public Collection getSelection() {
		return FrameWithBrowserText.getFrames(super.getSelection());
	}

}
