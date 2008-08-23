package edu.stanford.smi.protege.query.querytypes;

public interface QueryVisitor {
    
    void visit(AndQuery q);
    
    void visit(OrQuery q);
    
    void visit(MaxMatchQuery q);
    
    void visit(NestedOwnSlotValueQuery q);
    
    void visit(OWLRestrictionQuery q);
    
    void visit(OwnSlotValueQuery q);
    
    void visit(PhoneticQuery q);
    
    void visit(LuceneOwnSlotValueQuery q);
}
