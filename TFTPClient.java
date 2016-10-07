// TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   
 
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class TFTPClient {

   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket sendReceiveSocket;
   public static Controller controller;
   private int count;
   
   public TFTPClient()
   {
      try {
         // Construct a datagram socket and bind it to any available
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets.
         sendReceiveSocket = new DatagramSocket();
	     sendReceiveSocket.setSoTimeout(10000);
		 count=0;
      } catch (SocketException se) {   // Can't create the socket.
         se.printStackTrace();
         System.exit(1);
      }
   }

   public void sendAndReceive(String request, String filename, String mode)
   {
      byte[] msg = new byte[100], // message we send
             fn, // filename as an array of bytes
             md, // mode as an array of bytes
             data; // reply as array of bytes
      int j, len, sendPort;
      boolean quit = false; //Used for exit condition
      //TODO: use this instead of static
      //String path = controller.getPath();
      String path = ".\\client\\";
      
      //If user enters "normal" as the mode
      //user sends directly to port 69 on the server
      //otherwise it sends to the error simulator
      if (controller.getRunMode().equals("normal")) 
         sendPort = 69;
      else
         sendPort = 23;
         
       msg[0] = 0;
       if(request.equalsIgnoreCase("READ")) 
           msg[1]=1;
       if(request.equalsIgnoreCase("WRITE")) 
           msg[1]=2;
       
       //Create class instance handling file I/O
       //If client is writing, it's reading from a file, if
       //client is reading, it's writing to a file
       TFTPReadWrite fileHandler;
       if(request.equalsIgnoreCase("READ")) {
    	   try{
    		   fileHandler = new TFTPReadWrite(filename, "WRITE", path, "Client");
    	   }catch(TFTPException e){
				byte[] error = e.getErrorBytes();
				return;
    	   }
       } else {
    	   try{
    		   fileHandler = new TFTPReadWrite(filename, "READ", path, "Client");
    	   }catch(TFTPException e){
				byte[] error = e.getErrorBytes();
				return;
    	   }
       }
       
       // convert to bytes
       fn = filename.getBytes(); 
       
       // and copy into the msg
       System.arraycopy(fn,0,msg,2,fn.length);
       // format is: source array, source index, dest array,
       // dest index, # array elements to copy
       // i.e. copy fn from 0 to fn.length to msg, starting at
       // index 2
        
       // now add a 0 byte
       msg[fn.length+2] = 0;

       // now add "octet" (or "netascii")
       md = mode.getBytes();
        
       // and copy into the msg
       System.arraycopy(md,0,msg,fn.length+3,md.length);
        
       len = fn.length+md.length+4; // length of the message
       // length of filename + length of mode + opcode (2) + two 0s (2)
       // second 0 to be added next:

       // end with another 0 byte 
       msg[len-1] = 0;

       // Construct a datagram packet that is to be sent to a specified port
       // on a specified host.
       // The arguments are:
       //  msg - the message contained in the packet (the byte array)
       //  the length we care about - k+1        
       //  InetAddress.getLocalHost() - the Internet address of the
       //     destination host
       //     In this example, we want the destination to be the same as
       //     the source (i.e., we want to run the client and server on the
       //     same computer). InetAddress.getLocalHost() returns the Internet
       //     address of the local host.
       //  69 - the destination port number on the destination host.
       
       try {
    	   sendPacket = new DatagramPacket(msg, len,
    			   InetAddress.getLocalHost(), sendPort);
       } catch (UnknownHostException e) {
    	   e.printStackTrace();
    	   System.exit(1);
       }
       if (controller.getOutputMode().equals("verbose")){
    	   System.out.println("To host: " + sendPacket.getAddress());
    	   System.out.println("Destination host port: " + sendPacket.getPort());
    	   len = sendPacket.getLength();
    	   System.out.println("Length: " + len);
    	   System.out.println("Containing: ");
    	   for (j=0;j<len;j++) {
    		   System.out.println("byte " + j + " " + msg[j]);
    	   }
       }
       
       try {
           sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
           e.printStackTrace();
           System.exit(1);
        }
       if (controller.getOutputMode().equals("verbose"))
    	   System.out.println("Client: Packet sent.");
       
       int i = 1;

       while (!quit) {

    	   data = new byte[516];
	       receivePacket = new DatagramPacket(data, data.length);
	       
	       if (controller.getOutputMode().equals("verbose"))
	    	   System.out.println("Client: Waiting for packet.");
	       
	       try {
	           // Block until a datagram is received via sendReceiveSocket.
	           sendReceiveSocket.receive(receivePacket);
	       } catch (SocketTimeoutException e) {
	   			if(controller.quit) {
	   				sendReceiveSocket.close();
	   				System.exit(0);
	   			}
	        } catch(IOException e) {
	           e.printStackTrace();
	           System.exit(1);
	        }
	       System.out.println(receivePacket.getPort());
	    	   
	       
	       // Process the received datagram.
	       len = receivePacket.getLength();
	       System.out.println("Client: Packet received:");
	       if (controller.getOutputMode().equals("verbose")){
	    	   System.out.println("From host: " + receivePacket.getAddress());
	    	   System.out.println("Host port: " + receivePacket.getPort());
	    	   System.out.println("Length: " + len);
	    	   int packetNo = (int) ((data[2] << 8) + data[3]);
	    	   System.out.println("Packet No.: " + packetNo);
	       }
	       
	       if (request.equalsIgnoreCase("READ")){
	    	   fileHandler.writeFilesBytes(Arrays.copyOfRange(data, 4, len));
	    	   System.out.println("Data length: " + len);
	    	   if (len < 516)
	    		   break;
	       }
	       
	       quit = false;
	     //Preparing next packet
	       if(request.equalsIgnoreCase("WRITE")) {
	    	   int length = 512;
	    	   if (i == fileHandler.getNumSections())
	    		   length = fileHandler.getFileLength() - ((fileHandler.getNumSections()-1) * 512);
	    	   msg = new byte[length+4];
	    	   msg[0] = 0;
	    	   msg[1] = 4;
	    	   msg[2] = (byte) ((i >> 8)& 0xff);
	    	   msg[3] = (byte) (i & 0xff);
	    	   System.arraycopy(fileHandler.readFileBytes(length), 0, msg, 4, length);
	    	   len = length+4;
	    	   System.out.println(length);
	    	   if(i >= fileHandler.getNumSections() )
	    		   quit = true;
	       } else if(request.equalsIgnoreCase("READ")) {
	    	   msg = new byte[4];
	    	   msg[0] = 0;
	    	   msg[1] = 3;
	    	   msg[2] = data[2];
	    	   msg[3] = data[3];
	    	   len = 4;
	       }
	       
	       try {
	    	   sendPacket = new DatagramPacket(msg, len,
	    			   InetAddress.getLocalHost(), receivePacket.getPort());
	       } catch (UnknownHostException e) {
	    	   e.printStackTrace();
	    	   System.exit(1);
	       }
	       System.out.println(sendPacket.getPort());
	       if (controller.getOutputMode().equals("verbose")){
	    	   System.out.println("To host: " + sendPacket.getAddress());
	    	   System.out.println("Destination host port: " + sendPacket.getPort());
	    	   len = sendPacket.getLength();
	    	   System.out.println("Length: " + len);
	    	   for (j=0;j<len;j++) {
	    		   System.out.println("byte " + j + " " + msg[j]);
	    	   }
	    	   int packetNo = (int) ((msg[2] << 8) + msg[3]);
	    	   System.out.println("Byte Packet No.: " + msg[2] + " " + msg[3]);
	    	   System.out.println("Packet No.: " + packetNo);
	    	   // Form a String from the byte array, and print the string.
	           String sending = new String(msg,0,len);
	           System.out.println(sending);
	       }

	       // Send the datagram packet to the server via the send/receive socket.
	
	       try {
	           sendReceiveSocket.send(sendPacket);
	        } catch (IOException e) {
	           e.printStackTrace();
	           System.exit(1);
	        }
	       if (controller.getOutputMode().equals("verbose"))
	    	   System.out.println("Client: Packet sent.");
	
	       // Construct a DatagramPacket for receiving packets up
	       // to 100 bytes long (the length of the byte array).
	
	       i++;
	        
	       System.out.println();
       }
       System.out.println("File Transfer Complete");
   }

   public static void main(String args[]){
	   TFTPClient client = new TFTPClient();
		controller = new Controller(client);
		controller.start();
   }
}