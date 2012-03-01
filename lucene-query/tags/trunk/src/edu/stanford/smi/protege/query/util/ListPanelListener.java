package edu.stanford.smi.protege.query.util;

import javax.swing.JPanel;


/**
 * Notifies listeners when {@link JPanel} get added and removed from a {@link ListPanel}.
 *
 * @author Chris Callendar
 * @date 15-Sep-06
 */
public interface ListPanelListener {

	/**
	 * Notifies listeners that a panel was added.
	 * @param panel the added panel
	 * @param listPanel the list panel (for convenience)
	 */
	public void panelAdded(JPanel panel, ListPanel listPanel);
	
	/**
	 * Notifies listeners that a panel was removed.
	 * @param panel the removed panel
	 * @param listPanel the list panel (for convenience)
	 */
	public void panelRemoved(JPanel comp, ListPanel listPanel);
	
}
