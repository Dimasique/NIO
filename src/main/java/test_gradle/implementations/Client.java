package test_gradle.implementations;

import test_gradle.AbstractClient;
import test_gradle.Pair;
import test_gradle.interfaces.CallbackClient;
import test_gradle.interfaces.IDecoder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Client<T> extends AbstractClient<T> {

    private final static Logger log = LogManager.getLogger(test_gradle.implementations.Client.class);
    private CallbackClient<T> callback;
    private boolean connected;

    public Client(IDecoder<T> decoder, CallbackClient<T> callback) throws IOException{
        this.decoder = decoder;
        this.selector = Selector.open();
        this.callback = callback;
    }

    @Override
    public void connect(String IP, int port) throws IOException {
        InetAddress hostIP = InetAddress.getByName(IP);
        myAddress = new InetSocketAddress(hostIP, port);
    }


    @Override
    public void start() {

        try {
            this.socketChannel = SocketChannel.open(myAddress);
            this.socketChannel.configureBlocking(false);
            this.socketChannel.register(selector,
                    SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        } catch (IOException e) {
            callback.onException(e);
        }

        new Thread(() -> {
            try {
                connected = true;
                while (connected) {
                    selector.select();

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> i = selectedKeys.iterator();

                    while (i.hasNext()) {
                        SelectionKey key = i.next();
                        if (key.isConnectable()) {
                            socketChannel.finishConnect();

                        } else if (key.isReadable()) {

                            if (!readingPreviousMessage) {
                                buffer.clear();
                            }

                            int receiveData = socketChannel.read(buffer);
                            //System.out.println(receiveData);
                            if (receiveData == 0 || receiveData == -1) {
                                connected = false;
                                callback.onDisconnect();
                                i.remove();
                                continue;
                            }

                            indexBegin = 0;
                            Pair<T, Integer> decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            while(decoded != null && indexBegin < buffer.capacity()) {
                                callback.onMessageReceive(decoded.getKey());
                                indexBegin = decoded.getValue() + 1;
                                decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            }

                            readingPreviousMessage = indexBegin < buffer.position();
                            shiftBuffer(indexBegin);

                            //key.interestOps(SelectionKey.OP_WRITE);

                        } else if (key.isWritable()) {
                            T line = queue.poll();

                            if (line != null) {
                                socketChannel.write(decoder.encode(line));
                                //System.out.println("sent!");
                            }
                            //key.interestOps(SelectionKey.OP_READ);
                        }
                        i.remove();
                    }
                }
            }
            catch (IOException e) {
                callback.onException(e);
            }
        }).start();
    }

    public void setCallback(CallbackClient<T> callback) {
        this.callback = callback;
    }

    @Override
    public void close(){
        try {
            connected = false;
            socketChannel.close();
        }
        catch (IOException e) {
            callback.onException(e);
        }
    }

    private void shiftBuffer(int end) {
        int index = 0;
        for(int i = end; i < buffer.position(); i++) {
            buffer.put(index++, buffer.get(i));
            buffer.put(i, (byte)0);
        }
        buffer.position(index);
    }

}