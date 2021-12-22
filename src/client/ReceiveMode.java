package client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Scanner;

public class ReceiveMode implements Runnable{
    private SocketChannel socketChannel;
    ReceiveMode(SocketChannel socketChannel){
        this.socketChannel = socketChannel;
    }

    @Override
    public void run() {

        Scanner scanner = new Scanner(System.in);
        String name;
        long size;
        ByteBuffer byteBuffer;
        Path path;

        while (true){
            System.out.print("Please enter directory path:");
            String pathString = scanner.next();
            path = Path.of(pathString);

            File folder = path.toFile();
            if(folder.exists()&&folder.isDirectory())
                break;
            System.out.println("Please enter right and existed directory path!");
        }


        while (true){
            System.out.print("Please enter download code:");
            int code = scanner.nextInt();
            byteBuffer = ByteBuffer.allocate(1024*1024);
            byteBuffer.putInt(1);
            byteBuffer.putInt(code);
            byteBuffer.flip();
            try {
                socketChannel.write(byteBuffer);
                byteBuffer.clear();
                int length = socketChannel.read(byteBuffer);
                byteBuffer.flip();
                if(byteBuffer.getInt()==2){
                    System.out.println("The code doesn't exist!");
                    continue;
                }
                size = byteBuffer.getLong();
                byte[] nameBytes = new byte[length-12];
                byteBuffer.get(nameBytes);
                name = new String(nameBytes);
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


            path = path.resolve(name);

            File file = path.toFile();
            if(file.exists()){
                file.delete();
            }

            try {
                file.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                FileChannel fileChannel = fileOutputStream.getChannel();
                byteBuffer.clear();
                System.out.println("Start receiving......");
                int l ;
                int count = 0;
                int rate = 0;
                while ((l=socketChannel.read(byteBuffer))>=0){
                    count += l;
                    byteBuffer.flip();
                    fileChannel.write(byteBuffer);
                    byteBuffer.clear();
                    int now = (int) (40.0*count/size);
                    for(int i = 0;i<now- rate;i++){
                        System.out.print('|');
                    }
                    rate = now;
                }

                fileChannel.force(true);

                fileChannel.close();
                socketChannel.close();
                System.out.println("End of receiving. Bye~");
            } catch (IOException e) {
                e.printStackTrace();
            }



    }
}
