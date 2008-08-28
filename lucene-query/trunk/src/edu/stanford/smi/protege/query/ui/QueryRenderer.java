package edu.stanford.smi.protege.query.ui;

import java.awt.Color;

import javax.swing.ListCellRenderer;

import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.OWLRestrictionQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;

public interface QueryRenderer extends ListCellRenderer {

	/**
	 * Sets the color to use for matched strings.
	 * If null then the string is painted with same color (default).
	 * @param c the color to paint matched strings
	 */
	public void setMatchColor(Color c);

	public Color getMatchColor();

	/**
	 * Sets the color to use for the "Searching..." string.
	 * If null then the string is painted with same color (default).
	 * @param c the color to paint the "Searching..." string
	 */
	public void setSearchColor(Color c);

	public Color getSearchColor();

	/**
	 * Sets whether matches should be painted using a bold font.
	 * This is true by default.
	 */
	public void setBoldMatches(boolean useBolding);

	public boolean isBoldMatches();

	/**
	 * Sets the query string based on the {@link Query}.
	 * At the moment {@link OWLRestrictionQuery} are not dealt with.
	 * If the query is an {@link OrQuery} or an {@link AndQuery}
	 * then each query string is combined together.  
	 * @param q the query
	 */
	public void setQuery(Query q);

	/**
	 * Sets the query string to use.  This will be 
	 * matched against any text that is rendered and the matching
	 * parts will be highlighted.
	 * @param string
	 */
	public void setQueryString(String string);

}