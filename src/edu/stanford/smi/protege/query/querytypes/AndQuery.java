package edu.stanford.smi.protege.query.querytypes;

import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;

public class AndQuery implements VisitableQuery {
	public Collection<VisitableQuery> conjuncts;

	public AndQuery(Collection<VisitableQuery> conjuncts) {
		this.conjuncts = conjuncts;
	}

	public void accept(QueryVisitor visitor) {
		visitor.visit(this);
	}


	public Collection<VisitableQuery> getConjuncts() {
		return conjuncts;
	}

	public void localize(KnowledgeBase kb) {
		for (Query q : conjuncts) {
			q.localize(kb);
		}
	}

	@Override
	public String toString() {
		if (conjuncts.size() == 0) {
			return "(empty AND query)";
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append("(");
		Iterator<VisitableQuery> it = conjuncts.iterator();
		while (it.hasNext()) {
			buffer.append(it.next().toString());
			buffer.append(" and\n");
		}
		buffer.delete(buffer.length() - 5, buffer.length()); //remove last "and"
		buffer.append(")");
		return buffer.toString();
	}

}
