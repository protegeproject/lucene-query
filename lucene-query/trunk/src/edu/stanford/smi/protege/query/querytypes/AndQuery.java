package edu.stanford.smi.protege.query.querytypes;

import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.ui.QueryUtil;

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
		return toString(0);
	}

	public String toString(int indent) {
		StringBuffer indentStr = QueryUtil.getIndentString(indent);
		if (conjuncts.size() == 0) {//rare case
			return indentStr.toString() + "(empty AND query)";
		}

		StringBuffer buffer = new StringBuffer();
		Iterator<VisitableQuery> it = conjuncts.iterator();
		while (it.hasNext()) {
			buffer.append(it.next().toString(indent));
			buffer.append("\n");
			buffer.append(indentStr);
			buffer.append("AND\n");
		}
		buffer.delete(buffer.length() - 5 - indent, buffer.length()); //remove last "and"
		return buffer.toString();
	}

}
