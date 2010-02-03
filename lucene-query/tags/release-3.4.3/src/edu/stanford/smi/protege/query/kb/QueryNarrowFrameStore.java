package edu.stanford.smi.protege.query.kb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import edu.stanford.smi.protege.exception.ModificationException;
import edu.stanford.smi.protege.exception.OntologyException;
import edu.stanford.smi.protege.exception.ProtegeError;
import edu.stanford.smi.protege.exception.ProtegeIOException;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Facet;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Reference;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.model.query.QueryCallback;
import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.query.indexer.IndexMechanism;
import edu.stanford.smi.protege.query.indexer.Indexer;
import edu.stanford.smi.protege.query.indexer.PhoneticIndexer;
import edu.stanford.smi.protege.query.indexer.StdIndexer;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.SubsetQuery;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.querytypes.impl.AndQuery;
import edu.stanford.smi.protege.query.querytypes.impl.LuceneOwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.impl.MaxMatchQuery;
import edu.stanford.smi.protege.query.querytypes.impl.NestedOwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OWLRestrictionQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OrQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OwnSlotValueQuery;
import edu.stanford.smi.protege.query.querytypes.impl.PhoneticQuery;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.SimpleStringMatcher;
import edu.stanford.smi.protege.util.transaction.TransactionMonitor;
import edu.stanford.smi.protegex.owl.model.OWLModel;

public class QueryNarrowFrameStore implements NarrowFrameStore {
	private static transient Logger log = Log.getLogger(QueryNarrowFrameStore.class);
	
	public static final String CONFIGURATION_FILE = "configuration.xml";

	private KnowledgeBase kb;

	private NarrowFrameStore delegate;
	private String name;

	private Slot nameSlot;

	private boolean indexingInProgress = false;
	
	private File indexLocation;
	
	private QueryConfiguration configuration;
	
	private Set<Indexer> indexers = new HashSet<Indexer>();

	/*-----------------------------------------------------------
	 * Query Narrow Frame Store support methods.
	 */

	public QueryNarrowFrameStore(String name, 
	                             NarrowFrameStore delegate, 
	                             KnowledgeBase kb,
	                             File indexLocation) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("Constructing QueryNarrowFrameStore");
		}
		this.delegate = delegate;
		nameSlot = kb.getNameSlot();
		this.kb = kb;
		this.indexLocation = indexLocation;
		readConfigFile();
		initialize();
	}
	
	public void readConfigFile() {
	    if (indexLocation == null || !indexLocation.exists() || !indexLocation.isDirectory()) {
	        return;
	    }
	    File configFile = new File(indexLocation, CONFIGURATION_FILE);
	    try {
	        XStream xstream = new XStream(new DomDriver());
	        ObjectInputStream input = xstream.createObjectInputStream(new FileInputStream(configFile));
	        configuration = (QueryConfiguration) input.readObject();
	        configuration.localize(kb);
	        input.close();
	        return;
	    }
	    catch (IOException ioe) {
	        if (log.isLoggable(Level.FINE)) {
	            log.log(Level.FINE, "Configuration not found", ioe);
	        }
	        return;
	    }
	    catch (ClassNotFoundException cnfe) {
	        if (log.isLoggable(Level.FINE)) {
	            log.log(Level.FINE, "Configuration not found", cnfe);
	        }
	        return;
	    }
	}
	
	public void writeConfigFile() {
	    if (indexLocation == null || !indexLocation.exists() || !indexLocation.isDirectory()) {
	        return;
	    }
	    File configFile = new File(indexLocation, CONFIGURATION_FILE);
	    try {
	        XStream xstream = new XStream(new DomDriver());
	        ObjectOutputStream output = xstream.createObjectOutputStream(new FileOutputStream(configFile));
	        output.writeObject(configuration);
	        output.close();
	    }
	    catch (IOException ioe) {
	        log.log(Level.WARNING, "Could not write indexer configuration", ioe);
	    }
	}
	
	public void initialize() {
	    getConfiguration();
	    if (configuration == null) {
	        return;
	    }
	    indexers = new HashSet<Indexer>();
	    for (IndexMechanism mechanism : configuration.getIndexers()) {
	        try {
	            Indexer indexer = mechanism.getIndexerClass().newInstance();
	            indexer.setSearchableSlots(configuration.getSearchableSlots());
	            indexer.setBaseIndexPath(configuration.getBaseIndexPath());
	            indexer.setKnowledgeBase(kb);
	            indexer.setOWLMode(kb instanceof OWLModel);
	            indexer.setNarrowFrameStoreDelegate(delegate);
	            indexers.add(indexer);
	        }
	        catch (IllegalAccessException e) {
	            ;
	        }
	        catch (InstantiationException e) {
	            ;
	        }
	    }
	}
	
	public QueryConfiguration getConfiguration() {
	    return configuration;
	}
	
	public void setConfiguration(QueryConfiguration configuration) {
	    this.configuration = configuration;
	    initialize();
	}
	
	
	public Set<Slot> getSearchableSlots() {
		return getConfiguration().getSearchableSlots();
	}

	public void indexOntologies(QueryConfiguration configuration) {
	    this.configuration = configuration;
	    initialize();
		synchronized (kb) {
			indexingInProgress = true;
		}
		try {
		    for (Indexer indexer : indexers) {
		        indexer.indexOntologies();
		    }
		} finally {
			synchronized (kb) {
				indexingInProgress = false;
			}
			writeConfigFile();
		}
	}

	public void checkWriteable() {
		synchronized (kb) {
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
		synchronized (kb) {
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
					QueryResultsCollector collector = new QueryResultsCollector(kb, delegate, indexers);
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
		    for (Indexer indexer : indexers) {
		        indexer.addValues(frame, slot, values);
		    }
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
            for (Indexer indexer : indexers) {
                indexer.removeValue(frame, slot, value);
            }
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
	          for (Indexer indexer : indexers) {
	              indexer.removeValues(frame, slot);
	              indexer.addValues(frame, slot, values);
	          }
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
        for (Indexer indexer : indexers) {
            indexer.removeValues(fname);
        }
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
		checkWriteable();
		delegate.replaceFrame(original, replacement);
	}

}
