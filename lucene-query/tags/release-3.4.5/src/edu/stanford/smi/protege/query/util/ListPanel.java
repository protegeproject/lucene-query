package edu.stanford.smi.protege.query.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;



/**
 * This class is meant to immitate a {@link JList} which contains {@link JPanel} objects 
 * that can be selected and moved up or down. 
 *
 * @author Chris Callendar
 * @date 13-Sep-06
 */
public class ListPanel extends JPanel {

	public static final Color DEFAULT_COLOR_SELECTED = new Color(240, 230, 202);
	public static final Color DEFAULT_COLOR = new Color(236, 233, 216);
	public static final Color DEFAULT_BORDER_COLOR = new Color(245, 165, 16);
	private static final Border BORDER_NOFOCUS = new CustomLineBorder(Color.lightGray, null, Color.lightGray, null, 1);

	class MouseFocus extends MouseAdapter implements FocusListener {
		public void focusLost(FocusEvent e) {}
		public void focusGained(FocusEvent e) {
			if (e.getSource() instanceof JPanel) {
				focusPanel((JPanel) e.getSource());
			}
		}
		public void mouseClicked(MouseEvent e) {
			if (e.getSource() instanceof JPanel) {
				focusPanel((JPanel) e.getSource());
			}
		}
		private void focusPanel(JPanel pnl) {
			if (pnl != focussedPnl) {
				if (focussedPnl != null) {
					unfocus(focussedPnl);
				}
				focussedPnl = pnl;
			}
			focus(focussedPnl);
		}
	}
	
	private ArrayList<ListPanelListener> listeners;
	private ArrayList<JPanel> panels = new ArrayList<JPanel>();
	private Box emptyBox = null;
	private MouseFocus listener = new MouseFocus();
	private JPanel focussedPnl = null;
	private final int minimumHeight;
	
	private Border borderFocus = BorderFactory.createLineBorder(DEFAULT_BORDER_COLOR, 1);
	private Color selectedColor = DEFAULT_COLOR_SELECTED;
	private Color normalColor = DEFAULT_COLOR;
	private final boolean changeColorOnSelection;
	private final boolean highlightSelectedPanel;
	
	/**
	 * Initializes this panel with a minimum height of 200 and true for changeColorOnSelection.
	 */
	public ListPanel() {
		this(200, true, true);
	}

	/**
	 * Initializes this panel.
	 * @param minHeight the minimum height for the panel
	 * @param highlightSelectedPanel if the selected/focussed panel should be highlighted with a border and a different bg color
	 * @see ListPanel#setFocusBorderColor(Color)
	 */
	public ListPanel(int minHeight, boolean highlightSelectedPanel) {
		this(minHeight, highlightSelectedPanel, highlightSelectedPanel);
	}
	
	/**
	 * Initializes this panel.
	 * @param minHeight the minimum height for the panel
	 * @param highlightSelectedPanel if the selected/focussed panel should be highlighted with a border and possibly a bg color
	 * @param changeColorOnSelection if true then all children of the selected panel are colored in the selected color
	 * @see ListPanel#setFocusBorderColor(Color)
	 * @see ListPanel#setSelectedPanelColor(Color)
	 * @see ListPanel#setPanelColor(Color)
	 */
	public ListPanel(int minHeight, boolean highlightSelectedPanel, boolean changeColorOnSelection) {
		super();

		this.minimumHeight = minHeight;
		this.highlightSelectedPanel = highlightSelectedPanel;
		this.changeColorOnSelection = changeColorOnSelection;
		this.listeners = new ArrayList<ListPanelListener>(1);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(new Color(240, 240, 240));		
		add(getEmptyBox());
	}
	
