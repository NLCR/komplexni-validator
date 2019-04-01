package nkp.pspValidator.shared.engine;

/**
 * Created by Martin Řehánek on 21.10.16.
 */
public enum ValueType {

    STRING, INTEGER, FILE, IDENTIFIER, // simple types
    STRING_LIST, FILE_LIST, IDENTIFIER_LIST, //lists
    STRING_LIST_LIST, FILE_LIST_LIST, IDENTIFIER_LIST_LIST,  //lists of lists
    BOOLEAN, LEVEL, RESOURCE_TYPE, EXTERNAL_UTIL, METADATA_FORMAT, ENTITY_TYPE; //enums

}
