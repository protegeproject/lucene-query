package edu.stanford.smi.protege.query.querytypes;

import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.ui.QueryUtil;

public class OrQuery implements VisitableQuery, BoundableQuery {

  Collection<VisitableQuery> disjuncts;
  int maxMatches = KnowledgeBase.UNLIMITED_MATCHES;

  public OrQuery(Collection<VisitableQuery> disjuncts) {
    this.disjuncts = disjuncts;
  }

  public void accept(QueryVisitor visitor) {
      visitor.visit(this);
  }

  public Collection<VisitableQuery> getDisjuncts() {
    return disjuncts;
  }

  public int getMaxMatches() {
	return maxMatches;
  }

  public void setMaxMatches(int maxMatches) {
	  this.maxMatches = maxMatches;
  }

  public OrQuery shallowClone() {
	  OrQuery q = new OrQuery(disjuncts);
	  q.setMaxMatches(getMaxMatches());
	  return q;
  }

  public void localize(KnowledgeBase kb) {
    for (Query q : disjuncts) {
      q.localize(kb);
    }
  }

	@Override
	public String toString() {
		return toString(0);
	}

	public String toString(int indent) {
	  StringBuffer indentStr = QueryUtil.getIndentString(indent);
	  if (disjuncts.size() == 0) {
			return indentStr.toString() + "(empty OR query)";
		}

		StringBuffer buffer = new StringBuffer();
		Iterator<VisitableQuery> it = disjuncts.iterator();
		while (it.hasNext()) {
			buffer.append(it.next().toString(indent));
			buffer.append("\n");
			buffer.append(indentStr);
			buffer.append("OR\n");
		}
		buffer.delete(buffer.length() - 4 - indent, buffer.length()); //remove last "or"
		return buffer.toString();
  }

}
