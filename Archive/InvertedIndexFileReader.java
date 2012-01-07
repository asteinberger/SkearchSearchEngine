package search.indexserver;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.TreeMap;
import search.common.Timer;
import search.common.Timer.Method;

/**
 * Read the Inverted Index file into memory and query the index.
 * @author Adam Steinberger, Sam Gunther
 */
public class InvertedIndexFileReader {
	
	// provides random access to the index (e.g. to jump to an offset in the index area)
	private RandomAccessFile indexFile;
	
	// provides sequential access to the index (for reading the lex map into memory)
	private DataInputStream indexFile1;
	
	// the in-memory lexicon for looking up offsets/ndocs by word
	private TreeMap<String, LexMapEntry> lexMap;
	
	private final static Timer timer = new Timer("InvertedIndexFileReader",true);
	private boolean verbose;
	
	public boolean isVerbose() {
		return verbose;
	} // end isVerbose()

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	} // end setVerbose()

	/**
	 * Create a new inverted index file reader given a docId.
	 * Note: There is a bit of a startup delay while reading the lexicon into memory.
	 * @param docId
	 */
	public InvertedIndexFileReader(int docId, boolean v) {
		
		// add new method to timer
		Method method = timer.addMethod("InvertedIndexFileReader");
		
		try {
			
			// initiate timer
			this.verbose = v;
			
			// fire up the random and sequential file readers
			timer.startTimer("Open index chunk for reading",method);
			String filename = String.format("chunk%1$016x.index", docId);
			this.indexFile = new RandomAccessFile(new File(filename), "r");
			this.indexFile1 = new DataInputStream(new FileInputStream(new File(filename)));
			timer.stopTimer(method);
			
			// read the lexicon into memory (this takes a minute)
			timer.startTimer("Read Lexicon Map into Memory",method);
			this.lexMap = getLexMap();
			timer.stopTimer(method);
			
		} catch (Exception ex) {
			System.out.println("! Could not create inverted index file reader.");
			System.out.println(ex);
		} // end try/catch
		
	} // end InvertedIndexFileReader constructor
	
	/**
	 * Transfer query words to priority queue based on nDocs size.
	 * @param words
	 */
	private PriorityQueue<Word> enqueueWords(ArrayDeque<String> words) {
		
		PriorityQueue<Word> result = new PriorityQueue<Word>();
		Iterator<String> it = words.iterator();
		
		// add new method to timer
		Method method = timer.addMethod("enqueueWords");
		
		while (it.hasNext()) {
			
			String word = (String) it.next();
			int nDocs = lexMap.get(word).nDocs;
			Word w = new Word(word,nDocs);
			
			// add word to queue
			timer.startTimer("Enqueue \"" + w.getWord() + "\" to priority queue",method);
			result.add(w);
			timer.stopTimer(method);
			
		} // end while
		
		return result;
		
	} // end enqueueWords()
	
	/**
	 * Query words ORed together.
	 * @return
	 */
	public int[] queryOR(ArrayDeque<String> words, int limit) {
		
		// add new method to timer
		Method method = timer.addMethod("queryOR");
		
		// enqueue search words based on nDocs size
		timer.startTimer("Enqueue search terms",method);
		PriorityQueue<Word> search = enqueueWords(words);
		timer.stopTimer(method);
		
		// get docIds for word with smallest nDocs.
		Word word = search.poll();
		
		// get docIds for search term
		timer.startTimer("Search inverted index for \"" + word.getWord() + "\"",method);
		int[] results = getDocIds(word.getWord(),word.getnDocs());
		timer.stopTimer(method);
		
		// merge docIds for all other words, so only common docIds remain
		while (!search.isEmpty()) {
			
			// get next search term
			word = search.poll();
			
			// get docIds for search term
			timer.startTimer("Search inverted index for \"" + word.getWord() + "\"",method);
			int[] temp = getDocIds(word.getWord(),word.getnDocs());
			timer.stopTimer(method);
			
			// merge search results
			timer.startTimer("Merge search results",method);
			results = mergeOR(temp,results,limit);
			timer.stopTimer(method);
			
		} // end while
		
		// return null if no results found.
		if (results.length == 0) {
			System.out.println("! No results found.");
			results = null;
		} // end if
		
		return results;
		
	} // end queryOR()
	
	/**
	 * Merge two int arrays so all unique elements in both arrays are added to new array
	 * @param t
	 * @param r
	 * @return
	 */
	private int[] mergeOR(int[] t, int[] r, int limit) {
		
		// add new method to timer
		Method method = timer.addMethod("mergeOR");
		
		// start timer
		timer.startTimer("Add all docIds to results array",method);
		
		int[] result = null;
		if (r.length > t.length) {
			if (t.length >= limit) {
				result = Arrays.copyOf(t,limit);
			} else {
				int size = limit;
				if (t.length + r.length < limit) {
					size = t.length + r.length;
				} // end if
				result = new int [size];
				for (int i = 0; i < t.length; i++) {
					result[i] = t[i];
				} // end for
				int index = -1;
				for (int i = t.length; i < size; i++) {
					int search = -1;
					while (search < 0) {
						index++;
						Arrays.binarySearch(t,r[index]);
					} // end while
					result[i] = r[index];
				} // end for
			} // end if
		} else {
			if (r.length >= limit) {
				result = Arrays.copyOf(r,limit);
			} else {
				int size = limit;
				if (r.length + t.length < limit) {
					size = r.length + t.length;
				} // end if
				result = new int [size];
				for (int i = 0; i < r.length; i++) {
					result[i] = r[i];
				} // end for
				for (int i = r.length; i < size; i++) {
					result[i] = t[i-r.length];
				} // end for
			} // end if
		} // end if
		
		// stop timer
		timer.stopTimer(method);
		
		return result;
		
	} // end mergeOR()
	
	/**
	 * Query words ANDed together.
	 * @return
	 */
	public int[] queryAND(ArrayDeque<String> words, int limit) {
		
		// add new method to timer
		Method method = timer.addMethod("queryAND");
		
		// enqueue search words based on nDocs size
		timer.startTimer("Enqueue search terms",method);
		PriorityQueue<Word> search = enqueueWords(words);
		timer.stopTimer(method);
		
		// get docIds for word with smallest nDocs.
		Word word = search.poll();
		
		// get docIds for search term
		timer.startTimer("Search inverted index for \"" + word.getWord() + "\"",method);
		int[] results = getDocIds(word.getWord(),word.getnDocs());
		timer.stopTimer(method);
		
		// merge docIds for all other words, so only common docIds remain
		while (!search.isEmpty()) {
			
			// get next search term
			word = search.poll();
			
			// get docIds for search term
			timer.startTimer("Search inverted index for \"" + word.getWord() + "\"",method);
			int[] temp = getDocIds(word.getWord(),word.getnDocs());
			timer.stopTimer(method);
			
			// merge search results
			timer.startTimer("Merge search results",method);
			results = mergeAND(temp,results,limit);
			timer.stopTimer(method);
			
		} // end while
		
		// resize results to results limit
		if (results.length > limit) {
			
			// resize results array
			timer.startTimer("Resize results array to size " + limit,method);
			results = Arrays.copyOf(results, limit);
			timer.stopTimer(method);
			
		} // end if
		
		// return null if no results found.
		if (results.length == 0) {
			System.out.println("! No results found.");
			results = null;
		} // end if
		
		return results;
		
	} // end queryAND()
	
	/**
	 * Merge two int arrays so only elements common to both arrays remain
	 * @param t
	 * @param r
	 * @return
	 */
	private int[] mergeAND(int[] t, int[] r, int limit) {
		
		// add new method to timer
		Method method = timer.addMethod("mergeAND");
		
		int size = limit;
		if (t.length < limit || r.length < limit) {
			size = Math.min(t.length,r.length);
		} // end if
		
		// start timer
		timer.startTimer("Add all docIds common to both arrays to list",method);
		
		int[] result = new int [size];
		int i = 0, j = 0, index = 0;
		while ((i < t.length) && (j < r.length) && (index < size)) {
			if (t[i] > r[j]) {
				j++;
			} else if (t[i] < r[j]) {
				i++;
			} else {
				result[index] = t[i];
				index++;
				i++;
				j++;
			} // end if
		} // end while
		
		// stop timer
		timer.stopTimer(method);
		
		return result;
		
	} // end mergeAND()
	
	/**
	 * Read the Lex Map into Memory.
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private TreeMap<String, LexMapEntry> getLexMap() {
		
		// add new method to timer
		Method method = timer.addMethod("getLexMap");
		
		// create new LexMap
		TreeMap<String, LexMapEntry> map = new TreeMap<String, LexMapEntry>();
		
		try {
			
			// the first 8 bytes tell us the offset that we
			// should STOP reading the lexicon from (that's the
			// end of the lex area and beginning of the index area)
			timer.startTimer("Get offset to stop reading lexicon from",method);
			byte[] boundaryBytes = new byte[8];
			indexFile1.read(boundaryBytes);
			long lexMapBoundary = ByteBuffer.wrap(boundaryBytes).getLong();
			int offset = 8;
			timer.stopTimer(method);
			
			// start timer
			timer.startTimer("Read word and nDocs info from lexicon",method);
			
			// now, read the word and its lexmapentry (ndocs-int, offsetToIndexArea-long)
			// we keep reading until we reach the end of the lexMap (lexMapBoundary)
			String word = "";
			while (offset < lexMapBoundary) {
				
				// get the word
				word = indexFile1.readLine();
				offset+= word.getBytes().length;
								
				// get ndocs, offset
				LexMapEntry entry = new LexMapEntry(indexFile1.readInt(),indexFile1.readLong());
				map.put(word.toString(), entry);
				offset += 12;
				
			} // end while
			
			// stop timer
			timer.stopTimer(method);
		
		} catch (Exception ex) {
			System.out.println("! Could not read from the lexicon.");
			ex.printStackTrace();
		} // end try/catch
		
		return map;
		
	} // end getLexMap()
	
	/**
	 * Return an array of DocIds for single-word query.
	 * @param word
	 * @param limit
	 * @return
	 */
	public int[] getDocIds(String word, int limit) {
		
		// add new method to timer
		Method method = timer.addMethod("getDocIds");
		
		if (limit > 0) {
			
			try {
				
				// get the number of matching docs from the lexicon for this word
				int nDocs = lexMap.get(word).nDocs;
				
				// now seek into the index file at the offset we get from the lexicon for this word
				timer.startTimer("Seek word pointer for search query in lexicon",method);
				indexFile.seek(lexMap.get(word).ptr);
				timer.stopTimer(method);

				// limit is what we use to stop reading from the index - we stop when we hit the limit given
				// at query time, or nDocs if that is smaller than what the user wants
				if (nDocs < limit) {
					limit = nDocs;
				} // end if
				
				// the array of docIds to return
				int[] docIds = new int [limit];

				int docsFound = 0;		// number of docs found so far
				int docsSearched = 0;	// number of docs searched so far

				// start timer
				timer.startTimer("Get docIds from the lexicon",method);
				
				// we set done = true once we have enough docs
				boolean done = false;
				while ((docsFound < limit) && !done) {
					
					docIds[docsFound++] = indexFile.readInt();
					
					if (++docsSearched == limit) {
						done = true;	// if we have searched enough...
					} else {
						// now move to next docId - we have to skip over the current docId's
						// hitlist to get to the next one
						indexFile.seek(indexFile.getFilePointer() + getHitListSize());
					} // end if
					
				} // end while
				
				// stop timer
				timer.stopTimer(method);
				
				// return the query results
				return docIds;
				
			} catch (IOException e) {
				System.out.println("! Could not get docIds from lexicon.");
				e.printStackTrace();
				return null;
			} // end try/catch
			
		} else {
			System.out.println("! ERROR: Limit must be greater than zero!");
			return null;
		} // end if
		
	} // end getDocIds()
	
	/**
	 * Read the HitList for given docId and return size of that hitlist
	 * @return
	 */
	private int getHitListSize() {
		try {
			int hitListSize = (indexFile.readByte() & 0xff);
			if (hitListSize < 0xff) {
				return (hitListSize & 0xff) * 2 + 1;
			} else {
				return (indexFile.readShort() * 2) + 3;
			} // end if
		} catch (Exception e) {
			System.out.println("! Could not get hit list size.");
			e.printStackTrace();
			return -1;
		} // end try/catch
	} // end getHitListSize()
	
} // end InvertedIndexFileReader class

/**
 * LexMapEntry is what is stored for each word in lexicon.
 * How many docs in this index contain the word,
 * and pointer to place in index area of inverted index where entries for this word start.
 * @author Adam Steinberger, Sam Gunther
 */
class LexMapEntry {
	
	public Integer nDocs;
	public Long ptr;
	
	/**
	 * LexMapEntry constructor.
	 * @param nd nDocs
	 * @param pt Pointer
	 */
	public LexMapEntry(Integer nd, Long pt) {
		nDocs = nd;
		ptr = pt;
	} // end LexMapEntry constructor
	
} // end LexMapEntry class
