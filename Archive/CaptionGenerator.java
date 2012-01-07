package search.indexserver;
import org.htmlparser.Parser;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.ParserException;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.Node;

import search.common.Timer;
import search.common.Timer.Method;
import search.indexbuilder.ContentChunkFile;
import search.indexbuilder.DocIndexFile;
import search.indexbuilder.Doc;

import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Generate Captions from query result documents.
 * @author Sam Gunther, Adam Steinberger
 */
public class CaptionGenerator implements Runnable {
	
	private int docID;
	private String[] terms;
	private Pattern[] termRegs;
	private String captionResult;
	private boolean getPage;
	private Doc returnDoc;
	private ContentChunkFile conChunk;
	public QLatch cglatch;
	private DocIndexFile docInd;
	private static final boolean DEBUG = true;
	private Timer timer = new Timer("IndexServer",true);
	
	/**
	 * Simplified constructor for use with the new file format.
	 * @param ccf 
	 * @param docID
	 * @param terms
	 * @param pageOnly
	 */
	public CaptionGenerator(ContentChunkFile ccf, int docID, String[] terms, boolean pageOnly)
	{
		// add new method to timer
		Method method = timer.addMethod("CaptionGenerator");
		
		getPage = pageOnly;
		//this.myHandler = handler;
		cglatch = new QLatch(1);
		conChunk = ccf;
		this.docID = docID;
		this.terms = terms;
		this.termRegs = new Pattern[terms.length];
		
		for (int i=0;i<terms.length;i++) {
			StringBuilder regex = new StringBuilder("\\b");
			regex.append(this.terms[i].toLowerCase());
			regex.append("\\b");
			termRegs[i] = Pattern.compile(regex.toString());
		} // end for
	} // end new CaptionGenerator() constructor
	
	
	/**
	 * Basic constructor for CaptionGenerator
	 * @param ccf The ContentChunkFile to be searched in.
	 * @param dif The DocIndexFile to be used.
	 * @param docID The docID associated with the document that 
	 */
	public CaptionGenerator(ContentChunkFile ccf,DocIndexFile dif, int docID, String[] terms, boolean pageOnly) {
		
		// add new method to timer
		Method method = timer.addMethod("CaptionGenerator");
		
		getPage = pageOnly;
		//this.myHandler = handler;
		cglatch = new QLatch(1);
		docInd = dif;
		conChunk = ccf;
		this.docID = docID;
		this.terms = terms;
		this.termRegs = new Pattern[terms.length];
		
		for (int i=0;i<terms.length;i++) {
			StringBuilder regex = new StringBuilder("\\b");
			regex.append(this.terms[i].toLowerCase());
			regex.append("\\b");
			termRegs[i] = Pattern.compile(regex.toString());
		} // end for
		
	} // end CaptionGenerator() constructor
	
//	public void setReturnMode(boolean in)
//	{
//		getPage = in;
//	}
	
	public void run() {
		
		// add new method to timer
		Method method = timer.addMethod("run");
		
		if (getPage) {
			
			this.timer.startTimer("Retrieve page",method);
			retrievePage();
			this.timer.stopTimer(method);
			
		} else {
			
			this.timer.startTimer("Make caption",method);
			makeCaption();
			this.timer.stopTimer(method);
			
		} // end if
		
		this.timer.startTimer("Count down caption generator",method);
		cglatch.countDown();
		this.timer.stopTimer(method);
		
	} // end run()
	
//	public void setTask(int docID, String[] terms)
//	{
//		this.docID = docID;
//		this.terms = terms;
//	}
	
	public Doc getPage()
	{
		return returnDoc;
	}
	
	public void setDocID(int toFind)
	{
		this.docID = toFind;
	}
	
	public String getCaption()
	{
		return this.captionResult;
	}
	
	public void retrievePage()
	{
		long off = docInd.getDocPtr(docID);
		returnDoc = conChunk.getDocAt(off);
	}
	
