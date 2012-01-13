package edu.stanford.smi.protege.query.querytypes.impl;

import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protegex.owl.model.impl.OWLSystemFrames;

public class Util {

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
    public static Frame getOWLExprHead(Frame expr, NarrowFrameStore nfs) {
        OWLSystemFrames systemFrames = (OWLSystemFrames) expr.getKnowledgeBase().getSystemFrames();
        for (Frame listEntry : nfs.getFrames(systemFrames.getRdfFirstProperty(), null, false, expr)) {
            Frame listHead = Util.getListHead(listEntry, nfs);
            for (Frame intersection : nfs.getFrames(systemFrames.getOwlIntersectionOfProperty(), null, false, listHead)) {
                return getOWLExprHead(intersection, nfs);
            }
            for (Frame union : nfs.getFrames(systemFrames.getOwlUnionOfProperty(), null, false, listHead)) {
                return getOWLExprHead(union, nfs);
            }
            break;
        }
        return expr;
    }

    public static Frame getListHead(Frame listEntry, NarrowFrameStore nfs) {
        OWLSystemFrames systemFrames = (OWLSystemFrames) listEntry.getKnowledgeBase().getSystemFrames();
        for (Frame previousEntry : nfs.getFrames(systemFrames.getRdfRestProperty(), null, false, listEntry)) {
            return getListHead(previousEntry, nfs);
        }
        return listEntry;
    }

    public static boolean isOWLNamedClass(Frame frame, NarrowFrameStore nfs) {
        OWLSystemFrames systemFrames = (OWLSystemFrames) frame.getKnowledgeBase().getSystemFrames();
        for (Object name : nfs.getValues(frame, systemFrames.getNameSlot(), null, false)) {
            return !((String) name).startsWith("@");
        }
        return false;
    }

}
