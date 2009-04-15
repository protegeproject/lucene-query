package edu.stanford.smi.protege.query.querytypes.impl;

import java.util.HashSet;
import java.util.Set;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.ui.QueryUtil;
import edu.stanford.smi.protege.util.LocalizeUtils;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNames;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.impl.OWLSystemFrames;

public class OWLRestrictionQuery implements VisitableQuery {
	private static boolean owlInitialized = false;

	private static RDFProperty equivalentClass;
	private static RDFProperty someValuesFrom;
	private static RDFProperty allValuesFrom;
	private static RDFProperty onProperty;
	private static RDFProperty owlIntersectionOf;
	private static RDFProperty rdfFirst;
	private static RDFProperty rdfRest;

	private static Slot directSubclasses;
	private static Slot nameSlot;

	private OWLProperty property;
	private VisitableQuery query;
	
	private OWLModel owlModel;

	public OWLRestrictionQuery(OWLModel om,
			OWLProperty property,
			VisitableQuery query) {
		initializeOWLEntities(om);
		this.property = property;
		this.query = query;
		owlModel = om;
	}

	private void initializeOWLEntities(OWLModel om) {
		if (!owlInitialized) {
		    OWLSystemFrames frames = om.getSystemFrames();
			equivalentClass   = frames.getOwlEquivalentClassProperty();
			
			onProperty        = frames.getOwlOnPropertyProperty();
			owlIntersectionOf = frames.getOwlIntersectionOfProperty();
			rdfFirst          = frames.getRdfFirstProperty();
			rdfRest           = frames.getRdfRestProperty();
			someValuesFrom    = frames.getOwlSomeValuesFromProperty();
			allValuesFrom     = frames.getOwlAllValuesFromProperty();

			directSubclasses  = frames.getDirectSubclassesSlot();
			nameSlot          = frames.getNameSlot();

			owlInitialized = true;
		}

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
		Set<Frame> results = new HashSet<Frame>();
		for (Frame restriction : getRestrictions(innerQueryResult, nfs)) {
			boolean done = false;
			Frame owlHeadExpr = getOWLExprHead(restriction, nfs);
			for (Frame equivCls : nfs.getFrames(equivalentClass, null, false, owlHeadExpr)) {
				results.addAll(getOWLNamedSubClasses((Cls) equivCls, nfs));
				done = true;
			}
			if (!done && restriction instanceof Cls) {
				results.addAll(getOWLNamedSubClasses((Cls) restriction, nfs));
			}
		}

		return results;
	}

	private Set<Cls> getOWLNamedSubClasses(Cls cls, NarrowFrameStore nfs) {
		Set<Cls> subClasses = new HashSet<Cls>();
		for (Object subCls : nfs.getClosure(cls, directSubclasses, null, false)) {
			if (subCls instanceof Cls && isOWLNamed((Cls) subCls, nfs)) {
				subClasses.add((Cls) subCls);
			}
		}
		return subClasses;
	}

	private boolean isOWLNamed(Frame frame, NarrowFrameStore nfs) {
		for (Object name : nfs.getValues(frame, nameSlot, null, false)) {
			return !((String) name).startsWith("@");
		}
		return false;
	}



	private Set<Frame> getRestrictions(Cls innerQueryResult, NarrowFrameStore nfs) {
		Set<Frame> restrictions = new HashSet<Frame>();
		for (Frame restriction : nfs.getFrames(someValuesFrom, null, false, innerQueryResult)) {
			for (Object restrictionProperty : nfs.getValues(restriction, onProperty, null, false)) {
				if (restrictionProperty instanceof Frame && restrictionProperty.equals(property)) {
					restrictions.add(restriction);
				}
			}
		}
		for (Frame restriction : nfs.getFrames(allValuesFrom, null, false, innerQueryResult)) {
		    for (Object restrictionProperty : nfs.getValues(restriction, onProperty, null, false)) {
		        if (restrictionProperty instanceof Frame && restrictionProperty.equals(property)) {
		            restrictions.add(restriction);
		        }
		    }
		}
		return restrictions;
	}

	/*
	 * This function is looking for the head of the owl expression.  But it will only handle a couple of cases -
	 * the expr is itself the head or the head is part of an intersection.
	 * The algorithm is as follows:
	 *   if the head expr is of the form
	 *      Intersection( expr1, expr2, ... expr ... exprn)
	 *   then find the Intersection class by working backwards through the rdf list and through the
	 *   owl:intersectionOf.
	 *
	 *   If any part of the above search fails then simply return the expr itself.
	 */
	private Frame getOWLExprHead(Frame expr, NarrowFrameStore nfs) {
		for (Frame listEntry : nfs.getFrames(rdfFirst, null, false, expr)) {
			Frame listHead = getListHead(listEntry, nfs);
			for (Frame intersection : nfs.getFrames(owlIntersectionOf, null, false, listHead)) {
				return intersection;
			}
			break;
		}
		return expr;
	}

	private Frame getListHead(Frame listEntry, NarrowFrameStore nfs) {
		for (Frame previousEntry : nfs.getFrames(rdfRest, null, false, listEntry)) {
			return getListHead(previousEntry, nfs);
		}
		return listEntry;
	}

	public void localize(KnowledgeBase kb) {
		initializeOWLEntities((OWLModel) kb);
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