	/**
	 * This internal method processes the data in terms and docID and uses them to generate a caption and puts that caption into result caption. 
	 * @return the caption resulting from the last docID and terms input
	 */
	public void makeCaption() {
		
		// add new method to timer
		Method method = timer.addMethod("makeCaption");
		
//		debugPrint("Making caption...");
		
		String htmlDoc,url;
		StringBuilder caption;
		
		//StringBuilder created, about to open ContentChunk.
//		debugPrint(".");
		if(docInd != null)
		{
			this.timer.startTimer("Content Chunk opened, getting offset and retrieving document",method);
			long off = docInd.getDocPtr(docID);
			this.timer.stopTimer(method);
		
			//long off = conChunk.getDocPtr(docID); //This is for the new version which we don't have now.
			this.timer.startTimer("Get document at offset",method);
			returnDoc = conChunk.getDocAt(off);
			this.timer.stopTimer(method);
		}
		else
		{
//			this.timer.startTimer("Content Chunk opened, getting offset and retrieving document",method);
//			long off = conChunk.getDocPtr(docID);
//			this.timer.stopTimer(method);
//		
//			//long off = conChunk.getDocPtr(docID); //This is for the new version which we don't have now.
//			this.timer.startTimer("Get document at offset",method);
//			returnDoc = conChunk.getDocAt(off);
//			this.timer.stopTimer(method);
		}
//		System.out.println("Doc Retrieved.");
		if (returnDoc == null) {
			System.out.println("Doc is null.");
		} // end if
		
		this.timer.startTimer("Get document content",method);
		htmlDoc = returnDoc.content;
		this.timer.stopTimer(method);
		
		this.timer.startTimer("Create parser",method);
		Parser pars = Parser.createParser(htmlDoc, null);
		this.timer.stopTimer(method);
		
		/*
		 * Make the Snippet here.
		 */
		StringBuilder snippet;
		this.timer.startTimer("Make document content snippet",method);
		
		try {
			
			if (terms.length == 1)
				snippet = snippetOne(pars);
			else
				snippet = snippetMulti(pars);
			
		} catch (ParserException pe) {
			pe.printStackTrace();
			return;
		} // end try/catch
		
		this.timer.stopTimer(method);
		
		//Retrieve the URL.
		this.timer.startTimer("Retrieve document URL",method);
		url = returnDoc.url;
		this.timer.stopTimer(method);
		
//		debugPrint("Snippet we have so far:");
//		debugPrint(snippet.toString());
//		debugPrint("url:");
//		debugPrint(url);
		/*
		 * Quick and dirty search for title.
		 * This should probably be fixed as it does not implement the HTML Parser.
		 */
		int titleLoc = htmlDoc.indexOf("<title>");
		
		this.timer.startTimer("Retrieve document title",method);
		
		if (titleLoc >= 0) {
//			debugPrint("Found title, removing...");
			int endTitle = htmlDoc.indexOf("</title>");
			caption= new StringBuilder(htmlDoc.substring(titleLoc+7,endTitle));
			caption.append("\n");
		} else {
//			debugPrint("No title found, using url as title...");
			caption = new StringBuilder(url);
			caption.append("\n");
		} // end if
		
		this.timer.stopTimer(method);
		
		//Now we add the rest of the caption components into the caption.
//		debugPrint("Assembling Caption...");
		this.timer.startTimer("Assemble document content caption",method);
		caption.append(url);
		caption.append("\n");
		caption.append(snippet);
		this.timer.stopTimer(method);
		
		this.timer.startTimer("Convert document content caption to string",method);
		debugPrint("makeCaption() execution finished, caption before return:");
		debugPrint(caption.toString());
		this.captionResult = caption.toString();
		this.timer.stopTimer(method);
		
	} // end makeCaption()

	private StringBuilder snippetOne(Parser p) throws ParserException {
		
		// add new method to timer
		Method method = timer.addMethod("snippetOne");
		
		this.timer.startTimer("Create string filter of terms and extract all nodes",method);
		StringFilter stf = new StringFilter(terms[0]);
		NodeList nl = p.extractAllNodesThatMatch(stf);
		this.timer.stopTimer(method);
		
		debugPrint("NodeList Length: " + nl.size());
		
		String term = terms[0];
		StringBuilder toRet = new StringBuilder();
		String temp;
		
		while (toRet.length() < 300 && nl.size() > 0) {
			
			this.timer.startTimer("Get snippet of text from node list",method);
			temp = nl.remove(0).getText();
			this.timer.stopTimer(method);
			
			if (temp.length() < 100) {
				this.timer.startTimer("Append snippet to string builder",method);
				toRet.append(temp);
				this.timer.stopTimer(method);
			} else {
				
				/*
				 * This section cuts chunks of text out of the file.
				 */
				this.timer.startTimer("Get document chunk closest to term",method);
				while (toRet.length() < 300) {
					
					int targ = temp.indexOf(term);
					int s,e;
					
					if (targ >= 0) {
						
						if (targ <= 50)
							s = 0;
						else
							s=targ-50;
						
						if (temp.length() - (targ+50) <=0)
							e = temp.length()-1;
						else
							e = targ+50;
						
						toRet.append(temp.substring(s,e));
						
					} else {
						break;
					} // end if
					
				} // end while
				this.timer.stopTimer(method);
				
			} // end if
			
		} // end while
		
		return toRet;
		
	} // end snippetOne()
	
