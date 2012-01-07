package search.indexserver;

import java.io.IOException;
import java.util.ArrayDeque;

import search.common.LogLibrary;
import search.common.Timer;
import search.common.Timer.Method;

/**
 * Query Thread for searching index from queue of queries.
 * @author Adam Steinberger, Sam Gunther
 */
public class QueryThread implements Runnable {
	
	private static LogLibrary logs;
	private final static Timer timer = new Timer("QueryThread",true);
	private ArrayDeque<Query> queries;
	private ArrayDeque<String> words;
	private QLatch queryLatch;
	private QLatch serverLatch;
	private boolean kill;
	private boolean verbose;
	private InvertedIndexFileReader iiReader;
	
	/**
	 * QueryThread dequeues search queries from a search queue and queries index
	 * @param sq query queue
	 * @param sl server latch
	 * @throws IOException 
	 */
	public QueryThread(int docId, ArrayDeque<Query> sq, QLatch sl, boolean vb) {
		logs = LogLibrary.instance("QueryThread.log");
		this.queries = sq;
		this.serverLatch = sl;
		this.kill = false;
		this.verbose = vb;
		this.iiReader = new InvertedIndexFileReader(docId,vb);
	} // end QueryThread constructor

	public boolean isVerbose() {
		return verbose;
	} // end isVerbose()

	public void setVerbose(boolean v) {
		this.verbose = v;
		this.iiReader.setVerbose(v);
	} // end setVerbose()

	/**
	 * Kill QueryThread process
	 */
	public void kill() {
		try {
			logs.log("Index Server", "medium risk", "kill query thread");
			this.kill = true;	
		} catch (Exception e) {
			System.out.println("! Could not kill query thread.");
			e.printStackTrace();
		} // end try/catch
	} // end kill()
	
	/**
	 * Parse Query into words pushed onto a queue.
	 * @param query
	 * @return
	 */
	private boolean parseQuery(String query) {
		
		// add new method to timer
		Method method = timer.addMethod("parseQuery");
		
		// reset query word queue
		this.words = new ArrayDeque<String>();
		
		// check query for || operators
		int hasOR = query.indexOf("||");
		
		if (hasOR >= 0) {
			
			// split query at every || and enqueue to search queue
			for (int i = 0; i < query.length(); i++) {
				
				int pos = query.indexOf(" || ", i);
				
				if (pos > 0) {
					
					String s = query.substring(i, pos);
					
					// add word to search queue
					timer.startTimer("Add \"" + s + "\" to search queue",method);
					this.words.addFirst(s);
					timer.stopTimer(method);
					
					i = pos+3;
					
				} // end if
				
			} // end for
			
			int pos = query.lastIndexOf(" || ");
			String s = query.substring(pos+4, query.length());
			
			// add word to search queue
			timer.startTimer("Add \"" + s + "\" to search queue",method);
			this.words.addFirst(s);
			timer.stopTimer(method);
			
		} else if (query.indexOf(" ") >= 0) {
			
			// split query at every || and enqueue to search queue
			for (int i = 0; i < query.length(); i++) {
				
				int pos = query.indexOf(" ", i);
				
				if (pos > 0) {
					
					String s = query.substring(i, pos);
					
					// add word to search queue
					timer.startTimer("Add \"" + s + "\" to search queue",method);
					this.words.addFirst(s);
					timer.stopTimer(method);
					
					i = pos;
					
				} // end if
				
			} // end for
			
			int pos = query.lastIndexOf(" ");
			String s = query.substring(pos+1, query.length());

			// add word to search queue
			timer.startTimer("Add \"" + s + "\" to search queue",method);
			this.words.addFirst(s);
			timer.stopTimer(method);
			
		} else {
			
			// add word to search queue
			timer.startTimer("Add \"" + query + "\" to search queue",method);
			this.words.addFirst(query);
			timer.stopTimer(method);
			
		} // end if/else
		
		return (hasOR >= 0);
		
	} // end parseQuery()
	
	/**
	 * Run QueryThread to search the index for queries
	 */
	public void run() {
		
		// add new method to timer
		Method method = timer.addMethod("run");
		
		// Make sure kill switch disengaged
		while (!this.kill) {
			
			// dequeue first search query from search queue
			Query search = this.queries.pollFirst();
			
			// open threadLatch if search queue is empty
			if (search != null) {
				
				int[] docIds;
				
				// get query latch for this search
				this.queryLatch = search.getLatch();
				
				// parse query into words
				timer.startTimer("Parse query into words",method);
				boolean hasOR = parseQuery(search.getQuery());
				timer.stopTimer(method);
				
				// start timer
				timer.startTimer("Query the inverted index",method);
				
				// query index using OR or AND search
				if (hasOR) {
					docIds = this.iiReader.queryOR(this.words,search.getLimit());
				} else {
					docIds = this.iiReader.queryAND(this.words,search.getLimit());
				} // end if
				
				// stop timer
				timer.stopTimer(method);
				
				// add results to result queue
				timer.startTimer("Attach results to query object",method);
				search.setDocIds(docIds);
				timer.stopTimer(method);
				
				// countDown query latch
				timer.startTimer("Count down query latch",method);
				this.queryLatch.countDown();
				timer.stopTimer(method);
				
			} // end if
			
			try {
				
				// try waiting for index server to get queries
				timer.startTimer("Wait on index server latch until new query is available",method);
				this.serverLatch.awaitZero();
				timer.stopTimer(method);
				
			} catch (InterruptedException e) {
				try {
					logs.log("Index Server", "high risk", "server latch will not open");
				} catch (IOException e1) {
					e1.printStackTrace();
				} // end try/catch
				e.printStackTrace();
			} // end try/catch
						
		} // end while
		
	} // end run()
	
} // end QueryThread class
