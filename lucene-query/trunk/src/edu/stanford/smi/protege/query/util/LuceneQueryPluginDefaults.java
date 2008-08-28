package edu.stanford.smi.protege.query.util;

import java.util.HashMap;

import edu.stanford.smi.protege.model.ValueType;
import edu.stanford.smi.protege.query.ui.QueryComponent;
import edu.stanford.smi.protege.util.ApplicationProperties;

/**
 * Class for storing the default configuration of the Advance Query Plugin.
 * The default configuration is read from protege.properties. This class stores 
 * both the keys from the AQP configuration and their default values and provides utility 
 * methods for getting the default configuration.
 * 
 * @author Tania Tudorache <tudorache@stanford.edu>
 *
 */
public class LuceneQueryPluginDefaults {
			
	public static final String PROTEGE_PROP_KEY_DEFAULT_SLOT = "query_plugin.default.search_slot";
	public static final String DEFAULT_SLOT_NAME = "Preferred_Name";
	
	private static final String PROTEGE_PROP_KEY_DEFAULT_SEARCH_TYPE_BASE = "query_plugin.default.search_type.";
	
	private static HashMap<ValueType, String> valueType2ProtegeKeysMap = new HashMap<ValueType, String>();
	private static HashMap<ValueType, String> valueType2DefaultValueMap = new HashMap<ValueType, String>();
	
	static {
		valueType2ProtegeKeysMap.put(ValueType.ANY, PROTEGE_PROP_KEY_DEFAULT_SEARCH_TYPE_BASE + ValueType.ANY.toString());
		valueType2ProtegeKeysMap.put(ValueType.BOOLEAN, PROTEGE_PROP_KEY_DEFAULT_SEARCH_TYPE_BASE + ValueType.BOOLEAN.toString());
		valueType2ProtegeKeysMap.put(ValueType.CLS, PROTEGE_PROP_KEY_DEFAULT_SEARCH_TYPE_BASE + ValueType.CLS.toString());
		valueType2ProtegeKeysMap.put(ValueType.FLOAT, PROTEGE_PROP_KEY_DEFAULT_SEARCH_TYPE_BASE + ValueType.FLOAT.toString());
		valueType2ProtegeKeysMap.put(ValueType.INSTANCE, PROTEGE_PROP_KEY_DEFAULT_SEARCH_TYPE_BASE + ValueType.INSTANCE.toString());
		valueType2ProtegeKeysMap.put(ValueType.INTEGER, PROTEGE_PROP_KEY_DEFAULT_SEARCH_TYPE_BASE + ValueType.INTEGER.toString());
		valueType2ProtegeKeysMap.put(ValueType.STRING, PROTEGE_PROP_KEY_DEFAULT_SEARCH_TYPE_BASE + ValueType.STRING.toString());
		valueType2ProtegeKeysMap.put(ValueType.SYMBOL, PROTEGE_PROP_KEY_DEFAULT_SEARCH_TYPE_BASE + ValueType.SYMBOL.toString());
		
		valueType2DefaultValueMap.put(ValueType.ANY, QueryComponent.EXACT_MATCH);
		valueType2DefaultValueMap.put(ValueType.BOOLEAN, QueryComponent.IS);
		valueType2DefaultValueMap.put(ValueType.CLS, QueryComponent.CONTAINS);
		valueType2DefaultValueMap.put(ValueType.FLOAT, QueryComponent.IS);
		valueType2DefaultValueMap.put(ValueType.INSTANCE, QueryComponent.CONTAINS);
		valueType2DefaultValueMap.put(ValueType.INTEGER, QueryComponent.IS);
		valueType2DefaultValueMap.put(ValueType.STRING, QueryComponent.EXACT_MATCH);
		valueType2DefaultValueMap.put(ValueType.SYMBOL, QueryComponent.IS);
	}
	
		
	/**
	 * @return The default search slot name as defined in the protege.properties for the
	 * key defined as value of {@link PROTEGE_PROP_KEY_DEFAULT_SLOT}. 
	 * If this property is not present, it will return the value of the constant
	 * {@link DEFAULT_SLOT_NAME}.
	 * 
	 */
	public static String getDefaultSearchSlotName() {
		return ApplicationProperties.getApplicationOrSystemProperty(PROTEGE_PROP_KEY_DEFAULT_SLOT, DEFAULT_SLOT_NAME);
	}
	
	
	/**
	 * Gets the default search type for a certain {@link ValueType} 
	 * @param valueType The value type as a {@link ValueType} object
	 * @return The default search type, which are defined in {@link QueryComponent}
	 * It returns null, if the a default search type for this value type was not found. 
	 * 
	 */
	public static String getDefaultSearchType(ValueType valueType) {
		String propKey = valueType2ProtegeKeysMap.get(valueType);
		
		if (propKey == null) {
			return null;
		}
		
		return ApplicationProperties.getApplicationOrSystemProperty(propKey, valueType2DefaultValueMap.get(valueType));		
	}

}
