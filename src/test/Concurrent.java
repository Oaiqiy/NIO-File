package test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

/**
 * 并发测试类，注释的代码包括创建文件夹，以及未发送方复制文件操作，共创建32个线程。
 */

public class Concurrent {

    public static void main(String[] args) throws IOException, InterruptedException {

        String fromPath = "F:\\test\\from";
        String toPath = "F:\\test\\to";


//        for(int i=0;i<16;i++){
//            File from = new File(fromPath+i);
//            File to = new File(toPath+i);
//
//            if(from.exists())
//                from.delete();
//            if(to.exists())
//                to.delete();
//
//            from.mkdir();
//            to.mkdir();
//
//        }

//        File file = new File("F:\\FromC\\新建文件夹 (2)\\Captures\\腾讯会议 2020-06-10 10-33-48.mp4");
//        String name = file.getName();
//        FileInputStream fileInputStream = new FileInputStream(file);
//
//        byte[] bytes = fileInputStream.readAllBytes();
//
//        for(int i=0;i<16;i++){
//            FileOutputStream fileOutputStream = new FileOutputStream(fromPath+i+'\\'+name);
//            fileOutputStream.write(bytes);
//        }
//


        SocketChannel[] socketChannels = new SocketChannel[32];
        for(int i=0;i<32;i++){
            socketChannels[i] = SocketChannel.open();
            socketChannels[i].connect(new InetSocketAddress(59603));
        }


        for(int i=0;i<16;i++){
            new Thread(new TestSendMode(socketChannels[i],"F:\\test\\from"+i+"\\腾讯会议 2020-06-10 10-33-48.mp4")).start();
            Thread.sleep(100);
        }

        Thread.sleep(1000);


        for(int i=0;i<16;i++){
            while (TestSendMode.codes.isEmpty()){Thread.sleep(1000);}
            new Thread(new TestReceiveMode(socketChannels[i+16],"F:\\test\\to"+i,TestSendMode.codes.poll())).start();
            System.out.println(i);
            Thread.sleep(100);
        }





    }



}
