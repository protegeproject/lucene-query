package edu.stanford.smi.protege.query.querytypes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.ui.QueryUtil;
import edu.stanford.smi.protege.util.LocalizeUtils;

public class LuceneOwnSlotValueQuery implements VisitableQuery, BoundableQuery {
	private Collection<Slot> slots;
	private String expr;
	private int maxMatches = KnowledgeBase.UNLIMITED_MATCHES;
	
    public LuceneOwnSlotValueQuery(String expr) {
        this(new HashSet<Slot>(), expr, KnowledgeBase.UNLIMITED_MATCHES);
    }

	public LuceneOwnSlotValueQuery(Slot slot, String expr) {
	    this(Collections.singleton(slot), expr, KnowledgeBase.UNLIMITED_MATCHES);
	}
	
	public LuceneOwnSlotValueQuery(Slot slot, String expr, int maxMatches) {
        this(Collections.singleton(slot), expr, maxMatches);
	}
	
	public LuceneOwnSlotValueQuery(Collection<Slot> slots, String expr) {
	    this(slots, expr, KnowledgeBase.UNLIMITED_MATCHES);
	}
	
	public LuceneOwnSlotValueQuery(Collection<Slot> slots, String expr, int maxMatches) {
	    this.slots = slots;
	    this.expr = expr;
	    this.maxMatches  = maxMatches;
	}



	public void accept(QueryVisitor visitor) {
		visitor.visit(this);
	}

	public String getExpr() {
		return expr;
	}

	public Collection<Slot> getSlots() {
		return slots;
	}

	public int getMaxMatches() {
		return maxMatches;
	}

	public void setMaxMatches(int maxMatches) {
		this.maxMatches = maxMatches;
	}

	public LuceneOwnSlotValueQuery shallowClone() {
		return new LuceneOwnSlotValueQuery(slots, expr, maxMatches);
	}

	public void localize(KnowledgeBase kb) {
	    for (Slot slot : slots) {
	        LocalizeUtils.localize(slot, kb);
	    }
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
		for (Slot slot : slots) {
		    buffer.append(slot == null ? "(null slot)" : slot.getBrowserText());
		    buffer.append(", ");
		}
		buffer.append(" = ");
		buffer.append(expr);
		buffer.append("  [Lucene search] ");
		buffer.append(")");
		return buffer.toString();
	}

}
