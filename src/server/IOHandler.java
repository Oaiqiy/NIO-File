package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;

public class IOHandler implements Runnable{
    private static HashMap<Integer,IOHandler> codes = new HashMap<>();
    private static HashMap<Integer,SocketChannel> codesToReceive = new HashMap<>();
    private static Random random = new Random();

    private Integer code;

    private int count = 0;

    private String fileName;
    private Long size;

    STATUS status = STATUS.INIT;
    private final SocketChannel socketChannel;

    IOHandler(SocketChannel c, Selector selector)  {



        socketChannel = c;
        try {
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ).attach(this);
            selector.wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024*1024*128);

        int length;
        try {
            length = socketChannel.read(byteBuffer);
            byteBuffer.flip();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        switch (status){
            case INIT:
                if(byteBuffer.getInt()==0){
                   status = STATUS.SEND;
                   size = byteBuffer.getLong();
                   byte[] nameBytes = new byte[length-8-4];
                   byteBuffer.get(nameBytes);
                   fileName = new String(nameBytes);
                   code = random.nextInt(1000000);
                   while (codes.containsKey(code))
                       code = random.nextInt(1000000);

                   codes.put(code,this);
                   byteBuffer.clear();
                   byteBuffer.putInt(code);
                   byteBuffer.flip();
                   try {
                        socketChannel.write(byteBuffer);
                   } catch (IOException e) {
                        e.printStackTrace();
                   }
                   byteBuffer.clear();

                }else {
                    status = STATUS.RECEIVE;
                    int temp = byteBuffer.getInt();
                    var ioHandler = codes.get(temp);
                    codesToReceive.put(temp, socketChannel);
                    byteBuffer.clear();

                    if (codes.containsKey(temp)) {
                        byteBuffer.putInt(1);
                        byteBuffer.putLong(ioHandler.size);
                        byteBuffer.put(ioHandler.fileName.getBytes(StandardCharsets.UTF_8));
                        byteBuffer.flip();
                        try {
                            ioHandler.getSocketChannel().write(byteBuffer);
                            byteBuffer.rewind();
                            socketChannel.write(byteBuffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        byteBuffer.putInt(2);
                        byteBuffer.flip();
                        try {
                            socketChannel.write(byteBuffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }
                break;


            case SEND:
                if(length>0){
                    try {
                        count+=length;
                        var sc = codesToReceive.get(code);
                        while (sc.write(byteBuffer)<=0){}
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else if(length<0){
                    try {
                        System.out.println(count);
                        codes.get(code).getSocketChannel().close();
                        codesToReceive.get(code).shutdownOutput();
                        codesToReceive.remove(code);
                        codes.remove(code);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            case RECEIVE:
        }
        byteBuffer.clear();
    }

    enum STATUS{
        INIT,SEND,RECEIVE;
    }




    public String getFileName() {
        return fileName;
    }

    public Long getSize() {
        return size;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }
}
