package edu.stanford.smi.protege.query.querytypes.impl;

import java.util.Collection;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.SubsetQuery;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;

public class NegatedQuery implements VisitableQuery, SubsetQuery {
	private static final long serialVersionUID = 5678986969057772810L;
	
	private VisitableQuery queryToNegate;
	private Collection<Frame> framesToSearch;

	public NegatedQuery(VisitableQuery q) {
		queryToNegate = q;
	}

	public void accept(QueryVisitor visitor) {
		visitor.visit(this);
	}
	
	public String toString(int indent) {
		return new StringBuffer("NOT(")
				        .append(queryToNegate.toString(indent))
				        .append(")").toString();
	}

	public void localize(KnowledgeBase kb) {
		queryToNegate.localize(kb);
	}
	
	public VisitableQuery getQueryToNegate() {
		return queryToNegate;
	}

	public Collection<Frame> getFramesToSearch() {
		return framesToSearch;
	}

	public void setFramesToSearch(Collection<Frame> allFrames) {
		framesToSearch = allFrames;
	}
}
