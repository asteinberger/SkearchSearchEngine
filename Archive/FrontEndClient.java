package search.indexserver;

import java.io.*;
import  java.net.*;

/**
 * Front-End Client for Skearch
 * @author Tsepang Tsolele
 */
public class FrontEndClient {
	
	//will use this to access methods in frontEnd
	private static int[] DocID;
	
	//frontEnd will call this method to get a string of id's
	public int[] getData() {
		return DocID;
	} // end getData()
	
	public static int[] sendQueryToIndex(String s) {
		
		try {
			
			//open up a new socket with index server
			Socket clientSocket1 = new Socket("indexserver1", 9001);
			
			System.out.println("! Connected to: "+ 
					clientSocket1.getInetAddress()+" on port: "
					+ clientSocket1.getLocalPort());

			// assume we have got the searchString from the front end
			String serverDataOut = "hello world" + "\n";

			// set up dataStreams and DocIDectStreams
			DataOutputStream serverOut1 = new DataOutputStream(
					clientSocket1.getOutputStream());
			BufferedReader serverIn1 = new BufferedReader(
					new InputStreamReader(clientSocket1.getInputStream()));

			ObjectOutputStream out = new ObjectOutputStream(
					clientSocket1.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(
					clientSocket1.getInputStream());

			// send query
			serverOut1.writeBytes(serverDataOut);
			System.out.println(">sent: " + serverDataOut + " to server");

			// string to search has been sent out to the IndexServerListener
			DocID = (int[]) in.readObject();

			for (int i = 0; i < DocID.length; i++) {
				System.out.print(DocID[i] + " ");
			} // end for
			
			System.out.print("\n");

			// close and disconnect when results are printed
			clientSocket1.close();
			
			System.out.println("! Disconnected from remote host: "
					+ clientSocket1.getInetAddress());

			return DocID;
				
		} catch (Exception e) {
			System.out.println("! Could not send query to index server.");
			e.printStackTrace();
			return null;
		} // end try/catch
		
	} // end sendQueryToIndex()
	
	public static void main(String[] args) {
		
		try {
			sendQueryToIndex("Search");
		} catch (Exception e) {
			System.out.println("! Could not search.");
			e.printStackTrace();
		} // end try/catch
		
	} // end main()
	
} // end FrontEndClient class
