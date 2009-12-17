package edu.stanford.smi.protege.query.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.ValueType;
import edu.stanford.smi.protege.query.LuceneQueryPlugin;
import edu.stanford.smi.protege.query.kb.InvalidQueryException;
import edu.stanford.smi.protege.query.menu.SlotFilterType;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.querytypes.impl.AndQuery;
import edu.stanford.smi.protege.query.querytypes.impl.NestedOwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OWLRestrictionQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OrQuery;
import edu.stanford.smi.protege.query.util.ListPanel;
import edu.stanford.smi.protege.query.util.ListPanelListener;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFProperty;

/**
 * Extends {@link QueryComponent} to show an OWL restriction query.  
 * This component lets the user choose an {@link OWLProperty}, 
 * and then create a Query (can be an {@link OrQuery} or an {@link AndQuery}).
 * It is only used with OWL projects.
 *
 * @author Chris Callendar
 * @date 25-Sep-06
 */
public class OWLRestrictionOrOwnSlotQuery extends QueryComponent {

	private LabeledComponent groupLabeledComponent;
	private ListPanel groupListPanel;
	private JLabel queryTypeLabel;
	
	private JRadioButton btnAndQuery;
	private JRadioButton btnOrQuery;
	
	public OWLRestrictionOrOwnSlotQuery(OWLModel model, LuceneQueryPlugin plugin) {
		super(model, plugin, SlotFilterType.PROPERTIES_NOT_TAKING_DATA_VALUES);
		// add the default component (must be after searchable slot is set)
		addQueryComponent();
	}
	
	protected OWLModel getOWLModel() {
		return (OWLModel) getKnowledgeBase();
	}

	@Override
	protected VisitableQuery getQueryForType(Slot slot, ValueType type) throws InvalidQueryException {
		VisitableQuery query = QueryUtil.getQueryFromListPanel(groupListPanel, btnAndQuery.isSelected());
		OWLProperty property = (OWLProperty) slot;
		if (query != null) {
		    if (property.isAnnotationProperty()) {
		        return new NestedOwnSlotValueQuery(property, query);
		    }
		    else {
		        return new OWLRestrictionQuery(getOWLModel(), property, query);
		    }
		}
		return null;
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		
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
	
	@Override
	protected QueryListComponent getSelectSlotComponent() {
		if (selectSlot == null) {
			if (queryTypeLabel == null) {
				queryTypeLabel = new JLabel();
			}
			queryTypeLabel = new JLabel();
			selectSlot = new QueryListComponent("OWLProperty", getKnowledgeBase());
			selectSlot.setActions(createViewAction(selectSlot, "View Property", Icons.getViewSlotIcon()), 
								  createSelectSlotAction(selectSlot, "Select Property", Icons.getAddSlotIcon()),
								  createRemoveAction(selectSlot, "Remove Property", Icons.getRemoveSlotIcon()));
			selectSlot.addListener(new QueryListComponentListener() {
				public void valueChanged(Object value) {
					if (value != null &&
							value  instanceof RDFProperty) {
						queryTypeLabel.setText(((RDFProperty) value).isAnnotationProperty() ?
								"Property Value Query" :
									"Property Restriction Query");
					}
				}
			});
		}
		return selectSlot;
	}	
	
	/** Overridden to return a blank component. */
	@Override
	protected LabeledComponent getTypesComponent() {
		if (queryTypeLabel == null) {
			queryTypeLabel = new JLabel();
		}
		if (typesComponent == null) {
			typesComponent = new QueryLabeledComponent("", queryTypeLabel);
		}
		return typesComponent;
	}
	
	@Override
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
			
			JButton btn2 = groupLabeledComponent.addHeaderButton(new AbstractAction("Add Negated Query", Icons.getAddQueryLibraryIcon()) {
				public void actionPerformed(ActionEvent e) {
					addNegatedQueryComponent();
				}
			});
			btn2.setText("Add Negated Query");
			// have to change the sizes to show the text
			final Dimension dim2 = new Dimension(100, btn2.getPreferredSize().height);
			btn2.setMinimumSize(dim2);
			btn2.setPreferredSize(dim2);
			btn2.setMaximumSize(dim2);
			
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

	private void addQueryComponent() {
		QueryUtil.addQueryComponent(getKnowledgeBase(), getLuceneQueryPlugin(), groupListPanel, SlotFilterType.DIRECT_OWN_VALUE_PROPERTIES_APPLICABLE_TO_CLASSES);
	}
	
	private void addNegatedQueryComponent() {
		QueryUtil.addNegatedNestedQuery(getKnowledgeBase(), getLuceneQueryPlugin(), groupListPanel, SlotFilterType.DIRECT_OWN_VALUE_PROPERTIES_APPLICABLE_TO_CLASSES);
	}

}
