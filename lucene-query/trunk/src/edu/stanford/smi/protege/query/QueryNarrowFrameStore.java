package edu.stanford.smi.protege.query;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.exception.ModificationException;
import edu.stanford.smi.protege.exception.OntologyException;
import edu.stanford.smi.protege.exception.ProtegeError;
import edu.stanford.smi.protege.exception.ProtegeIOException;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Facet;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Model;
import edu.stanford.smi.protege.model.Reference;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.model.query.QueryCallback;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.LuceneOwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.MaxMatchQuery;
import edu.stanford.smi.protege.query.querytypes.NestedOwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.OWLRestrictionQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;
import edu.stanford.smi.protege.query.querytypes.OwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.PhoneticQuery;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.util.ApplicationProperties;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.SimpleStringMatcher;
import edu.stanford.smi.protege.util.transaction.TransactionMonitor;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class QueryNarrowFrameStore implements NarrowFrameStore {
	private static transient Logger log = Log.getLogger(QueryNarrowFrameStore.class);

	private Object kbLock;

	private NarrowFrameStore delegate;
	private String name;

	private PhoneticIndexer phoneticIndexer;
	private StdIndexer standardIndexer;

	private Slot nameSlot;

	private boolean indexingInProgress = false;

	/*-----------------------------------------------------------
	 * Query Narrow Frame Store support methods.
	 */

	public QueryNarrowFrameStore(String name, NarrowFrameStore delegate, Set<Slot> searchableSlots, KnowledgeBase kb) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("Constructing QueryNarrowFrameStore");
		}
		this.delegate = delegate;
		nameSlot = (Slot) delegate.getFrame(Model.SlotID.NAME);
		String path = ApplicationProperties.getApplicationDirectory().getAbsolutePath()
		+ File.separator + "lucene" + File.separator  + name;
		phoneticIndexer = new PhoneticIndexer(searchableSlots, delegate, path + File.separator + "phonetic", kb);
		standardIndexer = new StdIndexer(searchableSlots, this, path + File.separator + "phonetic", kb);
		if (kb instanceof OWLModel) {
			phoneticIndexer.setOWLMode(true);
			standardIndexer.setOWLMode(true);
		}
		this.kbLock = kb;
	}

	public void indexOntologies() {
		synchronized (kbLock) {
			indexingInProgress = true;
		}
		try {
			phoneticIndexer.indexOntologies();
			standardIndexer.indexOntologies();
		} finally {
			synchronized (kbLock) {
				indexingInProgress = false;
			}
		}
	}

	public void checkWriteable() {
		synchronized (kbLock) {
			if (indexingInProgress) {
				throw new ModificationException("Server project in read-only mode: Indexing in Progress");
			}
		}
	}

	private String getFrameName(Frame frame) {
		Collection values = delegate.getValues(frame, nameSlot, null, false);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return (String) values.iterator().next();
	}


	/*---------------------------------------------------------------------
	 * executeQuery methods
	 */

	public void executeQuery(final Query query, final QueryCallback qc) throws OntologyException, ProtegeIOException {
		synchronized (kbLock) {
			if (indexingInProgress) {
				throw new ProtegeIOException("Lucene Indicies not ready yet: Indexing in progress");
			}
		}
		if (!(query instanceof VisitableQuery)) {
			getDelegate().executeQuery(query, qc);
		}
		new Thread() {
			@Override
			public void run() {
				try {
					QueryResultsCollector collector = new QueryResultsCollector();
					((VisitableQuery) query).accept(collector);
					qc.provideQueryResults(collector.getResults());
				} catch (OntologyException oe) {
					qc.handleError(oe);
				} catch (ProtegeIOException ioe) {
					qc.handleError(ioe);
				} catch (Throwable  t) {
					qc.handleError(new ProtegeError(t));
				}
			}
		}.start();
	}

	public class QueryResultsCollector implements QueryVisitor {
		private Set<Frame> results = new HashSet<Frame>();

		private void setResults(Set<Frame> results) {
			this.results = results;
		}

		private void retainResults(Set<Frame> results) {
			this.results.retainAll(results);
		}

		private void addResults(Set<Frame> results) {
			this.results.addAll(results);
		}

		private void addResult(Frame frame) {
			results.add(frame);
		}

		public Set<Frame> getResults() {
			return results;
		}

		public void visit(AndQuery q) {
			Collection<VisitableQuery> conjuncts = q.getConjuncts();
			if (conjuncts.isEmpty()) {
				results = delegate.getFrames();
			}
			QueryResultsCollector innerCollector = new  QueryResultsCollector();
			Iterator<VisitableQuery> conjunctIterator = conjuncts.iterator();
			VisitableQuery conjunct = conjunctIterator.next();
			conjunct.accept(innerCollector);
			setResults(innerCollector.getResults());
			while (conjunctIterator.hasNext()) {
				conjunct = conjunctIterator.next();
				innerCollector = new QueryResultsCollector();
				conjunct.accept(innerCollector);
				retainResults(innerCollector.getResults());
			}
		}

		public void visit(OrQuery q) {
			for (VisitableQuery disjunct : q.getDisjuncts()) {
				QueryResultsCollector innerCollector = new QueryResultsCollector();
				disjunct.accept(innerCollector);
				addResults(innerCollector.getResults());
				if (boundSearchResults(q.getMaxMatches())) {
					break;
				}
			}
		}

		public void visit(MaxMatchQuery q) {
			QueryResultsCollector innerCollector = new QueryResultsCollector();
			q.getInnerQuery().accept(innerCollector);
			setResults(innerCollector.getResults());
			boundSearchResults(q.getMaxMatches());
		}

		public void visit(NestedOwnSlotValueQuery q) {
			QueryResultsCollector innerCollector = new QueryResultsCollector();
			q.getInnerQuery().accept(innerCollector);
			for (Frame frame : innerCollector.getResults()) {
				Set<Frame> frames = delegate.getFrames(q.getSlot(), null, false, frame);
				if (frames != null) {
					addResults(frames);
				}
			}
		}

		public void visit(OWLRestrictionQuery q) {
			VisitableQuery innerQuery = q.getInnerQuery();
			QueryResultsCollector inner = new QueryResultsCollector();
			innerQuery.accept(inner);
			Set<Frame> frames = inner.getResults();
			for (Frame frame : frames) {
				if (frame instanceof Cls) {
					addResults(q.executeQueryBasedOnQueryResult((Cls) frame, getDelegate()));
				}
			}
		}

		public void visit(OwnSlotValueQuery q) {
			String searchString = q.getExpr();
			if (searchString.startsWith("*")) {
				setResults(delegate.getMatchingFrames(q.getSlot(), null, false, searchString, q.getMaxMatches()));
			}
			else {
				SimpleStringMatcher matcher = new SimpleStringMatcher(searchString);
				Set<Frame> frames = delegate.getMatchingFrames(q.getSlot(), null, false,
						"*" + searchString, KnowledgeBase.UNLIMITED_MATCHES);
				for (Frame frame : frames)  {
					boolean found = false;
					for (Object o : delegate.getValues(frame, q.getSlot(), null, false)) {
						if (o instanceof String && matcher.isMatch((String) o)) {
							found = true;
							break;
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

		public void visit(PhoneticQuery q) {
			try {
				setResults(phoneticIndexer.executeQuery(q));
			} catch (IOException ioe) {
				Log.getLogger().log(Level.WARNING, "Search failed", ioe);
				throw new ProtegeIOException(ioe);
			}
		}
		
		public void visit(LuceneOwnSlotValueQuery q) {
			try {
				setResults(standardIndexer.executeQuery(q));
			}
			catch (IOException ioe) {
				Log.getLogger().log(Level.WARNING, "Search failed", ioe);
				throw new ProtegeIOException(ioe);
			}
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

	}

	/*---------------------------------------------------------------------
	 *  Common Narrow Frame Store Functions (excepting executeQuery)
	 */


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public NarrowFrameStore getDelegate() {
		return delegate;
	}

	public int getFrameCount() {
		return delegate.getFrameCount();
	}

	public int getClsCount() {
		return delegate.getClsCount();
	}

	public int getSlotCount() {
		return delegate.getSlotCount();
	}

	public int getFacetCount() {
		return delegate.getFacetCount();
	}

	public int getSimpleInstanceCount() {
		return delegate.getSimpleInstanceCount();
	}

	public Set<Frame> getFrames() {
		return delegate.getFrames();
	}

	public Frame getFrame(FrameID id) {
		return delegate.getFrame(id);
	}

	public List getValues(Frame frame, Slot slot, Facet facet, boolean isTemplate) {
		return delegate.getValues(frame, slot, facet, isTemplate);
	}

	public int getValuesCount(Frame frame, Slot slot, Facet facet,
			boolean isTemplate) {
		return delegate.getValuesCount(frame, slot, facet, isTemplate);
	}

	public void addValues(Frame frame, Slot slot, Facet facet,
			boolean isTemplate, Collection values) throws ProtegeIOException {
		checkWriteable();
		if (log.isLoggable(Level.FINE)) {
			log.fine("addValues");
		}
		delegate.addValues(frame, slot, facet, isTemplate, values);
		if (facet == null && !isTemplate) {
			phoneticIndexer.addValues(frame, slot, values);
		}
	}

	public void moveValue(Frame frame, Slot slot, Facet facet,
			boolean isTemplate, int from, int to) throws ProtegeIOException {
		checkWriteable();
		delegate.moveValue(frame, slot, facet, isTemplate, from, to);
	}

	public void removeValue(Frame frame, Slot slot, Facet facet,
			boolean isTemplate, Object value) throws ProtegeIOException {
		checkWriteable();
		if (log.isLoggable(Level.FINE)) {
			log.fine("Remove  Value");
		}
		delegate.removeValue(frame, slot, facet, isTemplate, value);
		if (facet == null && !isTemplate) {
			phoneticIndexer.removeValue(frame, slot, value);
		}
	}

	public void setValues(Frame frame, Slot slot, Facet facet,
			boolean isTemplate, Collection values) throws ProtegeIOException {
		checkWriteable();
		if (log.isLoggable(Level.FINE)) {
			log.fine("setValues");
		}
		delegate.setValues(frame, slot, facet, isTemplate, values);
		if (facet == null && !isTemplate) {
			phoneticIndexer.removeValues(frame, slot);
			phoneticIndexer.addValues(frame, slot, values);
		}
	}

	public Set<Frame> getFrames(Slot slot, Facet facet, boolean isTemplate,
			Object value) {
		return delegate.getFrames(slot, facet, isTemplate, value);
	}

	public Set<Frame> getFramesWithAnyValue(Slot slot, Facet facet,
			boolean isTemplate) {
		return delegate.getFramesWithAnyValue(slot, facet, isTemplate);
	}

	public Set<Frame> getMatchingFrames(Slot slot, Facet facet,
			boolean isTemplate, String value,
			int maxMatches) {
		return delegate.getMatchingFrames(slot, facet, isTemplate, value, maxMatches);
	}

	public Set<Reference> getReferences(Object value) {
		return delegate.getReferences(value);
	}

	public Set<Reference> getMatchingReferences(String value, int maxMatches) {
		return delegate.getMatchingReferences(value, maxMatches);
	}

	public void deleteFrame(Frame frame) throws ProtegeIOException {
		checkWriteable();
		if (log.isLoggable(Level.FINE)) {
			log.fine("deleteFrame ");
		}
		String fname = getFrameName(frame);
		delegate.deleteFrame(frame);
		phoneticIndexer.removeValues(fname);
	}

	public void close() {
		delegate.close();
	}

	public Set getClosure(Frame frame, Slot slot, Facet facet, boolean isTemplate) {
		return delegate.getClosure(frame, slot, facet, isTemplate);
	}

	public void replaceFrame(Frame frame) throws ProtegeIOException {
		checkWriteable();
		delegate.replaceFrame(frame);
	}

	public boolean beginTransaction(String name) {
		return delegate.beginTransaction(name);
	}

	public boolean commitTransaction() {
		return delegate.commitTransaction();
	}

	public boolean rollbackTransaction() {
		return delegate.rollbackTransaction();
	}

	public TransactionMonitor getTransactionStatusMonitor() {
		return delegate.getTransactionStatusMonitor();
	}

	public void reinitialize() {
		delegate.reinitialize();
	}

	public void replaceFrame(Frame original, Frame replacement) {
		//TODO: Tim please check this method
		checkWriteable();
		delegate.replaceFrame(original, replacement);
	}

}
