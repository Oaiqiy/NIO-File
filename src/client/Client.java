package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);
        SocketChannel socketChannel;


        try {
             socketChannel = SocketChannel.open();
             socketChannel.connect(new InetSocketAddress(59603));
        } catch (IOException e) {
            System.out.println("Please check network connection!");
            return;
        }

        System.out.println("----Welcome to NIO File Transfer Tool---");
        System.out.println("-------Please select working mode-------");
        System.out.println("1. Sender.");
        System.out.println("2. Receiver.  ------  please enter (s/r)");
        String mode = scanner.nextLine();
        if(mode.charAt(0)=='s' || mode.charAt(0)=='S'){
            new SendMode(socketChannel).run();
        }else if(mode.charAt(0)=='r' || mode.charAt(0)=='R'){
            new ReceiveMode(socketChannel).run();
        }

        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
