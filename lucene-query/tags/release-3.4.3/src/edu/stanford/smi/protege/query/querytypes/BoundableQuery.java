package edu.stanford.smi.protege.query.querytypes;

public interface BoundableQuery extends VisitableQuery {
	
	void setMaxMatches(int maxMatches);
	
	BoundableQuery shallowClone();

}
