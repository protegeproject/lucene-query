package edu.stanford.smi.protege.query.querytypes;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.ui.QueryUtil;
import edu.stanford.smi.protege.util.LocalizeUtils;

public class LuceneOwnSlotValueQuery implements VisitableQuery, BoundableQuery {
	private Slot slot;
	private String expr;
	private int maxMatches = KnowledgeBase.UNLIMITED_MATCHES;

	public LuceneOwnSlotValueQuery(Slot slot, String expr) {
		this.slot = slot;
		this.expr = expr;
	}

	public LuceneOwnSlotValueQuery(Slot slot, String expr, int maxMatches) {
		this.slot = slot;
		this.expr = expr;
		this.maxMatches = maxMatches;
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

	public int getMaxMatches() {
		return maxMatches;
	}

	public void setMaxMatches(int maxMatches) {
		this.maxMatches = maxMatches;
	}

	public LuceneOwnSlotValueQuery shallowClone() {
		return new LuceneOwnSlotValueQuery(slot, expr, maxMatches);
	}

	public void localize(KnowledgeBase kb) {
		LocalizeUtils.localize(slot, kb);
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
		buffer.append(slot == null ? "(null slot)" : slot.getBrowserText());
		buffer.append(" = ");
		buffer.append(expr);
		buffer.append("  [Lucene search] ");
		buffer.append(")");
		return buffer.toString();
	}

}
