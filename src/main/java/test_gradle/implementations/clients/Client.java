package test_gradle.implementations.clients;

import org.apache.logging.log4j.Level;
import test_gradle.AbstractClient;
import test_gradle.Pair;
import test_gradle.interfaces.CallbackClient;
import test_gradle.interfaces.IDecoder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Client<T> extends AbstractClient<T> {

    private final static Logger log = LogManager.getLogger(Client.class);
    private CallbackClient<T> callback;
    private final AtomicBoolean running = new AtomicBoolean();

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
            this.socketChannel = SocketChannel.open();
            this.socketChannel.configureBlocking(false);

            if (!this.socketChannel.connect(myAddress)) {
                this.socketChannel.register(selector,
                        SelectionKey.OP_CONNECT);
                log.log(Level.INFO, "channel is not connected yet");
            }

            else {
                this.socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                log.log(Level.INFO, "channel is connected already");
                connected.set(true);
            }


        } catch (IOException e) {
            callback.onException(e);
        }

        new Thread(() -> {
            try {
                running.set(true);
                while (running.get()) {
                    selector.select();

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> i = selectedKeys.iterator();

                    while (i.hasNext()) {
                        SelectionKey key = i.next();
                        if (key.isConnectable()) {
                            socketChannel.finishConnect();
                            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            connected.set(true);

                            log.info("client is connected now");

                        } else if (key.isReadable()) {

                            //log.info("trying to read message from server");
                            if (!readingPreviousMessage) {
                                buffer.clear();
                            }

                            int receiveData = socketChannel.read(buffer);
                            if (receiveData == 0 || receiveData == -1) {
                                running.set(false);
                                callback.onDisconnect();
                                break;
                            }

                            indexBegin = 0;
                            Pair<T, Integer> decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            while(decoded != null && indexBegin < buffer.capacity()) {
                                callback.onMessageReceive(decoded.getKey());
                                log.info("got message from server: " + decoded.getKey().toString());
                                indexBegin = decoded.getValue() + 1;
                                decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            }

                            readingPreviousMessage = indexBegin < buffer.position();
                            shiftBuffer(indexBegin);

                        } else if (key.isWritable()) {
                            T line = queue.poll();

                            if (line != null) {
                                socketChannel.write(decoder.encode(line));
                                log.info("sent message to server: " + line.toString());
                            }
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
        log.log(Level.INFO, "client is closing...");

        try {
            running.set(false);
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