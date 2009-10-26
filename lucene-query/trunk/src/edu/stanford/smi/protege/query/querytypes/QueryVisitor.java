package edu.stanford.smi.protege.query.querytypes;

import edu.stanford.smi.protege.query.querytypes.impl.AndQuery;
import edu.stanford.smi.protege.query.querytypes.impl.LuceneOwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.impl.MaxMatchQuery;
import edu.stanford.smi.protege.query.querytypes.impl.NegatedQuery;
import edu.stanford.smi.protege.query.querytypes.impl.NestedOwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OWLNamedClassesQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OWLRestrictionPropertyPresentQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OWLRestrictionQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OrQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.impl.PhoneticQuery;
import edu.stanford.smi.protege.query.querytypes.impl.PropertyPresentQuery;

public interface QueryVisitor {
    
    void visit(AndQuery q);
    
    void visit(OrQuery q);
    
    void visit(MaxMatchQuery q);
    
    void visit(NestedOwnSlotValueQuery q);
    
    void visit(OWLNamedClassesQuery q);
    
    void visit(OWLRestrictionQuery q);
    
    void visit(OWLRestrictionPropertyPresentQuery q);
    
    void visit(OwnSlotValueQuery q);
    
    void visit(PropertyPresentQuery q);
    
    void visit(PhoneticQuery q);
    
    void visit(LuceneOwnSlotValueQuery q);
    
    void visit(NegatedQuery q);
}
