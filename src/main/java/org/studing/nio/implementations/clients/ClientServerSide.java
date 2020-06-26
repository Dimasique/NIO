package org.studing.nio.implementations.clients;

import org.studing.nio.AbstractClient;
import org.studing.nio.interfaces.CallbackServer;
import org.studing.nio.interfaces.IDecoder;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ClientServerSide<T> extends AbstractClient<T> {

    private final static Logger log = LogManager.getLogger(Client.class);
    private CallbackServer<T> callback;

    public ClientServerSide(IDecoder<T> decoder, CallbackServer<T> callback) {
        this.decoder = decoder;
        this.callback = callback;
    }

    public void setChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    @Override
    public void connect(String IP, int port) throws IOException {
        callback.onException(new IOException("This client is already connected"));
    }

    @Override
    public void start() {
        try {
            this.socketChannel.configureBlocking(false);
            this.socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            connected.set(true);
        }
        catch (IOException e) {
            callback.onException(e);
        }
    }

    @Override
    public void close(){
        try {
            socketChannel.close();
        }
        catch (IOException e) {
            callback.onException(e);
        }
    }

    public void setCallback(CallbackServer<T> callback) {
        this.callback = callback;
    }

    public T poll() {
        return queue.poll();
    }

    public boolean isReadingPreviousMessage() {
        return readingPreviousMessage;
    }
}