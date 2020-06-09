
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class DnsClient {
	//private static int headLength = 12;
	private static String QTYPE;
	//private static int questionLength; 
	private static int timeout; 
	private static int retries;
	private static int port;
	
	//private static int answerLength = 180;
	private static boolean print = false;
	private static double totalTime;	
	private static String dnsString;
	private static String link;
	private static String errors = "";
	private static byte[] sendData = new byte[1024];
	private static byte[] receiveData = new byte[1024];
	private static boolean dnsSpecified = false;
	private static boolean argsChecked = false;
	private static boolean error = false;
	private static int numOfAns;
	private static short ID;
	private static int auth; 
	private static int numOfAdd;

	
	
	public static void main(String args[]) throws IOException {
		readInput(args);
		//print out error if there is error and exit the system
		if(error) {
			System.out.println(errors);
			System.exit(0);
			}
		build();
		
		int[] buffer = new int[4];
		byte [] dnsByteAddress =new byte[4];
		
		try {
			String dnsAddress[] = dnsString.split("\\.");	
			
			for(int i = 0; i < dnsAddress.length; i++) {
			
			    buffer[i] = Integer.valueOf(dnsAddress[i]);
			    dnsByteAddress[i]= (byte)buffer[i];	
			}
		
		}
		catch(Exception nameE){
			 System.out.println("ERROR\t Incorrect input syntax: [Incorrect DNS server IP input]");
			 System.exit(0);
		    			
		}
		System.out.println("DnsClient sending request for " + link + "\n" + "Server: " + dnsString + "\nRequest Type: " + QTYPE);
		System.out.println("Timeout: " +timeout+"\n" + "Retries: " + retries + "\n" + "Port: " +port + "\n");
		try {
			InetAddress IP = InetAddress.getByAddress(dnsByteAddress); 
			DatagramSocket clientSocket = new DatagramSocket();
	        DatagramPacket requestPacket = new DatagramPacket(sendData, sendData.length, IP, port);
	        DatagramPacket recivePacket = new DatagramPacket(receiveData, receiveData.length); // create response packet
	
	        clientSocket.setSoTimeout(timeout); // set the time out in mSec
	        clientSocket.send(requestPacket); // send the request
	        
			int numberOfRetries = 0;
			while(true){
				try {
					long startTime = System.currentTimeMillis();
					clientSocket.receive(recivePacket);
					double endTime = System.currentTimeMillis();
					totalTime = (endTime - startTime)/1000.0;
	        		readResponse(receiveData, totalTime, retries);	
	        		clientSocket.close(); // closed socket after received packet
					}
				catch(SocketTimeoutException e) {// timeout exception.
		    		numberOfRetries++; // add retry counter
		    		System.out.println("\n" + numberOfRetries + " Times Timeout reached! " + "Resending packet!");
		    		clientSocket.send(requestPacket); // resend request
		    		if(numberOfRetries == retries) {
		    			System.out.println("ERROR\t"+"Maximum number of retries ["+retries+"] exceeded");
		    			clientSocket.close();
		    			System.exit(0); //Exit the program when Error occurs 
		    		}
				}
			}
		}catch(SocketException e1){
			// catch the error message when socket closed after the transmission
			System.out.println("");
			
		} catch(Exception e2) {
			// for other exceptions, print it out
			e2.printStackTrace();
		}
	}
	
	static void readInput(String args[]) throws IOException{
		int dnsLength = 0;
		int dnsLocation = 0;

		timeout = 5*1000;
		retries = 3;
		port = 53;
		QTYPE = "A"; // default values for timeout, retries, port and Qtype;
		int numOfElementReadIn = 0;
		link = args[args.length - 1];

		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("-t")) {
				if(args[i+1].matches("[0-9]+")) { 
					// if -t is specified, add 2 into elementsUsed, 
					//parse the int value into timeout
					numOfElementReadIn += 2; 
					timeout = Integer.parseInt(args[i+1])*1000;
				}
				else {
					errors += "\nError\tIncorrect input syntax: timeout invalid";
					error = true;
				}
			}
			else if(args[i].equals("-r")) {
				// if -r is specified, add 2 into elementsUsed, 
				//parse the int value into retries
				if(args[i+1].matches("[0-9]+")) {
					numOfElementReadIn += 2;
					retries = Integer.parseInt(args[i+1]);
				}
				else {
					//if no number is inputed after -r, error
					errors += "\nError\tIncorrect input syntax: retries invalid";
					error = true;
				}
			}
			else if(args[i].equals("-p")) {
				// if -r is specified, add 2 into elementsUsed, 
				//parse the int value into port
				if(args[i+1].matches("[0-9]+")) {
					numOfElementReadIn += 2;
					port = Integer.parseInt(args[i+1]);
				}
				else {
					//if no number is inputed after -p, error
					errors += "\nError\tIncorrect input syntax: port invalid";
					error = true;
				}
			}
			else if(args[i].equals("-mx") | args[i].equals("-MX")) {
				//if the server type is mx, change Qtype to corresponding type
				numOfElementReadIn++;
				QTYPE = "mx";
			}
			else if(args[i].equals("-ns") | args[i].equals("-NS")) {
				//if the server type is ns, change Qtype to corresponding type
				numOfElementReadIn ++;
				QTYPE = "ns";
			}
			else if(args[i].toCharArray()[0] == '@') {
				//check whether dnsString is input or not, record the location of dnsString if inputed
				numOfElementReadIn ++;
				dnsLength = args[i].toCharArray().length - 1;
				dnsSpecified = true;
				dnsLocation = i;
			} else if (i!= args.length -1 && !args[i].matches("[0-9]+") && !argsChecked) {
				// check the args length to see if extra information is inputed 
				// check only once
				argsChecked = true;
				errors += "\nError\tIncorrect input syntax: arguments invalid";
				error = true;
			}
		}
		
		if(dnsSpecified) {
			// if dnsString is inputed in the command line
			char[] dnsGiven = args[dnsLocation].toCharArray();
			// make a buffer for the dnsString
			char[] dns = new char[dnsLength];
			// get rid of the @
			for(int i = 0; i < dnsLength; i++) {
				dns[i] = dnsGiven[i+1];
			}
			// assign value from buffer to dnsString
			dnsString = String.valueOf(dns);
			if(!validDNS()) {//check if it is valid by number of points
				errors += "\nError\tIncorrect input syntax: dns invalid";
				error = true;
			}
		} else {
			// DNS not specified
			errors += "\nError\tIncorrect input syntax: DNS not specified";
			error = true;
		}
		if(port != 53) {
			// port number is not 53 
			errors += "Warning:\t\tPossible port missmatch";
		}
		// split the dnsString by .
		String[] splited = dnsString.split("\\.");
		int[] dnsSplit = new int[splited.length];
		for(int i = 0; i< splited.length; i++) {
			dnsSplit[i] = Integer.parseInt(splited[i]);
			if(dnsSplit[i] > 255|dnsSplit[i]<0) {
				// dnsString should not greater than 255
				error = true;
				errors += "\nError\tIncorrect input syntax: dns out of range";
				break;
			}
		}
