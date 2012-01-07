package search.indexserver;

/**
 * Word class for splitting search queries by word and comparing words based on results size.
 * @author Adam Steinberger, Sam Gunther
 */
public class Word implements Comparable<Word> {

	private String word;
	private int nDocs;
	
	/**
	 * Query word for sorting by nDocs.
	 * @param w Word String
	 * @param n nDocs for Word String
	 */
	public Word(String w, int n) {
		this.word = w;
		this.nDocs = n;
	} // end Word constructor
	
	public String getWord() {
		return word;
	} // end getWord()

	public void setWord(String word) {
		this.word = word;
	} // end setWord()

	public int getnDocs() {
		return nDocs;
	} // end getnDocs()

	public void setnDocs(int nDocs) {
		this.nDocs = nDocs;
	} // end setnDocs()

	/**
	 * Compare words by nDocs.
	 */
	public int compareTo(Word w) {
		int nDocs2 = (int) w.getnDocs();
		return (nDocs - nDocs2);
	} // end compareTo()

} // end Word class
