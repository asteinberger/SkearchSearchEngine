package search.indexserver;
import java.io.*;
import  java.net.*;
import java.util.Arrays;

import search.common.Timer;
import search.common.Timer.Method;


public class IndexServerListener {
	
	private static Timer timer = new Timer("IndexServerListener",true);
	private static IndexServer instance;
	
	public static void main(String [] args) throws Exception {
		
		// add new method to timer
		Method method = timer.addMethod("main");
		
		BufferedReader configRead = new BufferedReader(new FileReader("fileNums.config"));
		int initNum = Integer.parseInt(configRead.readLine());
		instance = new IndexServer(initNum,true);
		
		timer.startTimer("Connecting to port " + args[0],method);
		ServerSocket listener = new ServerSocket(Integer.parseInt(args[0]));
		timer.stopTimer(method);
		
		while (true) {
			
			timer.startTimer("Listening on port " + args[0],method);
			Socket connection = listener.accept();
			timer.stopTimer(method);
			
			timer.startTimer("Accepted connection from " + connection.getRemoteSocketAddress(),
					method);
			
			BufferedReader clientIn = new  BufferedReader(
					new InputStreamReader(connection.getInputStream()));
			DataOutputStream clientOut = new DataOutputStream(
					connection.getOutputStream());
			
			ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
	        ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
	        
	        timer.stopTimer(method);
			
			//reads an incoming string
	        timer.startTimer("Reading incoming query string",method);
			String input = clientIn.readLine();
			timer.stopTimer(method);
			
			timer.startTimer("Incoming query string " + input + " read",method);
			input.trim();
			timer.stopTimer(method);
			
			timer.startTimer("Querying the index server",method);
			int[] ids = instance.queryDocID(10,input);
			timer.stopTimer(method);
			
			timer.startTimer("Sending docIds " + Arrays.toString(ids) + " over the network",method);
			out.writeObject(ids); //sends the array over the network
			timer.stopTimer(method);
			
			//this is where method calling takes place
			//specific to front-end communication
			
			//gets a query string from the front-end and communicates with a list of indexServers
			timer.startTimer("Remote client " + connection.getInetAddress() + " disconnected",method);
			connection.close();
			timer.stopTimer(method);
			
		} // end while
		
	} // end main()

} // end IndexServerListener class