//		
//		details += "\nTimeout:\t" + timeout;
//		details += "\nRetries:\t" + retries;
//		details +="\nPort:\t\t" + port;
//		details += "\nQTYPE:\t\t" + QTYPE;
//		details += "\ndnsString:\t" + dnsString;
//		details += "\nLink:\t\t" + link;
//		
		if(numOfElementReadIn != args.length - 1 && !error) {
			errors += "\nError\tIncorrect input syntax: name not provided";
			error = true;
		}
	}
	private static boolean validDNS() {
		//check valid DNS by number of . 
		char[] dnsArray = dnsString.toCharArray();
		int dotCount = 0;
	    for(int i = 0; i < dnsArray.length; i++) {
	    	if (dnsArray[i] == '0' | dnsArray[i] == '1' | dnsArray[i] == '2' | dnsArray[i] == '3' | dnsArray[i] == '4' | dnsArray[i] == '5' | dnsArray[i] == '6' | dnsArray[i] == '7' | dnsArray[i] == '8' | dnsArray[i] == '9' | dnsArray[i] == '.') {
	    		if(dnsArray[i] == '.'){
	    			dotCount++;
	    		}
	    	}else {
	        	return false;
	    	}
	    }
	    if(dotCount != 3) {
	    		return false;
	    }
	    return true;
	}
	
	public static boolean isValid() {
		return !error;
	}
	
	public static void build() throws IOException  {
		ByteArrayOutputStream temp = new ByteArrayOutputStream();
		DataOutputStream Qtemp = new DataOutputStream(temp);
		
		
		// write the DNS header 
		// according to dnsprimer example
		ID=(short)Math.random();
		// write Response ID
		Qtemp.writeShort(ID);
		//write FLAGS
		Qtemp.writeShort(0x0100);
		//write QDCOUNT
		Qtemp.writeShort(0x0001);
		//write ANCOUNT
		Qtemp.writeShort(0x0000);
		//write NSCOUNT
		Qtemp.writeShort(0x0000);
		//write ARCOUNT
		Qtemp.writeShort(0x0000);
		
		//write DNS Question 
		
		// split name based on .
		try {
			String Serverpart[] = link.split("\\.");
			for(int j = 0; j < Serverpart.length; j++) {
				byte[] Serverbyte = Serverpart[j].getBytes(); 
				// format is in dnsprimer
				// number first then string
				Qtemp.write(Serverbyte.length);
				Qtemp.write(Serverbyte);
		    }
		}
		//catch domain name error
		catch(Exception nameE){
		  System.out.println("ERROR\t Incorrect input syntax: [Incorrect domain name input]");
		  System.exit(0);
		}
	
        // signify the end of domain name
		Qtemp.writeByte(0x00);
		
		// write QTYPE according to Query type
		if(QTYPE.equals("A")) {
			Qtemp.writeShort(0x0001);
		}
		else if(QTYPE.equals("ns")) {
			Qtemp.writeShort(0x0002);
		}
		else if(QTYPE.equals("mx")) {
			Qtemp.writeShort(0x000f);
		}
		
		//QCLASS dnsprimer
		Qtemp.writeShort(0x0001);
		// convert to bytearray in order to send
		sendData = temp.toByteArray();
		
	}
	
	private static void readResponse(byte[] receivePacket, double totalTime, int retries) throws IOException {
		System.out.println("DnsClient sending request for: ["+link+"]");
		System.out.println("Server: ["+dnsString +"]");
		System.out.println("Request type: ["+QTYPE +"]");
		System.out.println("Response received after ["+(((float)totalTime)/1000)+"] seconds ["+retries+"] retries");
		
		DataInputStream reader = new DataInputStream(new ByteArrayInputStream(receivePacket));
		
        headerParser(reader);    // process the header of the response frame

        answerParser(reader);  // process the question section of response frame 
        
        
        System.out.println("Answer has "+numOfAns + "records");
        System.out.println();
        
        if(numOfAns==0) {
        	System.out.println("ERROR\t"+"Not Found");	
        	System.exit(0); //Exit the program when Error occurs 
        	
        }
        while(numOfAns>0){
        	
        	 parseResponseData(reader, receivePacket);  // process the answer record section of the response frame
                numOfAns--;
            
          }
	    
	}
	private static void headerParser(DataInputStream reader) throws IOException {
	    int id =reader.readShort();  
	    if(ID==id) {
	    	int flags = reader.readShort();
	    	
	    	auth = flags&1024;

		    //Process the errors from RCODE
		    if((flags&0x0f)==0){
		    	System.out.println("Response is correct");
		    	System.out.println("");
		    }else {
		    	if((flags&0x0f)==1) {
		    		System.out.println("ERROR\t"+"Unexpected response "+"[Format error]");
		    	}
		    	else if ((flags&0x0f)==2) {
		    		System.out.println("ERROR\t"+"Unexpected response "+"[Server Failure]");
		    	}
				else if ((flags&0x0f)==3) {
					System.out.println("ERROR\t"+"Unexpected response "+"[Name error]");		
				}
				else if ((flags&0x0f)==4) {
					System.out.println("ERROR\t"+"Unexpected response "+"[Not implemented]");
				}
				else if ((flags&0x0f)==5) {
					System.out.println("ERROR\t"+"Unexpected response "+"[Refused]");
				}
		    	System.exit(0);	//exit the program after printing the error message
		    	
		    }
		        
		    // read value of QDCOUNT
		    reader.readShort();
		    
		    // read value of ANCOUNT
		    numOfAns =reader.readShort();
	    
		    // read value of NSCOUNT
		    reader.readShort();
		    
		    // read value of ARCOUNT
		    numOfAdd=reader.readShort();
    }
    else {
    	
    	System.out.println("ERROR\t Packet reception error: [Wrong packet is received]");
		System.exit(0);
    	
	    }
	}
	private static void answerParser(DataInputStream reader) throws IOException {
		
	    int NameLen = 0;
	    // parse the name
        while ((NameLen = reader.readByte()) > 0) {
            for (int i = 0; i < NameLen; i++) {
                reader.readByte();
            }        
        }
        
        reader.readShort();         // read the value of Qtype
        
        reader.readShort();         // read the value of Qclass

	}
	
	private static void parseResponseData(DataInputStream reader,byte[] receivePacket) throws IOException {
		
        reader.readShort(); 			  // read the value of Pointer 
        
        int TYPE=reader.readShort(); 	  // read the value of RType
        
        // a new reader created to read pointer 
        
        String ipString ="";
        String alias="";
        int TTL;
        int pref=0;
    	reader.readShort();//Read class
    	TTL = reader.readInt(); // read ttl
    	
        // Process response for type A
        if (TYPE==1) {  
        	int ipLen=reader.readShort();//Read RDLENGTH
        	
            //read the ip address in a string
        	
            for (int i = 0; i < ipLen; i++ ) {
	              String temp = String.format("%d", (reader.readByte()&0xFF));
	              // read ip address into ipString
	              if(i==ipLen-1) {
	            	  ipString=ipString+temp;
	              }
	              else {
	            	  ipString=ipString+temp+".";  
	              }             
            }
            //construct a authen string
            String authen="";
            if(auth==0) {
              authen = "nonauth";
            }
            else {
              authen = "auth";	
            }    	

        }
        // Process other answer record types
        else {
	    // pref number for mx type rdata		        
        	int len=reader.readShort();//Read RDLENGTH
        	// read pref when it is mx type
            if(TYPE==15) {
              pref = reader.readShort();//read preference number
            }
        	//read name alias which is a string of characters
        	int counter=0;
            while ((len = reader.readByte()) > 0) {
            	counter++;
                byte[] aliasArray = new byte[len];

                for (int i = 0; i < len; i++) {
                    aliasArray[i] = reader.readByte();
                }
                String holder=new String(aliasArray, "UTF-8");
                if(counter==1) {
                	alias=holder;
                }else {
                	alias=alias+"."+holder;
                }
             }

           int inLoop=1;// to see if this is the first time the program is in the below while loop    
           int p = 0;     // first time reading a pointer if there is a pointer in the response           
           while(len<0) { // if byte start with 11, such as 11000000 in binary, is a negative number, hence it is a pointer
        	   
        	   DataInputStream preader = new DataInputStream(new ByteArrayInputStream(receivePacket));	
        	   
        	   if(inLoop == 1) {	            	
	            	p = reader.readByte(); // This p holds the byte position pointer is pointing to
        	   }
        	   inLoop = 0;  
	           // bitwise logic to read 10 digits of pointer value starting from LSB.
	           p = p&0xff;
        	   int pb9=len&1;
		       int pb10=len&2;	        	        
	           p=p+(256*pb9)+(256*pb10);
	    
	    		
	        	// skip to the location we need to read from using a another copy of reader handle
	        	for (int i=0; i<p; i++) {
	        		preader.readByte(); 		
	        	}
	
	        	// Read the string pointer is pointing to
	            while ((len = preader.readByte()) > 0) {
	            	
	            	counter++;
	                byte[] aliasArray = new byte[len];
	
	                for (int i = 0; i < len; i++) {
	                    aliasArray[i] = preader.readByte();
	                }
	                String holder=new String(aliasArray, "UTF-8");
	                if(counter==1) {
	                	alias = alias+holder;
	                }else {
	                	alias = alias+"."+holder;
	                }   
	                
	            }
	            if(len<0) {
	            	p=preader.readByte(); // read the next pointer value if it is a pointer
	            } 
            }
        }
        //construct a authen string
        String authen="";
        if(auth==0) {
          authen = "nonauth";
        }
        else {
          authen = "auth";	
        }
   
        // Print out the processed information from reading the answer record section of the response frame

        if(TYPE==1) {
          	 System.out.println("IP\t["+ipString+"]\t["+TTL+"]\t["+authen+"]");	
        }
        else if(TYPE==2) {
        	System.out.println("NS\t["+alias+"]\t["+TTL+"]\t["+authen+"]");
        }
        else if(TYPE==5) {
        	System.out.println("CNAME\t["+alias+"]\t["+TTL+"]\t["+authen+"]");
        }
        else if(TYPE==15) {
        	
        System.out.println("MX\t["+alias+"]\t["+pref+"]\t["+TTL+"]\t["+authen+"]");

        }
        else {
        	 System.out.println("Response Type Error");
        	 System.exit(0);
        }
        
        
	}
	
}
