package br.com.armange.socket.poc;

import java.io.IOException;
import java.net.ServerSocket;

public class Poc {

    public static void main(String[] args) {
        try {
            ServerSocket s = new ServerSocket(4321);
            Thread.sleep(2000);
            System.out.println(s.isClosed());
            ServerSocket s2 = new ServerSocket(4321);
            Thread.sleep(2000);
            s.close();
            s2.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
