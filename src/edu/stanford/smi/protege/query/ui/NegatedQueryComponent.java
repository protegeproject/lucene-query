package edu.stanford.smi.protege.query.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import edu.stanford.smi.protege.query.querytypes.impl.NegatedQuery;
import edu.stanford.smi.protege.query.util.ListPanel;
import edu.stanford.smi.protege.query.util.ListPanelListener;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.util.LabeledComponent;

/**
 * The negated query panel.
 */
public class NegatedQueryComponent extends QueryBuildingJPanel {
	private static final long serialVersionUID = 4459233470406708315L;
	
	private KnowledgeBase kb;
	private LuceneQueryPlugin plugin;

	private LabeledComponent groupLabeledComponent;
	private ListPanel groupListPanel;
	
	private JRadioButton btnAndQuery;
	private JRadioButton btnOrQuery;
	
	public NegatedQueryComponent(KnowledgeBase kb, LuceneQueryPlugin plugin) {
		this.kb = kb;
		this.plugin = plugin;
		
		setLayout(new BorderLayout(0, 0));
		setDimensions();
		
		add(getGroupLabeledComponent(), BorderLayout.CENTER);
		addQueryComponent();
	}

	
	protected void setDimensions() {
		setMinimumSize(new Dimension(100, 56));
		setMaximumSize(new Dimension(5000, 500));
	}	
	
	private LabeledComponent getGroupLabeledComponent() {
		if (groupLabeledComponent == null) {
			ListPanel pnl = getGroupListPanel();
			groupLabeledComponent = new LabeledComponent("Negated Queries", new JScrollPane(pnl));
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
		}
		return groupListPanel;
	}

	
	private void addQueryComponent() {
		QueryUtil.addQueryComponent(kb, plugin, groupListPanel);
	}


	@Override
	public VisitableQuery getQuery() throws InvalidQueryException {
		VisitableQuery queryToNegate = QueryUtil.getQueryFromListPanel(groupListPanel, btnAndQuery.isSelected());
		return new NegatedQuery(queryToNegate);
	}

}
