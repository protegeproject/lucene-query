package edu.stanford.smi.protege.query.kb;

import edu.stanford.smi.protege.model.query.Query;

/**
 * This exception gets thrown when a {@link Query} is being created and is invalid.  This will happen
 * if a query hasn't been constructed properly - missing a Slot or expression value.
 *
 * @author Chris Callendar
 * @date 25-Sep-06
 */
public class InvalidQueryException extends Exception {

	public InvalidQueryException(Query query) {
		super("Invalid query: " + (query == null ? "null" : query.toString()));
	}

	public InvalidQueryException(String message) {
		super(message);
	}

	public InvalidQueryException(Throwable cause) {
		super(cause);
	}

	public InvalidQueryException(String message, Throwable cause) {
		super(message, cause);
	}

}
