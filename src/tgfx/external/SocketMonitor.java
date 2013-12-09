/*
 * tgFX Socket Monitor Class
 * Copyright Synthetos.com
 */
package tgfx.external;

import tgfx.SerialDriver;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import tgfx.Main;

/**
 *
 * @author ril3y
 */
public class SocketMonitor {

    private SerialDriver ser = SerialDriver.getInstance();
    private int LISTENER_PORT;
    private ServerSocket server;
    private int clientCount = 0;
    
    public SocketMonitor(ServerSocket server) {
        this.server = server;
    }
    public SocketMonitor(String tmpport) {
        LISTENER_PORT = Integer.parseInt(tmpport);
        this.initServer();
        this.handleConnections();
    }

    int countClientConnections() {
        return (clientCount);
    }

    boolean initServer() {
        try {
            server = new ServerSocket(LISTENER_PORT);
            return (true);
        } catch (IOException e) {
            Main.print("Could not listen on port: " + String.valueOf(LISTENER_PORT));
            return (false);
        }
    }

    public void handleConnections() {
        Main.print("[+]Remote Monitor Listening for Connections....");
//        while (ser.isConnected()) {
            try {
                final Socket socket = server.accept();
            ConnectionHandler connectionHandler = new ConnectionHandler(socket);
            } catch (IOException ex) {
                Main.print("[!]Error: " + ex.getMessage());
            }
//        }
        Main.print("[!]Socket Monitor Terminated...");

    }


}
