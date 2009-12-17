package edu.stanford.smi.protege.query.querytypes.impl;

import java.util.HashSet;
import java.util.Set;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.ui.QueryUtil;
import edu.stanford.smi.protege.util.LocalizeUtils;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.impl.OWLSystemFrames;

public class OWLRestrictionQuery implements VisitableQuery {
    private OWLProperty property;
    private VisitableQuery query;
        
    private transient OWLModel owlModel;

    public OWLRestrictionQuery(OWLModel om,
                               OWLProperty property,
                               VisitableQuery query) {
        this.property = property;
        this.query = query;
        owlModel = om;
    }

    public void accept(QueryVisitor visitor) {
        visitor.visit(this);
    }

    public OWLProperty getProperty() {
        return property;
    }

    public VisitableQuery getInnerQuery() {
        return query;
    }

    public Set<Frame> executeQueryBasedOnQueryResult(Cls innerQueryResult, NarrowFrameStore nfs) {
        OWLSystemFrames systemFrames = owlModel.getSystemFrames();
        Set<Frame> results = new HashSet<Frame>();
        for (Frame restriction : getRestrictions(innerQueryResult, nfs)) {
            Frame owlHeadExpr = Util.getOWLExprHead(restriction, nfs);
            for (Frame equivCls : nfs.getFrames(systemFrames.getOwlEquivalentClassProperty(), null, false, owlHeadExpr)) {
                if (equivCls instanceof Cls && Util.isOWLNamedClass((Cls) equivCls, nfs)) {
                    results.add(equivCls);
                }
            }
            for (Object equivCls : nfs.getValues(owlHeadExpr, systemFrames.getDirectSubclassesSlot(), null, false)) {
                if (equivCls instanceof Cls && Util.isOWLNamedClass((Cls) equivCls, nfs)) {
                    results.add((Frame) equivCls);
                }
            }
        }

        return results;
    }

    private Set<Cls> getOWLNamedSubClasses(Cls cls, NarrowFrameStore nfs) {
        OWLSystemFrames systemFrames = owlModel.getSystemFrames();
        Set<Cls> subClasses = new HashSet<Cls>();
        for (Object subCls : nfs.getClosure(cls, systemFrames.getDirectSubclassesSlot(), null, false)) {
            if (subCls instanceof Cls && Util.isOWLNamedClass((Cls) subCls, nfs)) {
                subClasses.add((Cls) subCls);
            }
        }
        return subClasses;
    }

    private Set<Frame> getRestrictions(Cls innerQueryResult, NarrowFrameStore nfs) {
        OWLSystemFrames systemFrames = owlModel.getSystemFrames();
        Set<Frame> restrictions = new HashSet<Frame>();
        for (Frame restriction : nfs.getFrames(systemFrames.getOwlSomeValuesFromProperty(), null, false, innerQueryResult)) {
            for (Object restrictionProperty : nfs.getValues(restriction, systemFrames.getOwlOnPropertyProperty(), null, false)) {
                if (restrictionProperty instanceof Frame && restrictionProperty.equals(property)) {
                    restrictions.add(restriction);
                }
            }
        }
        for (Frame restriction : nfs.getFrames(systemFrames.getOwlAllValuesFromProperty(), null, false, innerQueryResult)) {
            for (Object restrictionProperty : nfs.getValues(restriction, systemFrames.getOwlOnPropertyProperty(), null, false)) {
                if (restrictionProperty instanceof Frame && restrictionProperty.equals(property)) {
                    restrictions.add(restriction);
                }
            }
        }
        return restrictions;
    }

    public void localize(KnowledgeBase kb) {
        owlModel = (OWLModel) kb;
        LocalizeUtils.localize(property, kb);
        query.localize(kb);
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
        buffer.append(property == null ? "(null property)" : property.getBrowserText());
        buffer.append("\n");
        buffer.append(query.toString(indent + 4));
        buffer.append("\n");
        buffer.append(indentStr);
        buffer.append(")");
        return buffer.toString();
    }

}
