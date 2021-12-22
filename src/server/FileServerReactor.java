package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

public class FileServerReactor implements Runnable{
    Selector selector;
    ServerSocketChannel serverSocketChannel;

    FileServerReactor(){
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(59603));
            selector = Selector.open();

            var selectionKey =serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            selectionKey.attach(new AcceptHandler());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        while(!Thread.interrupted()){
            try {
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Set<SelectionKey> selected = selector.selectedKeys();
            Iterator<SelectionKey> it = selected.iterator();
            while(it.hasNext()){
                var key = it.next();
                //new Thread((Runnable) key.attachment()).start();
                ((Runnable) key.attachment()).run();
            }
            selected.clear();
        }
    }

    class AcceptHandler implements Runnable{

        @Override
        public void run() {
            try {
                var socketChannel = serverSocketChannel.accept();
                if (socketChannel!=null){
                   new IOHandler(socketChannel,selector);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
