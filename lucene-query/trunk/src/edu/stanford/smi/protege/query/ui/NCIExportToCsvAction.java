package edu.stanford.smi.protege.query.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.stanford.smi.protege.action.ExportToCsvAction;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.util.ModalDialog;
import edu.stanford.smi.protege.util.StringUtilities;
import edu.stanford.smi.protegex.owl.model.OWLAllValuesFrom;
import edu.stanford.smi.protegex.owl.model.OWLClass;
import edu.stanford.smi.protegex.owl.model.OWLIntersectionClass;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNAryLogicalClass;
import edu.stanford.smi.protegex.owl.model.OWLObjectProperty;
import edu.stanford.smi.protegex.owl.model.OWLQuantifierRestriction;
import edu.stanford.smi.protegex.owl.model.OWLSomeValuesFrom;
import edu.stanford.smi.protegex.owl.model.OWLUnionClass;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSClass;
import edu.stanford.smi.protegex.owl.model.impl.DefaultRDFSLiteral;
import edu.stanford.smi.protegex.owl.model.visitor.OWLModelVisitorAdapter;


public class NCIExportToCsvAction extends ExportToCsvAction {
	
	private static final long serialVersionUID = -6034042595540029150L;
	
	private Component parent;	
	private boolean showExportClasses = false;

	public NCIExportToCsvAction(KnowledgeBase kb, Component parent, boolean showExportClasses) {
		super(kb);
		this.parent = parent;		
		this.showExportClasses = showExportClasses;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Collection instancesToExport = getClsesToExport();
		if (!showExportClasses && (instancesToExport == null || instancesToExport.size() == 0)) {
			ModalDialog.showMessageDialog(
					parent,
					"There are no results to export.\nPlease try a different search.",
					"Nothing to export");
			return;
		}
		
		getExportConfigurationPanel().setShowExportSuperclasses(true);	
		getExportConfigurationPanel().setShowExportedClasses(showExportClasses);		
		setExportMetadata(getStringToExport());		
		setInstancesToExport((Collection<Instance>)instancesToExport);
		setSlotsToExport(getPossibleExportSlots());
		
		super.actionPerformed(event);
	}

	private Collection<Slot> getPossibleExportSlots() {
		ArrayList<Slot> slots = new ArrayList<Slot>();
		if (getKnowledgeBase() instanceof OWLModel) {
			OWLModel owlModel = (OWLModel) getKnowledgeBase();
			slots.add(owlModel.getRDFSLabelProperty());
			slots.add(owlModel.getRDFSCommentProperty());
		} else {
			slots.add(getKnowledgeBase().getNameSlot());
		}
		return slots;
	}
	
	@Override
	protected Collection<Cls> getSuperclassesToExport(Instance inst) {				
		Collection<Cls> superclses = new ArrayList<Cls>(super.getSuperclassesToExport(inst));
		for (Iterator<Cls> iterator = superclses.iterator(); iterator.hasNext();) {
			Cls cls = iterator.next();
			if (cls instanceof RDFResource && ((RDFResource)cls).isAnonymous()) {
				iterator.remove();
			}
		}
		return superclses;
	}
	
	@Override
	protected Component getParentComponent() {			
		return parent;
	}
	
	/**
	 * @return Should return the string to be exported as the last line
	 * in the file, e.g. the query string.
	 * Should be overwritten in subclasses.
	 */
	protected String getStringToExport() {
		return "";
	}
	
	/**
	 * @return Should return the clses to export. 
	 * Should be overwritten in subclasses.
	 */
	protected Collection<Cls> getClsesToExport() {
		return new ArrayList<Cls>();
	}
	
	@Override
	protected String getExportName(Frame frame) {
	    if (!isExportBrowserTextEnabled()) { return getQuotedValule(super.getExportName(frame)); }	    
	    return getQuotedValule(StringUtilities.unquote(frame.getBrowserText()));
	}
	
	@Override
	protected String getExportDataValueName(Object data) {
	    if (getKnowledgeBase() instanceof OWLModel &&
	            data instanceof String && 
	            (((String) data).startsWith(DefaultRDFSLiteral.DATATYPE_PREFIX) || ((String) data).startsWith(DefaultRDFSLiteral.LANGUAGE_PREFIX))) {
	        DefaultRDFSLiteral literal = new DefaultRDFSLiteral((OWLModel) getKnowledgeBase(), (String) data);
	        return literal.getString();
	    }
	    return super.getExportDataValueName(data);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected String getSlotValuesExportString(Instance instance, Slot slot) {
		if ((slot instanceof OWLObjectProperty) 
				&& (instance instanceof OWLClass) 
				&& !((RDFProperty) slot).isAnnotationProperty()) {
			OWLClass cls = (OWLClass) instance;
			OWLObjectProperty p = (OWLObjectProperty) slot;
			
			OWLFillerCollector collector = new OWLFillerCollector(p);
			Set<OWLClass> superClasses = new HashSet<OWLClass>();
			superClasses.addAll(cls.getSuperclasses(false));
			superClasses.addAll(cls.getEquivalentClasses());
			for (OWLClass superClass : superClasses) {
				superClass.accept(collector);
			}
			StringBuffer buffer = new StringBuffer();
			Iterator<RDFResource> fillerIt = collector.getFillers().iterator();
			while (fillerIt.hasNext()) {
				RDFResource filler = fillerIt.next();
				buffer.append(getExportName(filler));
				if (fillerIt.hasNext()) {
					buffer.append(getSlotValuesDelimiter());
				}
			}
			return buffer.toString();
		}
		else {
			return super.getSlotValuesExportString(instance, slot);
		}
	}
	
	private static class OWLFillerCollector extends OWLModelVisitorAdapter {
		private Collection<RDFResource> fillers = new HashSet<RDFResource>();
		private OWLObjectProperty p;
		
		public OWLFillerCollector(OWLObjectProperty p) {
			this.p = p;
		}
		
		public Collection<RDFResource> getFillers() {
			return fillers;
		}
		
		@Override
		public void visitOWLIntersectionClass(OWLIntersectionClass owlIntersectionClass) {
			visitNAryLogicalClass(owlIntersectionClass);
		}
		
		@Override
		public void visitOWLUnionClass(OWLUnionClass owlUnionClass) {
			visitNAryLogicalClass(owlUnionClass);
		}
		
		
		private void visitNAryLogicalClass(OWLNAryLogicalClass logical) {
			for (RDFSClass junct : logical.getOperands()) {
				junct.accept(this);
			}
		}
		
		@Override
		public void visitOWLAllValuesFrom(OWLAllValuesFrom owlAllValuesFrom) {
			visitOWLQuantifierRestriction(owlAllValuesFrom);
		}
		
		@Override
		public void visitOWLSomeValuesFrom(OWLSomeValuesFrom someValuesFrom) {
			visitOWLQuantifierRestriction(someValuesFrom);
		}
		
		private void visitOWLQuantifierRestriction(OWLQuantifierRestriction restriction) {
			if (restriction.getOnProperty().equals(p)) {
				fillers.add(restriction.getFiller());
			}
			
		}
		
	}
	

}
