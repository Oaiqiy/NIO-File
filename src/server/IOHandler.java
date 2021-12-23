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
    private String fileName;
    private Long size;
    STATUS status = STATUS.INIT;
    private final SocketChannel socketChannel;
    private SelectionKey selectionKey;
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024*1024*16);

    IOHandler(SocketChannel c, Selector selector)  {
        System.out.println("aaaa");
        socketChannel = c;
        try {
            socketChannel.configureBlocking(false);
            selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(this);
            selector.wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        int length;


//        try {
//            length = socketChannel.read(byteBuffer);
//            byteBuffer.flip();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//
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
                    codesToReceive.put(temp, this);
                    byteBuffer.clear();

                    if (codes.containsKey(temp)) {
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
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }
                byteBuffer.clear();
                break;


            case SEND:
//                if(selectionKey.isWritable())
//                    return;

                try {
                    length = socketChannel.read(byteBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                if(length>0){
                    try {
                        byteBuffer.flip();
                        var sc = codesToReceive.get(code);
                        while (sc.getSocketChannel().write(byteBuffer)<=0){}
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else if(length<0){
                    try {
                        codesToReceive.get(code).getSelectionKey().interestOps(SelectionKey.OP_WRITE);
                        codes.get(code).getSocketChannel().close();
                        codesToReceive.get(code).getSocketChannel().shutdownOutput();
                        codesToReceive.remove(code);
                        codes.remove(code);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else {
//                    codesToReceive.get(code).getSelectionKey().interestOps(SelectionKey.OP_WRITE);
//                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                }
                break;

            case RECEIVE:
//                if (selectionKey.isWritable()) {
//
//                    selectionKey.interestOps(SelectionKey.OP_READ);
//                    byteBuffer.clear();
//                    ByteBuffer temp = codes.get(code).getByteBuffer();
//                    byteBuffer.put(temp.array(),0,temp.position());
//
//                    byteBuffer.flip();
//
//                    try {
//                        socketChannel.write(byteBuffer);
//                        if(byteBuffer.limit()<byteBuffer.capacity()){
//
//                            codesToReceive.remove(code);
//                            codes.remove(code);
//
//                            this.socketChannel.shutdownOutput();
//                            this.socketChannel.close();
//
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    codes.get(code).getByteBuffer().clear();
//                    codes.get(code).getSelectionKey().interestOps(SelectionKey.OP_READ);
//                    byteBuffer.clear();
//
//                }
        }

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

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }
}
