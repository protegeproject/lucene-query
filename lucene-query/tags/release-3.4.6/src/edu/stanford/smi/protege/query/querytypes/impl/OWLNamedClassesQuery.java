package edu.stanford.smi.protege.query.querytypes.impl;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.ui.QueryUtil;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class OWLNamedClassesQuery implements VisitableQuery {
    private transient OWLModel owlModel;

    public OWLNamedClassesQuery(OWLModel owlModel) {
        this.owlModel = owlModel;
    }

    public void accept(QueryVisitor visitor) {
        visitor.visit(this);
    }

    public String toString(int indent) {
        StringBuffer indentStr = QueryUtil.getIndentString(indent);
        StringBuffer buffer = new StringBuffer();
        buffer.append(indentStr);
        buffer.append("OWLNamedClasses()");
        return buffer.toString();
    }

    public void localize(KnowledgeBase kb) {
        this.owlModel = (OWLModel) kb;
    }

}
