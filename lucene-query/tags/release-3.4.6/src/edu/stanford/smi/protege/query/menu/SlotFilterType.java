/**
 * 
 */
package edu.stanford.smi.protege.query.menu;

import java.util.HashSet;
import java.util.Set;

import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.ValueType;
import edu.stanford.smi.protegex.owl.model.RDFProperty;

public enum SlotFilterType {
    NULL_FILTER {
      @Override
        public Set<Slot> filterSlots(QueryUIConfiguration config, Set<Slot> slots) {
          return slots;
        }  
    },
    DIRECT_OWN_VALUE_PROPERTIES {
        @Override
        public Set<Slot> filterSlots(QueryUIConfiguration config, Set<Slot> slots) {
            if (config.isOwl() && !config.isSearchResultsIncludeIndividuals() && !config.allowMetaModeling()) {
                return filterForAnnotationPropertiesOnly(slots);
            }
            else {
                return slots;
            }
        }
    },
	DIRECT_OWN_VALUE_PROPERTIES_APPLICABLE_TO_CLASSES {
        @Override
	    public Set<Slot> filterSlots(QueryUIConfiguration config, Set<Slot> slots) {
            if (config.isOwl() && !config.allowMetaModeling()) {
                return filterForAnnotationPropertiesOnly(slots);
            }
            else {
                return slots;
            }
	    }
	},
	PROPERTIES_NOT_TAKING_DATA_VALUES {
	    @Override
	    public Set<Slot> filterSlots(QueryUIConfiguration config, Set<Slot> slots) {
	        return removeSlotsReturningDataValues(slots);
	    }
	}
    
    
    ;
	
	public abstract Set<Slot> filterSlots(QueryUIConfiguration config, Set<Slot> slots);
    
    private static Set<Slot> filterForAnnotationPropertiesOnly(Set<Slot> slots) {
        Set<Slot> filtered = new HashSet<Slot>();
        for (Slot slot : slots) {
            if (slot instanceof RDFProperty && ((RDFProperty) slot).isAnnotationProperty()) {
                filtered.add(slot);
            }
        }
        return filtered;
    }
    
    private static Set<Slot> removeSlotsReturningDataValues(Set<Slot>  slots) {
        Set<Slot> filteredSlots = new HashSet<Slot>();
        for (Slot slot : slots) {
            ValueType vt = slot.getValueType();
            if (vt == ValueType.ANY || vt == ValueType.CLS || vt == ValueType.INSTANCE) {
                filteredSlots.add(slot);
            }
        }
        return filteredSlots;
    }
}