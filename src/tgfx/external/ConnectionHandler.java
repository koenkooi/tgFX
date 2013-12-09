/*
 * Copyright (C) 2013 Synthetos LLC. All Rights reserved.
 * see license.txt for terms.
 */

package tgfx.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;
import org.apache.log4j.Logger;
import tgfx.Main;
import tgfx.SerialDriver;
import tgfx.tinyg.TinygDriver;
import tgfx.tinyg.TinygDriverFactory;
/**
 * The <code> ConnectionHandler</code> class 
 * @author riley, pfarrell
 * Created on Dec 8, 2013 11:10:48 PM
 */

class ConnectionHandler implements Runnable, Observer {

    private boolean disconnect = false;
    public Socket socket;
    /** logger instance */
    private static final Logger aLog = Logger.getLogger(ConnectionHandler.class);
    private final TinygDriver tinygD;
    
    public ConnectionHandler(Socket socket) {
        this.socket = socket;
        tinygD = TinygDriverFactory.getTinygDriver();

       Main.print("[+]Opening Remote Listener Socket");
       aLog.info("[+]Opening Remote Listener Socket");
//        ser.addObserver(this);
       Thread t = new Thread(this);
//        t.start();
    }
    @Override
    public void update(Observable o, Object arg) {

        String[] MSG = (String[]) arg;

        if (MSG[0] == "JSON") {
            final String line = MSG[1];
            try {
                this.write(MSG[1] + "\n");
            } catch (IOException ex) {
                disconnect = true;
            } catch (Exception ex) {
                Main.print("update(): " + ex.getMessage());
            }
        }
    }



    private void write(String l) throws Exception {
        //Method for writing to the socket
        socket.getOutputStream().write(l.getBytes());
    }

    public void run() {
        try {
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            Main.print("GOT: " + stdIn.readLine());
//            try {
//                this.write("[+]Connected to tgFX\n");
//            } catch (Exception ex) {
//            }

            String line = "";
            SerialDriver ser = SerialDriver.getInstance();
            while (ser.isConnected() && !disconnect) {
                try {
                    line = stdIn.readLine() + "\n";
                    tinygD.write(line);
                    Thread.sleep(100);
                } catch (IOException ex) {
                    disconnect = true;
                } catch (Exception ex) {
                    Main.print("run(): " + ex.getMessage());
                    break;
                }
            }
            Main.print("[+]Closing Remote Listener Socket");
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
