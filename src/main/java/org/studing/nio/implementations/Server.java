package org.studing.nio.implementations;

import org.apache.logging.log4j.Level;
import org.studing.nio.ServerMessageHandler;
import org.studing.nio.factories.CallbackFactory;
import org.studing.nio.factories.DecoderFactory;
import org.studing.nio.implementations.clients.ClientServerSide;
import org.studing.nio.interfaces.CallbackServer;
import org.studing.nio.interfaces.IServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Server<T> implements IServer<T> {

    private final static Logger log = LogManager.getLogger(Server.class);
    private Selector selector;
    private ServerSocketChannel mySocket;
    private final AtomicBoolean running = new AtomicBoolean();
    private final CallbackServer<T> callback;
    private CallbackFactory<T> callbackFactory;
    private DecoderFactory<T> decoderFactory;
    private ServerMessageHandler<T> serverMessageHandler;
    private Thread thread;

    public Server(String IP, int port, DecoderFactory<T> decoderFactory,
                  CallbackFactory<T> callbackFactory) {

        this.decoderFactory = decoderFactory;
        this.callbackFactory = callbackFactory;
        this.callback = callbackFactory.getCallback();

        try {
            InetAddress hostIP = InetAddress.getByName(IP);

            selector = Selector.open();
            mySocket = ServerSocketChannel.open();
            InetSocketAddress address = new InetSocketAddress(hostIP, port);
            mySocket.socket().bind(address);

            mySocket.configureBlocking(false);
            mySocket.register(selector, SelectionKey.OP_ACCEPT);
        }
        catch (IOException e) {
            callback.onException(e);
        }
    }

    @Override
    public void start() {
        if (running.get()) {
            callback.onException(new Exception("Server has already started"));
        }

        serverMessageHandler =
                new ServerMessageHandler<>(decoderFactory.getDecoder(), callback);
        serverMessageHandler.start();

        log.log(Level.INFO, "server started");

        thread = new Thread(() -> {
            try {
                running.set(true);
                while (running.get()) {

                    selector.select();
                    if (!running.get()) {
                        break;
                    }
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> i = selectedKeys.iterator();

                    while (i.hasNext()) {
                        SelectionKey key = i.next();

                        if (key.isAcceptable()) {
                            log.info("accepting new client");
                            processAcceptEvent(mySocket, key);
                        }
                        i.remove();
                    }
                }
            }
            catch (Exception e) {
                callback.onException(e);
            }
        });
        thread.start();
    }

    @Override
    public void close() {
        try {
            log.log(Level.INFO, "server is closing...");
            running.set(false);
            selector.wakeup();

            try {
                thread.join();
            } catch (InterruptedException e) {
                callback.onException(e);
            }

            serverMessageHandler.close();
            mySocket.socket().close();
            mySocket.close();
        } catch (IOException e) {
            callback.onException(e);
        }

    }

    private void processAcceptEvent(ServerSocketChannel mySocket, SelectionKey key)
            throws IOException{

        SocketChannel newChannel = mySocket.accept();

        ClientServerSide<T> newClient =
                new ClientServerSide<>(decoderFactory.getDecoder(), callbackFactory.getCallback());

        newClient.setChannel(newChannel);
        newClient.setSelector(serverMessageHandler.getSelector());
        serverMessageHandler.addClient(newChannel, newClient);
        newClient.start();

        log.info("new client ran");
        callback.onNewClient(newClient);
    }

    public void setCallbackFactory(CallbackFactory<T> callbackFactory) {
        this.callbackFactory = callbackFactory;
    }

    public void setDecoderFactory(DecoderFactory<T> decoderFactory) {
        this.decoderFactory = decoderFactory;
    }

}