	private StringBuilder snippetMulti(Parser p) throws ParserException {
		
		// add new method to timer
		Method method = timer.addMethod("snippetMulti");
		
		String[] words = new String[3000];
		
		this.timer.startTimer("Convert parser to text string",method);
		String full = convertToText(p);
		this.timer.stopTimer(method);
		
		StringTokenizer sto = new StringTokenizer(full);
		debugPrint("Full: "+full.length());
		/*
		 * First, we use the convertToText method to create a large string which contains all the text in the document
		 * and then use StringTokenizer to create a 1000 element array of the first 1000 words in the array.
		 */
		int pos = 0;
		this.timer.startTimer("Add snippet words to words array",method);
		while (sto.hasMoreTokens() && pos < 1000) {
			String temp = sto.nextToken();
			words[pos] = temp;
			pos++;
		} // end while
		int numOfWords = pos;
		
		this.timer.stopTimer(method);
		
		if(DEBUG)
		{
			System.out.println("Now Testing word Array, first 20 words:");
			for(int k=0;k<20;k++)
			{
				System.out.print(words[k]+" ");
			}
		}
		
		/*
		 * Now we get six ranked items from the array.
		 */
		double[] ranks = new double[6];
		StringBuilder[] candidates = new StringBuilder[6];
		pos = 0;
		int docPos = 0;
		double uniqueFound = 0;
		int wordsInCurrent = 0;
		boolean[] found = new boolean[terms.length];
		
		for (int b=0; b<found.length; b++) {
			found[b] = false;
		} // end for
		
		this.timer.startTimer("Make and rank snippets of text from document content",method);
		
		//Main search loop
		while (docPos<numOfWords && pos < 6) {
			
			//First, check to see if we have more words left, if we don't, close up.
			if (words[docPos] == null) {
				
				if (candidates[pos] != null) {
					double rank = uniqueFound/terms.length;
//					this.timer.startTimer("Add rank " + rank + " to term " + words[pos],method);
					ranks[pos] = rank;
					pos++;
//					this.timer.stopTimer(method);
				} // end if
				
				break;
				
			} else {
				
//				this.timer.startTimer("Check if word is a query term",method);
				int t = isTerm(words[docPos]);
//				this.timer.stopTimer(method);
				
				//If the current word is a term...
				if (t != -1) {
					
					//First, note that we have found this:
					if (!found[t]) {
//						this.timer.startTimer("Query term found",method);
						uniqueFound++;
						found[t] = true;
//						this.timer.stopTimer(method);
					} // end if
				
					//Next, either create a new candidate StringBuilder, or append to the existing one.
					if (candidates[pos] == null) {
						
//						this.timer.startTimer("Create new string builder and append to candidates array",method);
						candidates[pos] = new StringBuilder(words[docPos]);
						wordsInCurrent++;
//						this.timer.stopTimer(method);
					
					//Otherwise, we are in the process of assembling a StringBuilder, we add this String, and note that we've found it.
					} else {
						
//						this.timer.startTimer("Append existing string builder to candidates array",method);
						candidates[pos].append(words[pos]);
						wordsInCurrent++;
//						this.timer.stopTimer(method);
						
					} // end if
				
				//Otherwise, if the current word is not a term,
				} else {
					
					//If we're working on a candidate, we add this String to it.
					if (candidates[pos] != null) {
//						this.timer.startTimer("If we're working on a candidate, we add " + words[docPos] + " to candidates",method);
						candidates[pos].append(" ");
						candidates[pos].append(words[docPos]);
//						this.timer.stopTimer(method);
					} // end if
					
				} // end if
			
				//Finally, check if we're done with this stringbuilder...
//				this.timer.startTimer("Check if we're done with string builder",method);
				
				if (candidates[pos]!=null) {
					
					if (wordsInCurrent > 50 || uniqueFound == terms.length) {
						
						ranks[pos] = uniqueFound/terms.length;
						pos++;
						uniqueFound = 0;
						wordsInCurrent = 0;
						
						for (int b=0;b<found.length;b++) {
							found[b] = false;
						} // end for
						
					} // end if
					
				} // end if
				
//				this.timer.stopTimer(method);
				
				docPos++;
				
			} // end if
			
		} // end while
		
		this.timer.stopTimer(method);
		
		if(DEBUG)
		{
			System.out.println("DEBUG Ranks:");
			for(int f=0;f<6;f++)
			{
				if(candidates[f]==null)
					break;
				System.out.println(candidates[f] + "|RANK:|" + ranks[f]);
			}
			if(candidates[0]!=null)
			{
				debugPrint("Doublechecking ranks:");
				StringTokenizer stroc = new StringTokenizer(candidates[0].toString());
				while(stroc.hasMoreTokens())
				{
					String w = stroc.nextToken();
					debugPrint("Word: " + w + ", isTerm: "+isTerm(w));
				}
			}
		}
		
		//We now have up to six potential candidates for the snippet, so we need to put them together.
		StringBuilder snippet = new StringBuilder("");
		int highestInd = 0;
		double highestRank = 0;
		
		while (snippet.length() < 150) {
			
			highestInd = 0;
			highestRank = 0;
			
			this.timer.startTimer("Find highest ranking candidate",method);
			for (int i=0;i<pos-1;i++) {
				if (highestRank < ranks[i]) {
					highestRank = ranks[i];
				} // end if
			} // end for
			this.timer.stopTimer(method);
			
			//If highestRank is > 0, then we have candidates to use:
			if (highestRank > 0) {
				
				this.timer.startTimer("Append highest ranked candidate to snippet",method);
				snippet.append(candidates[highestInd]);
				ranks[highestInd] = 0;
				this.timer.stopTimer(method);
				
			//Otherwise, we just take the beginning of the document:
			} else {
				
				this.timer.startTimer("Append beginning of document to snippet",method);
				if (full.length() < 151) {
					snippet.append(full.substring(0,full.length()-1));
				} else {
					snippet.append(full.substring(0, 151));
				} // end if
				this.timer.stopTimer(method);
				
				break;
				
			} // end if
			
		} // end while
		
		return snippet;
		
	} // end snippetMulti()
	
