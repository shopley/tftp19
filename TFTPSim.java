// TFTPSim.java
// This class is the beginnings of an error simulator for a simple TFTP server 
// based on UDP/IP. The simulator receives a read or write packet from a client and
// passes it on to the server.  Upon receiving a response, it passes it on to the 
// client.
// One socket (23) is used to receive from the client, and another to send/receive
// from the server.  A new socket is used for each communication back to the client.   

import java.io.*;
import java.net.*; 
import java.util.*;

public class TFTPSim{
   
   // UDP datagram packets and sockets used to send / receive
   private DatagramPacket sendPacket;
   private DatagramPacket receivePacket;
   private DatagramSocket receiveSocket;
   private DatagramSocket sendSocket;
   private DatagramSocket sendReceiveSocket;
   private int opCode;
   private int packetCount;
   public static Controller controller;
   
   public TFTPSim()
   {
      try {
         // Construct a datagram socket and bind it to port 23
         // on the local host machine. This socket will be used to
         // receive UDP Datagram packets from clients.
         receiveSocket = new DatagramSocket(23);
         // Construct a datagram socket and bind it to any available
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets from the server.
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {
         se.printStackTrace();
         System.exit(1);
      }
   }

   public void relayPacket()
   {
      byte[] data;
      
      int clientPort, j=0, len, serverPort=69;
      boolean restart = false; // If Client sends RRQ, after last ACK sent to Server, go back to top
      boolean switchPort = false; // If Client sends WRQ, after last DATA, switchPort back to 69
      
      packetCount = 0; //We start by dealing with the request packet and the next packet is the "first" packet

      for(;;) { // loop forever
    	  if (switchPort) {
    		  serverPort = 69;
    		  switchPort = false;
    	  }
         // Construct a DatagramPacket for receiving packets up
         // to 100 bytes long (the length of the byte array).
         
         data = new byte[516];
         receivePacket = new DatagramPacket(data, data.length);

         System.out.println("Simulator: Waiting for packet.");
         // Block until a datagram packet is received from receiveSocket.
         try {
            receiveSocket.receive(receivePacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }
         len = receivePacket.getLength();
         clientPort = receivePacket.getPort();
         if(packetCount==0) opCode = receivePacket.getData()[1]; //If it is the request packet then grab the operation code
         // Process the received datagram.
         System.out.println("Simulator: Packet received:");
         if (controller.getOutputMode().equals("verbose")){
        	 TFTPReadWrite.printPacket(receivePacket, clientPort, "receive");

        	 // Form a String from the byte array, and print the string.
        	 String received = new String(data,0,len);
        	 System.out.println(received);
         }
         
         // Now pass it on to the server (to port 69)
         // Construct a datagram packet that is to be sent to a specified port
         // on a specified host.
         // The arguments are:
         //  msg - the message contained in the packet (the byte array)
         //  the length we care about - k+1
         //  InetAddress.getLocalHost() - the Internet address of the
         //     destination host.
         //     In this example, we want the destination to be the same as
         //     the source (i.e., we want to run the client and server on the
         //     same computer). InetAddress.getLocalHost() returns the Internet
         //     address of the local host.
         //  69 - the destination port number on the destination host.

         sendPacket = new DatagramPacket(data, len,
                                        receivePacket.getAddress(), serverPort);
        
         System.out.println("Simulator: sending packet.");
         if (controller.getOutputMode().equals("verbose")){
        	 TFTPReadWrite.printPacket(sendPacket, sendPacket.getPort(), "send");
         }

         // Send the datagram packet to the server via the send/receive socket.

         errorSimSend(sendReceiveSocket,sendPacket,data);
         
         // If Client sends DATA < 516, next send to Server is on port 69
         if (receivePacket.getData()[1] == 3 && receivePacket.getLength() < 516) {
        	 switchPort = true;
         }

         // If Server send DATA < 516 and last ACK was transfered, back to the top
         if (restart) {
        	 serverPort = 69;
        	 restart = false;
         } else {
         
        	 // Construct a DatagramPacket for receiving packets up
        	 // to 100 bytes long (the length of the byte array).

        	 data = new byte[516];
        	 receivePacket = new DatagramPacket(data, data.length);

        	 System.out.println("Simulator: Waiting for packet.");
        	 try {
        		 // Block until a datagram is received via sendReceiveSocket.
        		 sendReceiveSocket.receive(receivePacket);
        	 } catch(IOException e) {
        		 e.printStackTrace();
        		 System.exit(1);
        	 }

        	 // After the connection is established, serverPort should be the thread's port
        	 serverPort = receivePacket.getPort();

        	 // Process the received datagram.
        	 System.out.println("Simulator: Packet received:");
        	 if (controller.getOutputMode().equals("verbose")){
        		 TFTPReadWrite.printPacket(receivePacket, serverPort, "receive");
        	 }

             //Check if DATA packet and if <516 bytes, if so, restart is true
             if (receivePacket.getData()[1] == 3 && receivePacket.getLength() < 516) {
            	 restart = true;
             }
        	 
        	 // Construct a datagram packet that is to be sent to a specified port
        	 // on a specified host.
        	 // The arguments are:
        	 //  data - the packet data (a byte array). This is the response.
        	 //  receivePacket.getLength() - the length of the packet data.
        	 //     This is the length of the msg we just created.
        	 //  receivePacket.getAddress() - the Internet address of the
        	 //     destination host. Since we want to send a packet back to the
        	 //     client, we extract the address of the machine where the
        	 //     client is running from the datagram that was sent to us by
        	 //     the client.
        	 //  receivePacket.getPort() - the destination port number on the
        	 //     destination host where the client is running. The client
        	 //     sends and receives datagrams through the same socket/port,
        	 //     so we extract the port that the client used to send us the
        	 //     datagram, and use that as the destination port for the TFTP
        	 //     packet.

        	 sendPacket = new DatagramPacket(data, receivePacket.getLength(),
        			 receivePacket.getAddress(), clientPort);
        	 len = sendPacket.getLength();
        	 System.out.println( "Simulator: Sending packet:");
        	 if (controller.getOutputMode().equals("verbose")){
        		 TFTPReadWrite.printPacket(sendPacket, sendPacket.getPort(), "send");
        	 }

        	 // Send the datagram packet to the client via a new socket.
        	 try {
        		 // Construct a new datagram socket and bind it to any port
        		 // on the local host machine. This socket will be used to
        		 // send UDP Datagram packets.
        		 sendSocket = new DatagramSocket();
        	 } catch (SocketException se) {
        		 se.printStackTrace();
        		 System.exit(1);
        	 }

        	 errorSimSend(sendSocket,sendPacket,data);

        	 if (controller.getOutputMode().equals("verbose")){
        		 System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
        		 System.out.println();
        	 }

        	 System.out.println("This is the "+packetCount+" set of packets dealt with");

        	 packetCount++;

        	 // We're finished with this socket, so close it.
        	 sendSocket.close();
         } // close 'if' that checks if the sim should start back at the top
      } // end of loop

   }
   
   private void errorSimSend(DatagramSocket socket, DatagramPacket packet, byte[] d){
	   if(controller.getTestSituation()==1 && opCode==controller.getAffectedOpcode() && packetCount == controller.getPacketNumber()){
		   System.out.println("Losing packet.");
		   return; //lose packet
	   }else if(controller.getTestSituation()==2 && opCode==controller.getAffectedOpcode()&& packetCount == controller.getPacketNumber()){
		   System.out.println("Delaying packet.");
		   //delay packet
		   try {
			Thread.sleep(controller.getDelayTime());
		   	} catch (InterruptedException e) {
		   		e.printStackTrace();
		   		System.exit(1);
		   	}
		   try {
	            socket.send(packet);
	         } catch (IOException e) {
	            e.printStackTrace();
	            System.exit(1);
	         }   
	   }else if(controller.getTestSituation()==3 && opCode==controller.getAffectedOpcode()&& packetCount == controller.getPacketNumber()){
		   System.out.println("Duplicating packet.");
		   //duplicate packet. Send packet, sleep, then send again.
		   try {
		       	socket.send(packet);
		   	} catch (IOException e) {
		       e.printStackTrace();
		       System.exit(1);
		   	} 
		   try {
			Thread.sleep(controller.getDelayTime());
			} catch (InterruptedException e) {
			   e.printStackTrace();
			   System.exit(1);
			}
		   try {
		       	socket.send(packet);
		   	} catch (IOException e) {
		       e.printStackTrace();
		       System.exit(1);
		   	} 
	   }else{
		   //send packet normally
		   try {
	            socket.send(packet);
	         } catch (IOException e) {
	            e.printStackTrace();
	            System.exit(1);
	         }
	   }
   }

   public static void main( String args[] ){
	   TFTPSim sim = new TFTPSim();
	   controller = new Controller(sim);
	   controller.start();
	   sim.relayPacket();
   }
}