package edu.stanford.smi.protege.query.querytypes;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.util.LocalizeUtils;

public class NestedOwnSlotValueQuery implements VisitableQuery {
    Slot slot;
    VisitableQuery innerQuery;

    public NestedOwnSlotValueQuery(Slot slot, VisitableQuery innerQuery) {
        this.slot = slot;
        this.innerQuery = innerQuery;
    }

    public Slot getSlot() {
        return slot;
    }

    public VisitableQuery getInnerQuery() {
        return innerQuery;
    }

    public void accept(QueryVisitor visitor) {
        visitor.visit(this);
    }

    public void localize(KnowledgeBase kb) {
        LocalizeUtils.localize(slot, kb);
        innerQuery.localize(kb);
    }

    @Override
    public String toString() {
    	StringBuffer buffer = new StringBuffer();
    	buffer.append("(");
		buffer.append(slot == null ? "(null slot)" : slot.getBrowserText());
		buffer.append(" has ");
		buffer.append(innerQuery.toString());
		buffer.append(")");
    	return buffer.toString();
    }

}