	/**
	 * 
	 * @param s
	 * @return the index of the term that s.equals(), or -1.
	 */
	private int isTerm(String s) {
		
		int toRet = -1;
		for (int i=0;i<terms.length;i++) {
			Matcher m = termRegs[i].matcher(s.toLowerCase());
			if (m.find()) {
				toRet = i;
				break;
			} // end if
		} // end for
		
		return toRet;
		
	} // end isTerm()
	
	private String convertToText(Parser con) throws ParserException {
		
		NodeIterator ni = con.elements();
		StringBuilder text = new StringBuilder("");
		
		while (ni.hasMoreNodes()) {
			processNodes(ni.nextNode(),text);
		} // end while
		
		return text.toString();
		
	} // end convertToText()
	
	private void processNodes(Node node,StringBuilder strb) {
		
		if (node instanceof TextNode) {
			
			// downcast to TextNode
			TextNode text = (TextNode) node;
			strb.append(text.getText());
			
		} else if (node instanceof TagNode) {
			
			TagNode tag = (TagNode) node;
			NodeList nl = tag.getChildren();
			
			if (nl != null) {
				try {
					for (NodeIterator i=nl.elements(); i.hasMoreNodes(); ) {
						processNodes(i.nextNode(), strb);
					} // end for
				} catch (Exception ex) {
					System.out.println(ex);
				} // end try/catch
			} // end if
			
		} // end if
		
	} // end processNodes()
	
	private void debugPrint(String s) {
		if (DEBUG) {
			System.out.println("DEBUG " + s);
		} // end if
	} // end debugPrint()
}
