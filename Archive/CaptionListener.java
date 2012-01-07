package search.indexserver;
import java.io.*;
import  java.net.*;
import java.util.Random;
import java.util.StringTokenizer;

import search.indexbuilder.DocIndexFile;
import search.indexbuilder.ContentChunkFile;

public class CaptionListener 
{
	
	private static DocIndexFile docIndex;
	private static ContentChunkFile contentFile;
	
	public static void main(String [] args) throws Exception
	{
		System.out.println("Reading config file...");
		BufferedReader configRead = new BufferedReader(new FileReader("fileNums.config"));
		int initNum = Integer.parseInt(configRead.readLine());
		docIndex = new DocIndexFile(initNum);
		contentFile = new ContentChunkFile(initNum);
		
		ServerSocket listener = new ServerSocket(Integer.parseInt(args[0]));
		System.out.println("! Listening on port: "+ listener.getLocalPort());
		
		while(true)
		{ 
			Socket connection = listener.accept();
			
			System.out.println("! Accepted connection from: "+
					connection.getRemoteSocketAddress()); //returns where the connection is from
		
			
			BufferedReader clientIn = new  BufferedReader(
					new InputStreamReader(connection.getInputStream()));
			DataOutputStream clientOut = new DataOutputStream(
					connection.getOutputStream());
			
			ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
	        ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
			
			
	        System.out.println("Incoming string string about to be read");
			
			//reads an incoming string
			String input = clientIn.readLine();
			input.trim();
			
			
		
			StringTokenizer st = new StringTokenizer(input);
			int count= st.countTokens();
			
			String[] queryArray = new String[count];
				int index =0;
				while(st.hasMoreTokens())
				{
					queryArray[index] = st.nextToken();
					System.out.println(queryArray[index]+" read into the array");
					index++;
				}
				
				
			System.out.println("String has been read " +input);
			//reads the incoming doc id
			int incoming = clientIn.read();
			System.out.println("int has been read " + incoming);
			
			/*New CaptionGenerator System
			 * Creating thread:
			 */
			CaptionGenerator capGen = new CaptionGenerator(contentFile,docIndex,incoming,queryArray,false);
			Thread qThread = new Thread(capGen,"CaptionGenerator");
			qThread.start();
			capGen.cglatch.awaitZero();
			
			String caps = capGen.getCaption();
				
			System.out.println("A caption related to the id" + caps);
			
		
			
			out.writeObject(caps); //sends the caption info over the network over the network
			//to FrontEndCaps which makes the caps available to FrontEnd
			
			System.out.println("The data object(cation) has been sent over the network");
				
			connection.close();
			System.out.println("!Remote Client Disconnected: "+ connection.getInetAddress());
			
		}
		
	}

}