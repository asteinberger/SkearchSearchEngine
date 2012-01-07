package search.indexserver;

/**
 * Hold QueryThreads until querying process completes for a single query.
 * @author Adam Steinberger, Sam Gunther
 */
public class QLatch {
	
	private final Object synchObj = new Object();
	private int count;
	private int startCount;
	
	/**
	 * QLatch constructor synchronizes synchObj for individual access to query queue.
	 * @param numThreads
	 */
	public QLatch(int numThreads) {
		synchronized (synchObj) {
			this.count = numThreads;
			this.startCount = numThreads;
		} // end synchronized
	} // end QLatch constructor
	
	/**
	 * Call this on main thread while background threads complete their work.
	 * @throws InterruptedException
	 */
	public void awaitZero() throws InterruptedException {
		synchronized (synchObj) {
			while (count > 0) {
				synchObj.wait();
			} // end while
		} // end synchronized
	} // end awaitZero()
	
	/**
	 * Call this when a thread completes its work.
	 */
	public void countDown() {
		synchronized (synchObj) {
			if (--count <= 0) {
				synchObj.notifyAll();
			} // end if
		} // end synchronized
	} // end countDown()
	
	/**
	 * Reset the QLatch.
	 */
	public void reset() {
		this.count = this.startCount;
	} // end reset()
	
} // end QLatch class
