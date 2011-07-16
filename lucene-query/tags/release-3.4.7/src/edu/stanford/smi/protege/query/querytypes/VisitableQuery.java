package edu.stanford.smi.protege.query.querytypes;

import java.io.Serializable;

import edu.stanford.smi.protege.model.query.Query;

public interface VisitableQuery extends Query, Serializable {

    public void accept(QueryVisitor visitor);

    /**
     * A toString() method with indentation.
     * This method can be used for pretty printing the queries.
     * @param indent - the number of chars to indent the query string
     */
    public String toString(int indent);
}
