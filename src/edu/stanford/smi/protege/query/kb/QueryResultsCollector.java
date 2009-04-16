package edu.stanford.smi.protege.query.kb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ProtegeIOException;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.MergingNarrowFrameStore;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.query.indexer.Indexer;
import edu.stanford.smi.protege.query.indexer.PhoneticIndexer;
import edu.stanford.smi.protege.query.indexer.StdIndexer;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.SubsetQuery;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.querytypes.impl.AndQuery;
import edu.stanford.smi.protege.query.querytypes.impl.LuceneOwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.impl.MaxMatchQuery;
import edu.stanford.smi.protege.query.querytypes.impl.NegatedQuery;
import edu.stanford.smi.protege.query.querytypes.impl.NestedOwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OWLRestrictionQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OrQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.impl.PhoneticQuery;
import edu.stanford.smi.protege.query.querytypes.impl.PropertyPresentQuery;
import edu.stanford.smi.protege.storage.database.DatabaseFrameDb;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.SimpleStringMatcher;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLOntology;
import edu.stanford.smi.protegex.owl.model.RDFResource;

public class QueryResultsCollector implements QueryVisitor {
	private transient Logger log = Log.getLogger(getClass());
	private Collection<Frame> results = new LinkedHashSet<Frame>();
	
	private KnowledgeBase kb;
	private NarrowFrameStore delegate;
	private Set<Indexer> indexers = new HashSet<Indexer>();

	public QueryResultsCollector(KnowledgeBase kb, NarrowFrameStore delegate, Set<Indexer> indexers) {
		this.kb = kb;
		this.delegate = delegate;
		this.indexers = indexers;
	}
	
	public Collection<Frame> getResults() {
		return results;
	}

	private void setResults(Collection<Frame> results) {
		this.results = results;
	}

	private void retainResults(Collection<Frame> results) {
		this.results.retainAll(results);
	}

	private void addResults(Collection<Frame> results) {
		this.results.addAll(results);
	}

	private void addResult(Frame frame) {
		results.add(frame);
	}

	private PhoneticIndexer getPhoneticIndexer() {
	    for (Indexer indexer : indexers) {
	        if (indexer instanceof PhoneticIndexer) {
	            return (PhoneticIndexer) indexer;
	        }
	    }
	    return null;
	}

	private StdIndexer getStdIndexer() {
	    for (Indexer indexer : indexers) {
	        if (indexer instanceof StdIndexer) {
	            return (StdIndexer) indexer;
	        }
	    }
	    return null;
	}

	private boolean boundSearchResults(int maxMatches) {
		if (maxMatches == KnowledgeBase.UNLIMITED_MATCHES) {
			return false;
		}
		if (results.size() > maxMatches) {
			// filtering this down seems like a waste but the
			// client may actually not be able to handle too many results
			Set<Frame> filteredResults = new HashSet<Frame>();
			int counter = 0;
			for (Frame f : results)  {
				if (++counter <= maxMatches) {
					filteredResults.add(f);
				}
				else {
					break;
				}
			}
			setResults(filteredResults);
			return true;
		}
		else if (results.size() == maxMatches) {
			return true;
		}
		return false;
	}

	private QueryResultsCollector getNewQueryResultsCollector() {
		return new QueryResultsCollector(kb, delegate, indexers);
	}
	
	private Collection<Frame> getAllFramesToSearch(SubsetQuery q) {
		Collection<Frame> frames = q.getFramesToSearch();
		if (frames == null) {
			frames = getAllFramesSafely();
		}
		else {
			frames = new HashSet<Frame>(frames);
		}
		return frames;
	}
	
	private Collection<Frame> getAllFramesSafely() {
		Collection<Frame> frames = new HashSet<Frame>();
		MergingNarrowFrameStore mnfs = MergingNarrowFrameStore.get(kb);
		for (NarrowFrameStore topLevelFrameStore : mnfs.getAvailableFrameStores()) {
			NarrowFrameStore bottomLevelFrameStore = topLevelFrameStore;
			while (bottomLevelFrameStore.getDelegate() != null) {
				bottomLevelFrameStore = bottomLevelFrameStore.getDelegate();
			}
			if (isThreadSafeForGetFrames(bottomLevelFrameStore)) {
				frames.addAll(bottomLevelFrameStore.getFrames());
			}
			else {
				synchronized (kb) {
					frames.addAll(bottomLevelFrameStore.getFrames());
				}
			}
		}
		if (kb instanceof OWLModel) {
			Collection<Frame> toRemove = new HashSet<Frame>();
			for (Frame frame : frames) {
				if (frame instanceof RDFResource && ((RDFResource) frame).isAnonymous()) {
					toRemove.add(frame);
				}
				if (frame instanceof OWLOntology) {
					toRemove.add(frame);
				}
			}
			frames.removeAll(toRemove);
		}
		return frames;
	}
	
	private boolean isThreadSafeForGetFrames(NarrowFrameStore nfs) {
		return nfs instanceof DatabaseFrameDb;
	}
	
	private Collection<VisitableQuery> putAllSubsetQueriesLast(Collection<VisitableQuery> queries) {
		List<VisitableQuery> sortedQueries = new ArrayList<VisitableQuery>();
		for (VisitableQuery q : queries) {
			if (!(q instanceof SubsetQuery)) {
				sortedQueries.add(q);
			}
		}
		for (VisitableQuery q : queries) {
			if (q instanceof SubsetQuery) {
				sortedQueries.add(q);
			}
		}
		return sortedQueries;
	}
	
	

