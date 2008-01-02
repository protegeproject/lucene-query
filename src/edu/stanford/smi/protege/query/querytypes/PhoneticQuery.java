package edu.stanford.smi.protege.query.querytypes;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.util.LocalizeUtils;

public class PhoneticQuery implements VisitableQuery {

  private Slot slot;
  private String expr;
  
  public PhoneticQuery(Slot slot, String expr) {
    this.slot = slot;
    this.expr = expr;
  }
  
  public void accept(QueryVisitor visitor) {
      visitor.visit(this);
  }
  
  public String getExpr() {
    return expr;
  }

  public Slot getSlot() {
    return slot;
  }

  public void localize(KnowledgeBase kb) {
    LocalizeUtils.localize(slot, kb);
  }

  @Override
  public String toString() {
	return "PhoneticQuery: slot=" + (slot != null ? slot.getBrowserText() : "null") + ", expression=" + expr;
  }
  
}
