package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class TestSendMode implements Runnable{

    private SocketChannel socketChannel;
    private String path;
    public int code;
    public static Queue<Integer> codes = new LinkedList<>();

    public TestSendMode(SocketChannel socketChannel,String path) {
        this.socketChannel = socketChannel;
        this.path=path;
    }



    @Override
    public void run() {
        FileChannel fileChannel;
        long size;
        String name;
        while (true){
            //String path = "F:\\FromC\\新建文件夹 (2)\\Captures\\腾讯会议 2020-06-02 12-59-19.mp4";
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

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024*1024*128);
        byteBuffer.putInt(0);
        byteBuffer.putLong(size);
        byteBuffer.put(name.getBytes(StandardCharsets.UTF_8));
        byteBuffer.flip();
        try {
            socketChannel.write(byteBuffer);
            byteBuffer.clear();
            socketChannel.read(byteBuffer);
            byteBuffer.flip();
          code = byteBuffer.getInt();

          codes.add(code);

            byteBuffer.clear();

            socketChannel.read(byteBuffer);
            byteBuffer.clear();


            while((fileChannel.read(byteBuffer))>0){

                byteBuffer.flip();
                socketChannel.write(byteBuffer);
                byteBuffer.clear();




            }



            socketChannel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}
