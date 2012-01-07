package search.indexserver;

import search.indexbuilder.ContentChunkFile;
import search.indexbuilder.DocIndexFile;

/**
 * This is the class is for testing.
 * @author Sam Gunther, Adam Steinberger
 */

public class CaptionGeneratorTest
{

	public static void main(String[] args) throws InterruptedException
	{
		DocIndexFile dif = new DocIndexFile(0);
		ContentChunkFile ccf = new ContentChunkFile(0);
		String[] terms = new String[3];
		terms[0] = "black";
		terms[1] = "cat";
		terms[2] = "hello";
		CaptionGenerator cg = new CaptionGenerator(ccf,dif,58,terms,false);
		Thread t0 = new Thread(cg,"CaptionGenerator");
		t0.start();
		cg.cglatch.awaitZero();
		System.out.println(cg.getCaption());
	}
	

}
