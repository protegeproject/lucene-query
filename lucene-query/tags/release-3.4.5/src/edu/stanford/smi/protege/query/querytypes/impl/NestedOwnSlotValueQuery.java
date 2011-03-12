package edu.stanford.smi.protege.query.querytypes.impl;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.ui.QueryUtil;
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
		return toString(0);
	}

	public String toString(int indent) {
		StringBuffer indentStr = QueryUtil.getIndentString(indent);
		StringBuffer buffer = new StringBuffer();
		buffer.append(indentStr);
		buffer.append("(");
		buffer.append("\n");
		buffer.append(slot == null ? "(null slot)" : slot.getBrowserText());
		buffer.append(innerQuery.toString(indent + 4));
		buffer.append("\n");
		buffer.append(indentStr);
		buffer.append(")");
		return buffer.toString();
	}

}
