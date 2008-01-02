package edu.stanford.smi.protege.query.querytypes;

import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.query.Query;

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
	StringBuffer buffer = new StringBuffer();
	for (Iterator iter = disjuncts.iterator(); iter.hasNext();) {
	  Query query = (Query) iter.next();
	  buffer.append(query.toString());
	}
	return "OrQuery { " + buffer.toString() + " }";
  }
  
}
