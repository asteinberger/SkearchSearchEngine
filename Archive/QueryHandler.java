package search.indexserver;

import java.io.IOException;

/**
 * Query Handler receives queries and returns docIDs for matching documents in the index.
 * @author Adam Steinberger, Sam Gunther
 */
public interface QueryHandler {
	
	/**
	 * A method to search a query in the index and return the associated docID's
	 * @param limit the maximum number of docID's to be returned
	 * @param query the String to be searched.
	 * @return an array of ints each representing a docID.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public int[] queryDocID(int limit, String query);
	
} // end QueryHandler interface
