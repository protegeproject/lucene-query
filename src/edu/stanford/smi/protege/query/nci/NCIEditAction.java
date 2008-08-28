package edu.stanford.smi.protege.query.nci;

import java.awt.Cursor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.plugin.PluginUtilities;
import edu.stanford.smi.protege.ui.ProjectManager;
import edu.stanford.smi.protege.ui.ProjectView;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.Selectable;
import edu.stanford.smi.protege.util.ViewAction;
import edu.stanford.smi.protege.widget.TabWidget;

/**
 * This action is used to show the {@link NCIEditTab} and select a {@link Cls} in the tree.
 * It will only be enabled if at least one {@link Cls} is selected.
 *
 * @author Chris Callendar
 * @date 25-Sep-06
 */
public class NCIEditAction extends ViewAction {
    private static transient Logger log = Log.getLogger(NCIEditAction.class);

	protected static final String NCITAB = "gov.nih.nci.protegex.edit.NCIEditTab";

	public NCIEditAction(String text, Selectable selectable, Icon icon) {
		super(text, selectable, icon);
	}
	
	/**
	 * Determines if the needed NCI classes are available.
	 */
	public static boolean isValid() {
		boolean valid = true;
		try {
			PluginUtilities.forName(NCITAB, true);
		} catch (Throwable t) {
			valid = false;
		}
		return valid;
	}

    public void onView(final Object o) {
		ProjectView projectView = ProjectManager.getProjectManager().getCurrentProjectView();
		TabWidget tab = (TabWidget) projectView.getTabByClassName(NCITAB);
		if (tab != null) {
			performAction(tab, projectView, (Cls) o);
		}
	}
    
    protected void performAction(final TabWidget tab, final ProjectView projectView, final Cls cls) {
		final Cursor oldCursor = projectView.getCursor();
		projectView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// show the NCI tab
		projectView.setSelectedTab(tab);
		
		// run this later to let the tab change occur
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// select the Cls in the Class Browser tree
				
				// use reflection to remove dependency on NCI code
				try {
					Method getClassPanelMethod = tab.getClass().getMethod("getClassPanel", new Class[0]);
					Object clsPanel = getClassPanelMethod.invoke(tab, new Object[0]);
					Method setSelectedClsMethod = clsPanel.getClass().getMethod("setSelectedCls", new Class[] { Cls.class });
					setSelectedClsMethod.invoke(clsPanel, new Object[] { cls });
				} catch (Throwable t) {
					log.warning("Warning - couldn't view the selected Cls in the NCIEditTab");
					//t.printStackTrace();
				}
									
				// dependency on NCI code
				/*
				try {
					if (tab instanceof NCIEditTab) {
						NCIEditTab nciTab = (NCIEditTab) tab;
						nciTab.getClassPanel().setSelectedCls(cls);
					}
				} catch (Throwable e) {
					System.err.println("Warning - couldn't view the selected Cls in the NCIEditTab");
					//e.printStackTrace();
				}
				*/
				
				projectView.setCursor(oldCursor);
			}
		});    	
	}

	/** Only allow if a Cls is selected. */
	@Override
    public void onSelectionChange() {
    	if (!getSelection().isEmpty()) {
        	setAllowed(getSelection().iterator().next() instanceof Cls);
    	}
    }

}
