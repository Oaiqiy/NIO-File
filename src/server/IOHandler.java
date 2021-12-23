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
    private static HashMap<Integer,IOHandler> codesToReceive = new HashMap<>();
    private static Random random = new Random();

    private Integer code;

    //private long send;
    //private long re;

    private String fileName;
    private Long size;
    STATUS status = STATUS.INIT;
    private final SocketChannel socketChannel;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(1024*128);
    private SelectionKey sk;


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
            case INIT:

                try {
                    length = socketChannel.read(byteBuffer);
                    byteBuffer.flip();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
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

                    byteBuffer.clear();

                    if (codes.containsKey(temp)) {

                        codesToReceive.put(temp, this);
                        code = temp;
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
                            socketChannel.shutdownOutput();
                            socketChannel.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }
                byteBuffer.clear();
                break;


            case SEND:
                //System.out.println("send");
                try {
                    length = socketChannel.read(byteBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                //send+=length;


                if(length>0){
                    //System.out.println(">0");
//                    try {
//                        var sc = codesToReceive.get(code);
//                        while (sc.write(byteBuffer)<=0){}
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }else if(length<0){
                    try {
//                        System.out.println("send  " + send);

                        byteBuffer.flip();
                        var ioHandler = codesToReceive.get(code);
                        ioHandler.getByteBuffer().clear();
                        ioHandler.getByteBuffer().put(byteBuffer.array(),0,byteBuffer.limit());
                        ioHandler.getByteBuffer().flip();
                        ioHandler.getSk().interestOps(SelectionKey.OP_WRITE);
                        sk.cancel();

                        codes.get(code).getSocketChannel().close();
                        //codesToReceive.get(code).getSocketChannel().shutdownOutput();
                        codesToReceive.remove(code);
                        codes.remove(code);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else{

                    sk.interestOps(0);
                    //System.out.println("=0");
                    byteBuffer.flip();
                    var ioHandler = codesToReceive.get(code);
                    ioHandler.getByteBuffer().clear();
                    ioHandler.getByteBuffer().put(byteBuffer.array(),0,byteBuffer.limit());
                    ioHandler.getByteBuffer().flip();
                    ioHandler.getSk().interestOps(SelectionKey.OP_WRITE);
                    byteBuffer.clear();

                }
                break;

            case RECEIVE:
                //System.out.println("receive");
                sk.interestOps(0);

                //re += byteBuffer.limit();
                //byteBuffer.rewind();

                try {

//                    while (socketChannel.write(byteBuffer)<=0||byteBuffer.position()<byteBuffer.limit());
                    socketChannel.write(byteBuffer);

                    if(byteBuffer.position()<byteBuffer.limit()){
                        sk.interestOps(SelectionKey.OP_WRITE);
                        return;
                    }


//                    System.out.println(byteBuffer.position() + "   " + byteBuffer.limit());
                    if(byteBuffer.limit()<byteBuffer.capacity()){

                        socketChannel.shutdownOutput();
//                        System.out.println("receive   " + re);
                        sk.cancel();

                    }else {

                        codes.get(code).getSk().interestOps(SelectionKey.OP_READ);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

        }

        //System.gc();
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

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public SelectionKey getSk() {
        return sk;
    }


}
