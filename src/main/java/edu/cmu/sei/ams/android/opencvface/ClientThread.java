package edu.cmu.sei.ams.android.opencvface;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class ClientThread implements Runnable {
    
	private String serverIpAddress = "";
    
    private boolean connected = false;
 
    private Handler handler = new Handler();
 
    public void run() {
        try {
            InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
            Log.v("ClientActivity", "C: Connecting...");
            Socket socket = new Socket("192.168.168.194", 6789);
            connected = true;
            while (connected) {
                try {
                    Log.v("ClientActivity", "C: Sending command.");
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                                .getOutputStream())), true);
                        // WHERE YOU ISSUE THE COMMANDS
                        out.println("Hey Server!");
                        Log.v("ClientActivity", "C: Sent.");
                } catch (Exception e) {
                    Log.v("ClientActivity", "S: Error", e);
                }
            }
            socket.close();
            Log.v("ClientActivity", "C: Closed.");
        } catch (Exception e) {
            Log.v("ClientActivity", "C: Error", e);
            connected = false;
        }
    }
}
