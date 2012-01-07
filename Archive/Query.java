package search.indexserver;

/**
 * Query object for passing search queries along with results limit, docIds results, and latch.
 * @author Adam Steinberger, Sam Gunther
 */
public class Query {

	private String query;
	private int limit;
	private int[] docIds;
	private QLatch latch;

	/**
	 * Search query to find docId results for.
	 * @param q Query
	 * @param l Latch
	 */
	public Query(String q, int l) {
		this.query = q;
		this.limit = l;
		this.latch = new QLatch(1);
	} // end Query constructor

	public QLatch getLatch() {
		return latch;
	} // end getLatch()

	public void setLatch(QLatch latch) {
		this.latch = latch;
	} // end setLatch()

	public int[] getDocIds() {
		return docIds;
	} // end getDocsIds()

	public void setDocIds(int[] docIds) {
		this.docIds = docIds;
	} // end setDocIds()

	public String getQuery() {
		return query;
	} // end getQuery()

	public void setQuery(String query) {
		this.query = query;
	} // end setQuery()

	public int getLimit() {
		return limit;
	} // end getLimit()

	public void setLimit(int limit) {
		this.limit = limit;
	} // end setLimit()
	
} // end Query class
