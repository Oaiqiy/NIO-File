package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Scanner;

public class TestReceiveMode implements Runnable{
    private SocketChannel socketChannel;
    private String pathString;
    private int code;



    TestReceiveMode(SocketChannel socketChannel,String pathString,int code) {
        this.socketChannel = socketChannel;
        this.pathString = pathString;
        this.code=code;
    }


    @Override
    public void run() {


        String name;
        long size;
        ByteBuffer byteBuffer;
        Path path;

        while (true) {
            System.out.print("Please enter directory path:");

            //String pathString = "F:\\test";
            path = Path.of(pathString);

            File folder = path.toFile();
            if (folder.exists() && folder.isDirectory())
                break;

        }


        while (true) {


            byteBuffer = ByteBuffer.allocate(1024 * 1024 * 128);
            byteBuffer.putInt(1);
            byteBuffer.putInt(code);
            byteBuffer.flip();
            try {
                socketChannel.write(byteBuffer);
                byteBuffer.clear();
                int length = socketChannel.read(byteBuffer);
                byteBuffer.flip();
                byteBuffer.getInt();
                size = byteBuffer.getLong();
                byte[] nameBytes = new byte[length - 12];
                byteBuffer.get(nameBytes);
                name = new String(nameBytes);
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        path = path.resolve(name);

        File file = path.toFile();

        if (file.exists()) {
            file.delete();
        }

        try {
            file.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            FileChannel fileChannel = fileOutputStream.getChannel();
            byteBuffer.clear();




            while (( socketChannel.read(byteBuffer)) > 0) {

                byteBuffer.flip();
                fileChannel.write(byteBuffer);
                byteBuffer.clear();

            }



            fileChannel.force(true);
            fileChannel.close();
            socketChannel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}