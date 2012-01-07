package search.indexserver;

import java.util.Arrays;
import search.common.Timer;
import search.common.Timer.Method;
import search.common.LogLibrary;

/**
 * Test the Index Server.
 * @author Adam Steinberger, Sam Gunther
 */
public class IndexServerTest {

	private final static Timer timer = new Timer("IndexServerTest",true);
	private static LogLibrary logs;
	
	private static boolean queryDocID(search.indexserver.IndexServer is,
			int limit, String query) {
		
		try {
			
			// add new method to timer
			Method method = timer.addMethod("queryDocID");
			
			// Query the index server
			timer.startTimer("Query the index server",method);
			int[] docs = is.queryDocID(limit,query);
			timer.stopTimer(method);
			
			if (docs == null) {
				logs.log("Index Server", "medium risk", "queryDocID returned null, " +
						"nothing was found or method failed");
				return false;
			} // end if
			
			// start timer
			timer.startTimer("RESULTS: " + Arrays.toString(docs),method);
			
			// stop timer
			timer.stopTimer(method);
			
			return true;
			
		} catch (Exception e) {
			System.out.println("! Could not query the index server.");
			e.printStackTrace();
			return false;
		} // end try/catch
		
	} // end queryDocID()
	
	/**
	 * The main test method.
	 */
	public static void main(String[] args) {
		
		try {
			
			// add new method to timer
			Method method = timer.addMethod("main");
			
			// create test log
			logs = LogLibrary.instance("IndexServerTest.log");
			
			// start index server
			timer.startTimer("Start new index server",method);
			search.indexserver.IndexServer myServ = new search.indexserver.IndexServer(0,true);
			timer.stopTimer(method);
			
			// start timer
			timer.startTimer("Query the index server for \"hello\"",method);
			
			// test one-word query
			boolean q = queryDocID(myServ,10,"hello");
			
			if (q) {
				logs.log("Index Server", "no risk", "single word query test passed");
			} else {
				logs.log("Index Server", "high risk", "single word query test failed");
			} // end if
			
			// stop timer
			timer.stopTimer(method);
			
			// start timer
			timer.startTimer("Query the index server for \"hello world\"",method);
			
			// test AND query
			q = queryDocID(myServ,10,"hello world");
			
			if (q) {
				logs.log("Index Server", "no risk", "anded words query test passed");
			} else {
				logs.log("Index Server", "high risk", "anded words query test failed");
			} // end if
			
			// stop timer
			timer.stopTimer(method);
			
			// start timer
			timer.startTimer("Query the index server for \"hello || world || search\"",method);
			
			// test OR query
			q = queryDocID(myServ,10,"hello || world || search");
			
			if (q) {
				logs.log("Index Server", "no risk", "ored words query test passed");
			} else {
				logs.log("Index Server", "high risk", "anded words query test failed");
			} // end if
			
			// stop timer
			timer.stopTimer(method);
			
			return;
			
		} catch (Exception e) {
			System.out.println("! Could not start the index server test.");
			e.printStackTrace();
		} // end try/catch
		
	} // end main()
	
} // end IndexServerTest class
