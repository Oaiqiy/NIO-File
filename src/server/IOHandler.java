package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;

/**
 * 系统IO核心类，包括全部的IO操作。
 */

public class IOHandler implements Runnable{
    private static HashMap<Integer,IOHandler> codes = new HashMap<>();
    private static HashMap<Integer,IOHandler> codesToReceive = new HashMap<>();
    private static Random random = new Random();

    private Integer code;
    private String fileName;
    private Long size;
    STATUS status = STATUS.INIT;
    private final SocketChannel socketChannel;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(ServerConfig.bufferSize);
    private SelectionKey sk;
    private int sendStatus = 0;


    IOHandler(SocketChannel c, Selector selector)  {

        socketChannel = c;
        try {
            socketChannel.configureBlocking(false);
            sk = socketChannel.register(selector, SelectionKey.OP_READ);
            sk.attach(this);
            selector.wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void run() {


        int length;

        switch (status){

            /**
             * 初始话模式，连接尚未确认发送还是接收。
             */

            case INIT:

                try {
                    byteBuffer.clear();
                    length = socketChannel.read(byteBuffer);
                    byteBuffer.flip();
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        socketChannel.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    return;
                }

                if(length<8){
                    try {
                        socketChannel.close();
                        return;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }


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

                    sk.interestOps(SelectionKey.OP_WRITE);


                }else {
                    status = STATUS.RECEIVE;
                    int temp = byteBuffer.getInt();
                    var ioHandler = codes.get(temp);

                    byteBuffer.clear();

                    if (codes.containsKey(temp)) {

                        codesToReceive.put(temp, this);
                        code = temp;
                        byteBuffer.putInt(1);
                        byteBuffer.putLong(ioHandler.size);
                        byteBuffer.put(ioHandler.fileName.getBytes(StandardCharsets.UTF_8));
                        byteBuffer.flip();

                        codes.get(code).getByteBuffer().clear();
                        codes.get(code).getByteBuffer().put(byteBuffer.array(),0,byteBuffer.limit());
                        codes.get(code).getSk().interestOps(SelectionKey.OP_WRITE);
                        sk.interestOps(SelectionKey.OP_WRITE);

                    } else {
                        byteBuffer.putInt(2);
                        byteBuffer.flip();
                        try {

                            socketChannel.write(byteBuffer);
                            socketChannel.shutdownOutput();
                            socketChannel.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }

                break;

            /**
             * 发送模式
              */
            case SEND:
                if (sk.isWritable()) {

                    if (sendStatus == 0) {
                        try {
                            socketChannel.write(byteBuffer);
                            if (byteBuffer.position() < byteBuffer.limit()) {
                                return;
                            }
                            byteBuffer.clear();
                            sendStatus++;
                            sk.interestOps(0);
                            byteBuffer.clear();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (sendStatus == 1) {
                        try {
                            socketChannel.write(byteBuffer);
                            if (byteBuffer.position() < byteBuffer.limit()) {
                                return;
                            }
                            byteBuffer.clear();
                            sendStatus++;
                            sk.interestOps(SelectionKey.OP_READ);
                            byteBuffer.clear();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        System.out.println(socketChannel.socket().getInetAddress().getHostAddress()+':'+socketChannel.socket().getPort() + "  begin sending");

                    }
                    return;
                }


                try {

                    length = socketChannel.read(byteBuffer);

                } catch (IOException e) {
                    try {
                        socketChannel.close();

                        byteBuffer.flip();
                        var ioHandler = codesToReceive.get(code);
                        ioHandler.getByteBuffer().clear();
                        ioHandler.getByteBuffer().put(byteBuffer.array(),0,byteBuffer.limit());
                        ioHandler.getByteBuffer().flip();
                        ioHandler.getSk().interestOps(SelectionKey.OP_WRITE);
                        sk.cancel();

                        codes.remove(code);
                        codesToReceive.remove(code);



                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                    return;
                }



                if(length>0){

                }else if(length<0){
                    try {

                        byteBuffer.flip();
                        var ioHandler = codesToReceive.get(code);
                        ioHandler.getByteBuffer().clear();
                        ioHandler.getByteBuffer().put(byteBuffer.array(),0,byteBuffer.limit());
                        ioHandler.getByteBuffer().flip();
                        ioHandler.getSk().interestOps(SelectionKey.OP_WRITE);
                        sk.cancel();

                        socketChannel.close();
                        //codesToReceive.get(code).getSocketChannel().shutdownOutput();
                        codesToReceive.remove(code);
                        codes.remove(code);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else{

                    sk.interestOps(0);
                    byteBuffer.flip();
                    var ioHandler = codesToReceive.get(code);
                    ioHandler.getByteBuffer().clear();
                    ioHandler.getByteBuffer().put(byteBuffer.array(),0,byteBuffer.limit());
                    ioHandler.getByteBuffer().flip();
                    ioHandler.getSk().interestOps(SelectionKey.OP_WRITE);
                    byteBuffer.clear();

                }
                break;

            /**
             * 接收模式
              */
            case RECEIVE:


                if(sendStatus==0){
                    try {
                        socketChannel.write(byteBuffer);
                        if(byteBuffer.position()<byteBuffer.limit())
                            return;
                        sk.interestOps(0);
                        sendStatus++;
                        byteBuffer.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    System.out.println(socketChannel.socket().getInetAddress().getHostAddress()+':'+socketChannel.socket().getPort() + "  begin receiving");
                    return;
                }

                sk.interestOps(0);

                try {
                    socketChannel.write(byteBuffer);

                    if(byteBuffer.position()<byteBuffer.limit()){
                        sk.interestOps(SelectionKey.OP_WRITE);
                        return;
                    }


                    if(byteBuffer.limit()<byteBuffer.capacity()){

                        socketChannel.shutdownOutput();

                        sk.cancel();

                    }else {
                        codes.get(code).getSk().interestOps(SelectionKey.OP_READ);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

        }

    }

    enum STATUS{
        INIT,SEND,RECEIVE;
    }




    public Long getSize() {
        return size;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public SelectionKey getSk() {
        return sk;
    }


}
