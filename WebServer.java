//Author: Keerthi Chandra

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.lang.*;

public class WebServer {
    public static void main(String[] args) {
        // dummy value that is overwritten below
        int port = 8080;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.out.println("Usage: java WebServer <port> ");
            System.exit(0);
        }

        WebServer serverInstance = new WebServer();
        try{
            serverInstance.start(port);
        }
        catch(IOException ioe){
            System.out.println("TCP Connection Failed!");
            ioe.printStackTrace();
        }
    }

    private void start(int port) throws IOException {
        System.out.println("Starting server on port " + port + "\n");

        ServerSocket initialSocket = new ServerSocket(port);
        Socket newSocket;

        while(true){
            newSocket = initialSocket.accept();
            handleClientSocket(newSocket);
        }      
    }

    
     // Handles requests sent by a client
     
     
    private void handleClientSocket(Socket client) {

        String fullReq = "";
        try{
            BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
            HttpRequest httpReq = new HttpRequest();
            String req = input.readLine();

            while(req != null){

                fullReq += (req + "\n");

                if(req!=null && (req.equals(""))){

                    httpReq.parseRequest(fullReq);
                    System.out.println(httpReq);
                    sendHttpResponse(client,formHttpResponse(httpReq));

                    fullReq = "";

                    String httpVersion = httpReq.getHttpVersion();
                    boolean persistent = true;

                    if(httpVersion.equals("HTTP/1.0"))
                    {
                        persistent = false;
                    }
                    if(persistent){ //version 1.1
                        client.setSoTimeout(2000);
                    }
                    else{ //version 1.0
                        try{
                            client.close();
                        }catch(IOException err){
                            err.printStackTrace();
                        }
                    }
                }
                req = input.readLine();
                if(req == null){
                    input.close();
                    break;
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }    
    }

    
     // Sends a response back to the client
     
    private void sendHttpResponse(Socket client, byte[] response) {

        try{
            OutputStream toClient = client.getOutputStream();
            toClient.write(response);
            toClient.flush();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    
     // Form a response to an HttpRequest
          
    private byte[] formHttpResponse(HttpRequest request) {

        byte[] buffer1 = request.getStatus();
        byte[] buffer2 = request.getWebObject();

        return concatenate(buffer1,buffer2);
    }


    
     // Concatenates 2 byte[] into a single byte[]
          
    private byte[] concatenate(byte[] buffer1, byte[] buffer2) {
        byte[] returnBuffer = new byte[buffer1.length + buffer2.length];
        System.arraycopy(buffer1, 0, returnBuffer, 0, buffer1.length);
        System.arraycopy(buffer2, 0, returnBuffer, buffer1.length, buffer2.length);
        return returnBuffer;
    }
}



class HttpRequest {

    private String rawRequest;
    private byte[] status;
    private byte[] webObject;
    private String strStatus;

    public byte[] getStatus(){
        return status;
    }

    public byte[] getWebObject(){
        return webObject;
    }


    public void parseRequest(String request){

        rawRequest = request;

        String newStr = "";
        for(int i = 0; i < rawRequest.length(); i++){
            if(rawRequest.charAt(i) == '\n'){
                break;
            }
            else{
                newStr += String.valueOf(rawRequest.charAt(i));
            }
        }
        String[] parsedRequest = newStr.split(" ");

        parsedRequest[1] = parsedRequest[1].replace("/","");

        StringBuilder statusLine = new StringBuilder(parsedRequest[2]);
        strStatus = statusLine.toString();
        String statusCode = "200 OK";

        File uri = new File(parsedRequest[1]);

        statusLine.append(" " + statusCode + "\r\n" + "Content-Length:" + " " + uri.length() + "\r\n" +"\r\n");

        status = statusLine.toString().getBytes();

        byte[] bytes;
        try{
            bytes = Files.readAllBytes(Paths.get(parsedRequest[1]));
            webObject = bytes;
        }
        catch(IOException err){
            err.printStackTrace();
        }
    }

    @Override
        public String toString(){
            return rawRequest;
        }

    public String getHttpVersion(){
        return strStatus;
    }
}
