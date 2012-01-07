package search.indexserver;

import java.util.ArrayDeque;
import java.util.Arrays;

import search.common.LogLibrary;
import search.common.Timer;
import search.common.Timer.Method;

/**
 * Index Server receives queries and returns docIDs for matching documents in the index.
 * @author Adam Steinberger, Sam Gunther
 */
public class IndexServer implements QueryHandler {

	private static LogLibrary logs;
	private QLatch serverLatch;
	private ArrayDeque<Query> queries;
	private QueryThread qThread;
	private boolean verbose;
	private Timer timer;
	private boolean kill;
	
	/**
	 * Skearch IndexServer constructor.
	 */
	public IndexServer(int docId, boolean v) {
		
		try {
			
			logs = LogLibrary.instance("IndexServer.log");
			this.serverLatch = new QLatch(1);
			this.queries = new ArrayDeque<Query>();
			this.qThread = new QueryThread(docId, this.queries, this.serverLatch, v);
			Thread t1 = new Thread(qThread);
			t1.start();
			this.verbose = v;
			this.timer = new Timer("IndexServer",this.verbose);
			this.kill = false;
			
		} catch (Exception e) {
			System.out.println("! Could not create index server.");
			e.printStackTrace();
		} // end try/catch
		
	} // end IndexServer() constructor
	
	public void setVerbose(boolean v) {
		this.verbose = v;
		this.qThread.setVerbose(v);
		this.timer.setVerbose(v);
	} // end setVerbose()
	
	public boolean isVerbose() {
		return this.verbose;
	} // end setVerbose()
	
	/**
	 * Query the Skearch Engine and get document IDs returned.
	 */
	public int[] queryDocID(int limit, String query) {
		
		query = query.trim();
		int [] result = new int [limit];
		Arrays.fill(result, -1);
		
		try {
			
			while (!this.kill && !query.isEmpty()) {
				
				// add new method to timer
				Method method = timer.addMethod("queryDocID");
				
				// setup search query and results
				Query search = new Query(query,limit);
				
				// add search query to queries deque
				this.timer.startTimer("Add \"" + query + "\" to search query deque",method);
				this.queries.addFirst(search);
				this.timer.stopTimer(method);
				
				// count down server latch so query threads know to check the queries deque
				this.timer.startTimer("Count down server latch",method);
				this.serverLatch.countDown();
				this.timer.stopTimer(method);
				
				try {
					
					// try waiting for search query object to get results
					this.timer.startTimer("Wait on search query latch until results are found",method);
					search.getLatch().awaitZero();
					this.timer.stopTimer(method);
					
				} catch (InterruptedException e) {
					logs.log("Index Server", "high risk", "query latch will not open");
					e.printStackTrace();
				} // end try/catch
				
				if (this.kill) {
					return result;
				} // end if
				
				// get results from search query object
				this.timer.startTimer("Get search results from query object",method);
				result = search.getDocIds();
				this.timer.stopTimer(method);
				
				// reset latch
				this.timer.startTimer("Reset server latch",method);
				this.serverLatch.reset();
				this.timer.stopTimer(method);
				
				return result;
				
			} // end if
			
		} catch (Exception e) {
			System.out.println("! Could not query the index server.");
			e.printStackTrace();
			return result;
		} // end try/catch
		
		return result;
		
	} // end queryDocID()
	
	/**
	 * DEPRECATED.
	 * Query the Skearch Engine with a docID and get a caption back.
	 * @param docID
	 * @return
	 */
	/*public String getCaption(int docID) {
		
		caps.setDocID(docID);
		serverCapLatch.countDown();
		serverCapLatch.reset();
		
		try {
			caps.cglatch.awaitZero();
		} catch (InterruptedException ie) {
			System.err.println("! Index Server: INTERRPUTED.");
		} // end try/catch
		
		return caps.getResult();
		
	} // end getCaption() */

} // end IndexServer class