	/* ***************************************************************************************
	 * Visitation Interfaces
	 */
	
	public void visit(AndQuery q) {
		Collection<VisitableQuery> conjuncts = q.getConjuncts();
		if (conjuncts.isEmpty()) {
			results = getAllFramesSafely();
		}
		conjuncts = putAllSubsetQueriesLast(conjuncts);
		QueryResultsCollector innerCollector = getNewQueryResultsCollector();
		Iterator<VisitableQuery> conjunctIterator = conjuncts.iterator();
		VisitableQuery conjunct = conjunctIterator.next();
		conjunct.accept(innerCollector);
		setResults(innerCollector.getResults());
		while (conjunctIterator.hasNext()) {
			conjunct = conjunctIterator.next();
			if (conjunct instanceof SubsetQuery) {
				((SubsetQuery) conjunct).setFramesToSearch(getResults());
			}
			innerCollector = getNewQueryResultsCollector();
			conjunct.accept(innerCollector);
			retainResults(innerCollector.getResults());
		}
	}

	public void visit(OrQuery q) {
		for (VisitableQuery disjunct : q.getDisjuncts()) {
			QueryResultsCollector innerCollector = getNewQueryResultsCollector();
			disjunct.accept(innerCollector);
			addResults(innerCollector.getResults());
			if (boundSearchResults(q.getMaxMatches())) {
				break;
			}
		}
	}

	public void visit(MaxMatchQuery q) {
		QueryResultsCollector innerCollector = getNewQueryResultsCollector();
		q.getInnerQuery().accept(innerCollector);
		setResults(innerCollector.getResults());
		boundSearchResults(q.getMaxMatches());
	}

	public void visit(NestedOwnSlotValueQuery q) {
		QueryResultsCollector innerCollector = getNewQueryResultsCollector();
		q.getInnerQuery().accept(innerCollector);
		for (Frame frame : innerCollector.getResults()) {
			Set<Frame> frames;
			synchronized (kb) {
				frames = delegate.getFrames(q.getSlot(), null, false, frame);
			}
			if (frames != null) {
				addResults(frames);
			}
		}
	}

	public void visit(OWLRestrictionQuery q) {
		VisitableQuery innerQuery = q.getInnerQuery();
		QueryResultsCollector inner = getNewQueryResultsCollector();
		innerQuery.accept(inner);
		Collection<Frame> frames = inner.getResults();
		for (Frame frame : frames) {
			if (frame instanceof Cls) {
				synchronized (kb) {
					addResults(q.executeQueryBasedOnQueryResult((Cls) frame, delegate));
				}
			}
		}
	}

	public void visit(OwnSlotValueQuery q) {
		String searchString = q.getExpr();
		if (searchString.startsWith("*")) {
			synchronized (kb) {
				setResults(delegate.getMatchingFrames(q.getSlot(), null, false, searchString, q.getMaxMatches()));
			}
		}
		else {
			SimpleStringMatcher matcher = new SimpleStringMatcher(searchString);
			Set<Frame> frames;
			synchronized (kb) {
				frames  = delegate.getMatchingFrames(q.getSlot(), null, false,
					"*" + searchString, KnowledgeBase.UNLIMITED_MATCHES);
			}
			for (Frame frame : frames)  {
				boolean found = false;
				synchronized (kb) {
					for (Object o : delegate.getValues(frame, q.getSlot(), null, false)) {
						if (o instanceof String && matcher.isMatch((String) o)) {
							found = true;
							break;
						}
					}
				}
				if (found) {
					addResult(frame);
					if (boundSearchResults(q.getMaxMatches())) {
						break;
					}
				}
			}
		}
	}
	
	public void visit(PropertyPresentQuery q) {
		Slot p = q.getProperty();
		Collection<Frame> frames = delegate.getFramesWithAnyValue(p, null, false);
		if (frames == null) {
			frames = new HashSet<Frame>();
		}
		setResults(frames);
	}

	public void visit(NegatedQuery q) {
		Collection<Frame> framesToSearch = getAllFramesToSearch(q);
		QueryResultsCollector inner = getNewQueryResultsCollector();
		q.getQueryToNegate().accept(inner);
		framesToSearch.removeAll(inner.getResults());
		setResults(framesToSearch);
	}

	public void visit(PhoneticQuery q) {
		try {
		    PhoneticIndexer phoneticIndexer = getPhoneticIndexer();
		    if (phoneticIndexer != null) {
		        setResults(phoneticIndexer.executeQuery(q));
		    }
		    else {
		        log.warning("No phonetic lucene indicies installed");
		    }
		} catch (IOException ioe) {
			log.log(Level.WARNING, "Search failed", ioe);
			throw new ProtegeIOException(ioe);
		}
	}
	
	public void visit(LuceneOwnSlotValueQuery q) {
		try {
		    StdIndexer standardIndexer = getStdIndexer();
		    if (standardIndexer != null) {
		        setResults(standardIndexer.executeQuery(q));
		    }
		    else {
		        log.warning("No standard lucene indicies installed");
		    }
		}
		catch (IOException ioe) {
			Log.getLogger().log(Level.WARNING, "Search failed", ioe);
			throw new ProtegeIOException(ioe);
		}
	}
	

}

