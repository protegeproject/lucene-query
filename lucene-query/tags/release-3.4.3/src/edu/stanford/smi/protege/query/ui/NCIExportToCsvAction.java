package edu.stanford.smi.protege.query.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protege.action.ExportToCsvAction;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.util.ModalDialog;
import edu.stanford.smi.protege.util.StringUtilities;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;


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
	    if (!isExportBrowserTextEnabled()) { return super.getExportName(frame); }	    
	    return StringUtilities.unquote(frame.getBrowserText());
	}
	

}