	public void addListener(ListPanelListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(ListPanelListener listener) {
		this.listeners.remove(listener);
	}
	
	public void setSelectedPanelColor(Color c) {
		this.selectedColor = c;
	}
	
	public void setPanelColor(Color c) {
		this.normalColor = c;
	}
	
	public void setFocusBorderColor(Color c) {
		c = (c == null ? DEFAULT_BORDER_COLOR : c);
		borderFocus = BorderFactory.createLineBorder(c, 1);
	}
	
	public void addPanel(JPanel pnl) {
		addPanel(pnl, panels.size());
	}

	public void addPanel(final JPanel pnl, int index) {
		remove(emptyBox);
		if (index <= panels.size()) {
			panels.add(index, pnl);
			add(pnl, index);
		} else {
			panels.add(pnl);
			add(pnl);
		}
		add(emptyBox);
		firePanelAdded(pnl);

		adjustHeight();
		
		// only add the focus listeners if we want to highlight the selected panel
		if (highlightSelectedPanel) {
			pnl.addMouseListener(listener);
			pnl.addFocusListener(listener);
			pnl.setFocusable(true);
			ListPanel.addFocusListenerRecursively(pnl, new FocusAdapter() {
				public void focusGained(FocusEvent e) {
					listener.focusPanel(pnl);
				}
			});
			unfocus(pnl);
		} else {
			pnl.setBorder(BORDER_NOFOCUS);
		}
		revalidate();
	}
	
	/** adjust the height to ensure that all the panels are visible */
	private void adjustHeight() {
		int height = emptyBox.getPreferredSize().height;
		for (JPanel p : panels) {
			height += p.getPreferredSize().height;
		}
		setPreferredSize(new Dimension(getPreferredSize().width, height));
	}
	
	protected void firePanelAdded(JPanel pnl) {
		for (Iterator<ListPanelListener> iter = listeners.iterator(); iter.hasNext(); ) {
			iter.next().panelAdded(pnl, this);
		}
	}

	protected void firePanelRemoved(JPanel pnl) {
		for (Iterator<ListPanelListener> iter = listeners.iterator(); iter.hasNext(); ) {
			iter.next().panelRemoved(pnl, this);
		}
	}
	
	public void removePanel(JPanel pnl) {
		if (panels.remove(pnl)) {
			remove(pnl);
			if (highlightSelectedPanel) {
				pnl.removeMouseListener(listener);
				pnl.removeFocusListener(listener);
			}
			if (pnl == focussedPnl) {
				focussedPnl = null;
			}
			firePanelRemoved(pnl);
			
			revalidate();
			repaint();
		}
	}

	public void removePanel(int index) {
		if ((index >= 0) && (index < panels.size())) {
			removePanel((JPanel)panels.get(index));
		}
	}
	

	public void removeAllPanels() {
		panels.clear();
		removeAll();
		add(emptyBox);
		focussedPnl = null;
	}		
	
	public void reloadPanels() {
		removeAll();
		for (int i = 0; i < panels.size(); i++) {
			add(panels.get(i));
		}
		add(emptyBox);
		revalidate();
		repaint();
		if (focussedPnl != null) {
			focussedPnl.requestFocus();
		}
	}
	
	private Box getEmptyBox() {
		if (emptyBox == null) {
			emptyBox = Box.createVerticalBox();
		    emptyBox.add(Box.createVerticalGlue());
		    emptyBox.add(Box.createVerticalStrut(minimumHeight));
		}
		return emptyBox;
	}	
	
	private static void addFocusListenerRecursively(Container container, FocusListener listener) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component child = container.getComponent(i);
			child.addFocusListener(listener);
			if (child instanceof Container) {
				addFocusListenerRecursively((Container)child, listener);					
			}
		}
	}
	
	public static void removeFocusListenerRecursively(Container container, FocusListener listener) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component child = container.getComponent(i);
			child.removeFocusListener(listener);
			if (child instanceof Container) {
				removeFocusListenerRecursively((Container)child, listener);					
			}
		}
	}	
	
	private void focus(JPanel pnl) {
		if (highlightSelectedPanel) {
			pnl.setBorder(borderFocus);
			if (changeColorOnSelection) {
				setColorRecursively(pnl, selectedColor);
			}
		}
	}
	
	private void unfocus(JPanel pnl) {
		if (highlightSelectedPanel) {
			pnl.setBorder(BORDER_NOFOCUS);
			if (changeColorOnSelection) {
				setColorRecursively(pnl, normalColor);
			}
		}
	}
	
	/** 
	 * Recursively sets the background color of the container and its children.
	 * {@link JTextComponent}, {@link JList}, and {@link JTable} are not colored. 
	 */
	private static void setColorRecursively(Container c, Color color) {
		c.setBackground(color);
		for (int i = 0; i < c.getComponentCount(); i++) {
			Component comp = c.getComponent(i);
			if ((comp instanceof Container) && !(comp instanceof JTextComponent) && 
				!(comp instanceof JList) && !(comp instanceof JTable) && !(comp instanceof ListPanel)) {
				Container child = (Container) comp;
				setColorRecursively(child, color);
			}
		}
	}

	public void moveSelectedUp() {
		JPanel pnl = focussedPnl;
		moveUp(pnl);
	}
	
	public void moveUp(JPanel pnl) {
		if (pnl != null) {
			int index = -1;
			for (int i = 0; i < panels.size(); i++) {
				if (pnl == panels.get(i)) {
					index = i;
					break;
				}
			}
			if (index > 0) {
				removePanel(pnl);
				addPanel(pnl, index - 1);
				pnl.requestFocus();
			}
		}
	}	

	public void moveSelectedDown() {
		JPanel pnl = focussedPnl;
		moveDown(pnl);
	}
	
	public void moveDown(JPanel pnl) {
		if (pnl != null) {
			int index = -1;
			for (int i = 0; i < panels.size(); i++) {
				if (pnl == panels.get(i)) {
					index = i;
					break;
				}
			}
			if ((index >= 0) && ((index + 1) < panels.size())) {
				removePanel(pnl);
				addPanel(pnl, index + 1);
				pnl.requestFocus();
			}
		}
	}	

	public int getPanelCount() {
		return panels.size();
	}

	public Collection<JPanel> getPanels() {
		return panels;
	}

}
