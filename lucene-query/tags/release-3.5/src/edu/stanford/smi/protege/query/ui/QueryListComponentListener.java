package edu.stanford.smi.protege.query.ui;

/**
 * Simple interface for being notified when a query component's value changes.
 *
 * @author Chris Callendar
 * @date 14-Sep-06
 */
public interface QueryListComponentListener {
	
	/**
	 * Indicates that a query value has changed.
	 * @param value
	 */
	public void valueChanged(Object value);
	
}
