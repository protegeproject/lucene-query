package edu.stanford.smi.protege.query.querytypes;

import edu.stanford.smi.protege.model.KnowledgeBase;

public class MaxMatchQuery implements VisitableQuery {
	private int maxMatches;
	private VisitableQuery innerQuery;

	public MaxMatchQuery(VisitableQuery innerQuery, int maxMatches) {
		this.maxMatches = maxMatches;
		if (innerQuery instanceof BoundableQuery) {
			BoundableQuery iq = ((BoundableQuery) innerQuery).shallowClone();
			iq.setMaxMatches(maxMatches);
			this.innerQuery = iq;
		}
		else {
			this.innerQuery = innerQuery;
		}
	}

	public int getMaxMatches() {
		return maxMatches;
	}

	public boolean innerQueryBounded() {
		return innerQuery instanceof BoundableQuery;
	}

	public VisitableQuery getInnerQuery() {
		return innerQuery;
	}


	public void accept(QueryVisitor visitor) {
		visitor.visit(this);
	}

	public void localize(KnowledgeBase kb) {
		innerQuery.localize(kb);
	}

	@Override
	public String toString() {
		return toString(0);
	}

	public String toString(int indent) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(innerQuery.toString(indent));
		buffer.append(" [max matches: ]");
		buffer.append(maxMatches);
		return buffer.toString();
	}

}
