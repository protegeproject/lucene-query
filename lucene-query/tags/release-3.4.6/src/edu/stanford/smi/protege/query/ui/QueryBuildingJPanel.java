package edu.stanford.smi.protege.query.ui;

import javax.swing.JPanel;

import edu.stanford.smi.protege.query.kb.InvalidQueryException;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;

public abstract class QueryBuildingJPanel extends JPanel {
	private static final long serialVersionUID = 5805022150953895151L;

	public abstract VisitableQuery getQuery() throws InvalidQueryException;
}
