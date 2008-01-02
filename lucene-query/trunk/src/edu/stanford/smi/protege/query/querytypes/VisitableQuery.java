package edu.stanford.smi.protege.query.querytypes;

import java.io.Serializable;

import edu.stanford.smi.protege.model.query.Query;

public interface VisitableQuery extends Query, Serializable {

    public void accept(QueryVisitor visitor);
}
