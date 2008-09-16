package edu.stanford.smi.protege.query.ui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JPanel;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.LuceneQueryPlugin;
import edu.stanford.smi.protege.query.kb.InvalidQueryException;
import edu.stanford.smi.protege.query.menu.QueryUIConfiguration;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.util.ListPanel;
import edu.stanford.smi.protege.query.util.ListPanelComponent;
import edu.stanford.smi.protegex.owl.model.OWLModel;

/**
 * A collection of useful methods when working with queries and {@link QueryComponent}s.
 *
 * @author Chris Callendar
 * @date 22-Sep-06
 */
public final class QueryUtil {

	/**
	 * Gets a {@link Query} from a {@link ListPanel}.   If any query is invalid then null is returned.
	 * @param listPanel the list panel
	 * @param andQuery if multiple queries exist then it will either create an {@link AndQuery} or an {@link OrQuery}.
	 * @return {@link Query} or null if no query was valid
	 * @throws InvalidQueryException if one of the query objects is invalid
	 */
	@SuppressWarnings("unchecked")
	public static VisitableQuery getQueryFromListPanel(ListPanel listPanel, boolean andQuery) throws InvalidQueryException {
		VisitableQuery query = null;
		Collection<JPanel> panels = listPanel.getPanels();
		ArrayList<VisitableQuery> queries = new ArrayList<VisitableQuery>(panels.size());
		for (Object element : panels) {
			ListPanelComponent comp = (ListPanelComponent) element;
			QueryComponent qc = (QueryComponent) comp.getMainPanel();
			// this throws InvalidQueryException
			VisitableQuery q = qc.getQuery();
			queries.add(q);
		}
		if (queries.size() == 1) {
			query = queries.get(0);
		} else if (queries.size() > 1) {
			query = andQuery ? new AndQuery(queries) : new OrQuery(queries);
		}
		return query;
	}

	public static void addQueryComponent(KnowledgeBase kb, LuceneQueryPlugin plugin, ListPanel listPanel) {
		addQueryComponent(kb, plugin, listPanel, "");
	}

	/**
	 * Adds a new query component to the {@link ListPanel}.
	 * @param kb the {@link KnowledgeBase} or {@link OWLModel}
	 * @param slots the allowed slots
	 * @param defaultSlot the slot to display by default (must be contained in the slots Collection)
	 * @param listPanel the {@link ListPanel} to add too
	 * @param defaultVaule the default value to be searched for as a String
	 */
	public static void addQueryComponent(KnowledgeBase kb, LuceneQueryPlugin plugin, ListPanel listPanel, String defaultValue) {
		QueryComponent qc = new QueryComponent(kb, plugin, defaultValue);
		ListPanelComponent comp = new ListPanelComponent(listPanel, qc, false, true);
		comp.setMinimumSize(new Dimension(60, 56));
		comp.setPreferredSize(new Dimension(500, 56));
		comp.setMaximumSize(new Dimension(5000, 56));
		comp.setRemoveActionToolTip("Remove query");
		listPanel.addPanel(comp);
	}



	/**
	 * Adds a new OWL restriction query component to the {@link ListPanel}.
	 * @param model the {@link OWLModel}
	 * @param slots the allowed slots for the restriction queries
	 * @param defaultSlot the slot to display by default (must be contained in the slots Collection)
	 * @param listPanel the {@link ListPanel} to add too
	 */
	public static void addRestrictionQueryComponent(OWLModel model, LuceneQueryPlugin plugin, ListPanel listPanel) {
		OWLRestrictionQueryComponent qc = new OWLRestrictionQueryComponent(model, plugin);
		ListPanelComponent comp = new ListPanelComponent(listPanel, qc, false, true);
		comp.setMinimumSize(new Dimension(60, 100));
		comp.setPreferredSize(new Dimension(500, 200));
		comp.setMaximumSize(new Dimension(5000, 500));
		comp.setRemoveActionToolTip("Remove query");
		listPanel.addPanel(comp);
	}

	/**
	 * Tries to guess the query type for ordinary queries (not Lucene),
	 * based on the query string.
	 * @param queryString - the query string
	 * @return - one of: "contains", "starts with", "ends with", "exact match"
	 */
	public static String getQueryType(String queryString) {
		if (queryString.startsWith("*") && queryString.endsWith("*")) {
			return "contains";
		}
		if (queryString.startsWith("*")) {
			return "starts with";
		}
		if (queryString.endsWith("*")) {
			return "ends with";
		}
		return "exact match";
	}

	public static StringBuffer getIndentString(int indent) {
		StringBuffer buffer = new StringBuffer(indent);
		for (int i = 0; i < indent; i++) {
			buffer.append(' ');
		}
		return buffer;
	}

}
