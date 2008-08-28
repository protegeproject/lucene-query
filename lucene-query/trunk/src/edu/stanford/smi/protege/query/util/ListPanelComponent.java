package edu.stanford.smi.protege.query.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

/**
 * Holds a {@link JPanel} and optionally adds 3 buttons on the right: move up, remove, and move down.
 *
 * @author Chris Callendar
 * @date 14-Sep-06
 */
public class ListPanelComponent extends JPanel {

	private ListPanel listPanel;
	private JComponent pnlMain;
	private JToolBar rightToolbar;
	private boolean showArrowButtons;
	private boolean showCloseButton;
	
	private Action upAction;
	private Action downAction;
	private Action removeAction;

	/**
	 * Initializes this component.   Adds the move up, remove, and move down buttons.
	 * @param listPanel the parent {@link ListPanel} component which is used for moving this component up or down in the list
	 * @param mainPanel the panel to add to this component
	 */
	public ListPanelComponent(ListPanel listPanel, JComponent mainPanel) {
		this(listPanel, mainPanel, true, true);
	}

	/**
	 * Initializes this component.   
	 * @param listPanel the parent {@link ListPanel} component which is used for moving this component up or down in the list
	 * @param mainPanel the panel to add to this component
	 * @param showArrowButtons if the up and down arrow buttons should be shown
	 * @param showCloseButton if the close buttons should be shown
	 */
	public ListPanelComponent(ListPanel listPanel, JComponent mainPanel, boolean showArrowButtons, boolean showCloseButton) {
		super(new BorderLayout(2, 0));
		this.listPanel = listPanel;
		this.pnlMain = mainPanel;
		this.showArrowButtons = showArrowButtons;
		this.showCloseButton = showCloseButton;
		
		initialize();
	}

	/**
	 * Gets the main {@link JComponent} displayed in this component.
	 * @return JComponent
	 */
	public JComponent getMainPanel() {
		return pnlMain;
	}

	protected void initialize() {
		//setMinimumSize(new Dimension(100, 56));
		//setPreferredSize(new Dimension(500, 56));
		//setMaximumSize(new Dimension(5000, 56));
		
		add(pnlMain, BorderLayout.CENTER);
		
		// only add the right panel if some buttons are to be shown
		if (showArrowButtons || showCloseButton) {
			add(getRightButtonsPanel(), BorderLayout.EAST);
		}
	}
	
	public void setUpActionToolTip(String tt) {
		upAction.putValue(Action.SHORT_DESCRIPTION, tt);
	}

	public void setDownActionToolTip(String tt) {
		downAction.putValue(Action.SHORT_DESCRIPTION, tt);
	}

	public void setRemoveActionToolTip(String tt) {
		removeAction.putValue(Action.SHORT_DESCRIPTION, tt);
	}

	protected JToolBar getRightButtonsPanel() {
		if (rightToolbar == null) {
			rightToolbar = new JToolBar(JToolBar.VERTICAL);
			rightToolbar.setOpaque(false);
			rightToolbar.setRollover(true);
			rightToolbar.setFloatable(false);
			rightToolbar.setBorderPainted(false);
			rightToolbar.setBorder(null);

			rightToolbar.setPreferredSize(new Dimension(19, 56));
			rightToolbar.setMinimumSize(new Dimension(19, 56));
			rightToolbar.setMaximumSize(new Dimension(19, 5000));
			
			upAction = new AbstractAction(null, getIcon("up.gif")) {
				public void actionPerformed(ActionEvent e) {
					if (listPanel != null) {
						listPanel.moveUp(ListPanelComponent.this);
					}
				}
			};
			rightToolbar.add(addButton(upAction, "Move up", showArrowButtons, showArrowButtons));

			removeAction = new AbstractAction(null, getIcon("remove.gif")) {
				public void actionPerformed(ActionEvent e) {
					if (listPanel != null) {
						listPanel.removePanel(ListPanelComponent.this);
					}
				}
			};
			rightToolbar.add(addButton(removeAction, "Remove", showCloseButton, showCloseButton));
			
			downAction = new AbstractAction(null, getIcon("down.gif")) {
				public void actionPerformed(ActionEvent e) {
					if (listPanel != null) {
						listPanel.moveDown(ListPanelComponent.this);
					}
				}
			};
			rightToolbar.add(addButton(downAction, "Move down", showArrowButtons, showArrowButtons));
		}
		return rightToolbar;
	}
	
	protected JButton addButton(Action action, String text, boolean enabled, boolean visible) {
		JButton btn = new JButton(action);
		btn.setText(null);
		btn.setToolTipText(text);
		btn.setOpaque(false);
        btn.setRolloverEnabled(true);
        btn.setEnabled(enabled);
        btn.setVisible(visible);
        final Dimension dim = new Dimension(19, 18);
		btn.setPreferredSize(dim);
		btn.setMaximumSize(dim);
		btn.setMinimumSize(dim);
		//btn.setBorder(null);
		return btn;
	}

	protected Icon getIcon(String path) {
        URL url = ListPanelComponent.class.getResource(path);
        return (url != null ? new ImageIcon(url) : null);
	}
	
	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		if (pnlMain != null) {
			pnlMain.setBackground(bg);
		}
		if (rightToolbar != null) {
			rightToolbar.setBackground(bg);
		}
	}

	public static void main(String[] args) {
		JDialog dlg = new JDialog();
		dlg.setModal(true);
		dlg.setTitle("Test");
		
		final ListPanel listPanel = new ListPanel();
		dlg.getContentPane().add(new JScrollPane(listPanel), BorderLayout.CENTER);
		
		Action action = new AbstractAction("Add") {
			public void actionPerformed(ActionEvent e) {
				JPanel pnl = new JPanel();
				pnl.add(new JLabel("Label #" + (listPanel.getPanelCount()+1)));
				ListPanelComponent comp = new ListPanelComponent(listPanel, pnl, true, true);
				listPanel.addPanel(comp);
			}
		};
		
		for (int i = 1; i < 4; i++) {
			action.actionPerformed(null);
		}
		
		JPanel pnl = new JPanel(new FlowLayout());
		dlg.getContentPane().add(pnl, BorderLayout.SOUTH);
		pnl.add(new JButton(action));
		
		dlg.setSize(550, 250);
		dlg.setLocation(410, 200);
		dlg.setVisible(true);
		System.exit(0);
	}
	
}
