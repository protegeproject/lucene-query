package edu.stanford.smi.protege.query.querytypes;

import java.util.Collection;

import edu.stanford.smi.protege.model.Frame;

public interface SubsetQuery extends VisitableQuery {

	void setFramesToSearch(Collection<Frame> allFrames);
	
	Collection<Frame> getFramesToSearch();
}
