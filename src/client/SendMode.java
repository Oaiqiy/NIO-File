package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SendMode implements Runnable{

    private SocketChannel socketChannel;

    public SendMode(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        FileChannel fileChannel;
        long size;
        String name;
        while (true){
            System.out.print("Please enter file path:");
            //String path = scanner.nextLine();
            String path = "D:\\WIN_20200614_08_58_08_Pro.mp4";
            try {
                File file = new File(path);
                if (!file.exists()){
                    System.out.println("Please enter the right path!");
                    continue;
                }
                size = file.length();
                name = file.getName();
                FileInputStream fileInputStream = new FileInputStream(file);
                fileChannel = fileInputStream.getChannel();
                break;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024*1024*512);
        byteBuffer.putInt(0);
        byteBuffer.putLong(size);
        byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
        byteBuffer.flip();
        try {
            socketChannel.write(byteBuffer);
            byteBuffer.clear();
            socketChannel.read(byteBuffer);
            byteBuffer.flip();
            int code = byteBuffer.getInt();
            System.out.println("Verification code:  " + code);
            byteBuffer.clear();

            socketChannel.read(byteBuffer);
            byteBuffer.clear();
            System.out.println("Start sending......");




            int count = 0;
            int l;
            int rate = 0;
            while((l=fileChannel.read(byteBuffer))>0){
                count += l;
                byteBuffer.flip();
                socketChannel.write(byteBuffer);
                byteBuffer.clear();
                int now = (int) (40.0*count/size);
                for(int i = 0;i<now- rate;i++){
                    System.out.print('|');
                }
                rate = now;

            }


            System.out.println("\nEnd of sending. Bye~");
            //socketChannel.shutdownOutput();

            socketChannel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }





}
