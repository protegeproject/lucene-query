package edu.stanford.smi.protege.query.querytypes.impl;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.ui.QueryUtil;
import edu.stanford.smi.protege.util.LocalizeUtils;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.impl.OWLSystemFrames;

public class OWLRestrictionPropertyPresentQuery implements VisitableQuery {
    private static final long serialVersionUID = -6765862878470667539L;
    
    private transient OWLModel owlModel;
    private OWLProperty property;
    private transient OWLSystemFrames systemFrames;
    
    public OWLRestrictionPropertyPresentQuery(OWLModel owlModel, OWLProperty property) {
        this.owlModel = owlModel;
        this.property = property;
        systemFrames  = owlModel.getSystemFrames();
    }  

    public OWLModel getOwlModel() {
        return owlModel;
    }

    public OWLProperty getProperty() {
        return property;
    }
    
    public RDFProperty getOnProperty() {
        return systemFrames.getOwlOnPropertyProperty();
    }
    
    public RDFProperty getOwlEquivalentClassProperty() {
        return systemFrames.getOwlEquivalentClassProperty();
    }
    
    public Slot getDirectSubclassesSlot() {
        return systemFrames.getDirectSubclassesSlot();
    }

    public void accept(QueryVisitor visitor) {
        visitor.visit(this);
    }

    public String toString(int indent) {
        StringBuffer indentStr = QueryUtil.getIndentString(indent);
        StringBuffer buffer = new StringBuffer();
        buffer.append(indentStr);
        buffer.append("OWLRestrictionPresent(");
        buffer.append(property == null ? "(null property)" : property.getBrowserText());
        buffer.append(")");
        return buffer.toString();
    }

    public void localize(KnowledgeBase kb) {
        owlModel = (OWLModel) kb;
        LocalizeUtils.localize(property, owlModel);
        systemFrames = owlModel.getSystemFrames();
    }

}
