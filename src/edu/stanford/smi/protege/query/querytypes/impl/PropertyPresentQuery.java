package edu.stanford.smi.protege.query.querytypes.impl;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.util.LocalizeUtils;

public class PropertyPresentQuery implements VisitableQuery {
	private static final long serialVersionUID = -2595004152848892344L;
	
	private Slot p;
	
	public PropertyPresentQuery(Slot p) {
		this.p = p;
	}

	public void accept(QueryVisitor visitor) {
		visitor.visit(this);
	}

	public String toString(int indent) {
		return new StringBuffer("PropertyPresent(")
					.append(p.getBrowserText())
					.append(")").toString();
	}

	public void localize(KnowledgeBase kb) {
		LocalizeUtils.localize(p, kb);
	}
	
	public Slot getProperty() {
		return p;
	}

}
