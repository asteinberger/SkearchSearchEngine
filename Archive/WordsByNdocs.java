package search.indexserver;

import java.util.Comparator;

/**
 * Compare words by nDocs.
 * @author Adam Steinberger, Sam Gunther
 */
public class WordsByNdocs implements Comparator<Object> {

	/**
	 * Compare two query words by nDocs.
	 */
	public int compare(Object arg0, Object arg1) {
		
		Word w1 = (Word) arg0;
		Word w2 = (Word) arg1;
		
		int result = w1.compareTo(w2);
		
		return result;
		
	} // end compare()

} // end WordsByNdocs class
