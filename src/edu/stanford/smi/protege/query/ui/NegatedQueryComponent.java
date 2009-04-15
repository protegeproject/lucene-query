package edu.stanford.smi.protege.query.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.query.LuceneQueryPlugin;
import edu.stanford.smi.protege.query.kb.InvalidQueryException;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.querytypes.impl.AndQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OrQuery;
import edu.stanford.smi.protege.query.util.ListPanel;
import edu.stanford.smi.protege.query.util.ListPanelListener;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protegex.owl.model.OWLProperty;

/**
 * Extends {@link QueryComponent} to show an OWL restriction query.  
 * This component lets the user choose an {@link OWLProperty}, 
 * and then create a Query (can be an {@link OrQuery} or an {@link AndQuery}).
 * It is only used with OWL projects.
 *
 * @author Chris Callendar
 * @date 25-Sep-06
 */
public class NegatedQueryComponent extends QueryBuildingJPanel {
	private static final long serialVersionUID = 4459233470406708315L;
	
	private KnowledgeBase kb;
	private LuceneQueryPlugin plugin;
	
	private JPanel pnlQueryComponents;
	private LabeledComponent groupLabeledComponent;
	private ListPanel groupListPanel;
	
	private JRadioButton btnAndQuery;
	private JRadioButton btnOrQuery;
	
	public NegatedQueryComponent(KnowledgeBase kb, LuceneQueryPlugin plugin) {
		this.kb = kb;
		this.plugin = plugin;
		initialize();
		addQueryComponent();
	}
	
	
	private void initialize() {
		
		final JPanel pnl = getQueryComponentsPanel();
		pnl.setMinimumSize(new Dimension(60, 56));
		pnl.setPreferredSize(new Dimension(500, 56));
		pnl.setMaximumSize(new Dimension(5000, 56));

		// move the query components panel to be at the top
		remove(pnl);
		add(pnl, BorderLayout.NORTH);
		// add the group list panel
		add(getGroupLabeledComponent(), BorderLayout.CENTER);
		
	}		
	
	
	protected void setDimensions() {
		setMinimumSize(new Dimension(100, 56));
		//setPreferredSize(new Dimension(500, 200));
		setMaximumSize(new Dimension(5000, 500));
	}	
	
	private LabeledComponent getGroupLabeledComponent() {
		if (groupLabeledComponent == null) {
			ListPanel pnl = getGroupListPanel();
			groupLabeledComponent = new LabeledComponent("Queries", new JScrollPane(pnl));
			groupLabeledComponent.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createEmptyBorder(0, 2, 0, 2), 
					BorderFactory.createLoweredBevelBorder()));
			
			JButton btn = groupLabeledComponent.addHeaderButton(new AbstractAction("Add another query", Icons.getAddQueryLibraryIcon()) {
				public void actionPerformed(ActionEvent e) {
					addQueryComponent();
				}
			});
			btn.setText("Add Query");
			// have to change the sizes to show the text
			final Dimension dim = new Dimension(100, btn.getPreferredSize().height);
			btn.setMinimumSize(dim);
			btn.setPreferredSize(dim);
			btn.setMaximumSize(dim);
			
			btnAndQuery = new JRadioButton("Match All ", false);
			btnOrQuery = new JRadioButton("Match Any ", true);
			btn.getParent().add(btnAndQuery);
			btn.getParent().add(btnOrQuery);
			ButtonGroup group = new ButtonGroup();
			group.add(btnAndQuery);
			group.add(btnOrQuery);
			
		}
		return groupLabeledComponent;
	}
	
	public ListPanel getGroupListPanel() {
		if (groupListPanel == null) {
			groupListPanel = new ListPanel(20, false);
			// ensure that there is always one query panel
			groupListPanel.addListener(new ListPanelListener() {
				public void panelAdded(JPanel panel, ListPanel listPanel) {}
				public void panelRemoved(JPanel comp, ListPanel listPanel) {
					if (listPanel.getPanelCount() == 0) {
						addQueryComponent();
					}
				};
			});
			groupListPanel.setPreferredSize(new Dimension(400, 150));
			//groupListPanel.setMaximumSize(new Dimension(5000, 300));
		}
		return groupListPanel;
	}

	protected JPanel getQueryComponentsPanel() {
		if (pnlQueryComponents == null) {
			pnlQueryComponents = new JPanel(new GridLayout(1, /* 4 */ 3, 5, 0));
		}
		return pnlQueryComponents;
	}
	
	private void addQueryComponent() {
		QueryUtil.addQueryComponent(kb, plugin, groupListPanel);
	}


	@Override
	public VisitableQuery getQuery() throws InvalidQueryException {
		throw new UnsupportedOperationException();
	}

}
