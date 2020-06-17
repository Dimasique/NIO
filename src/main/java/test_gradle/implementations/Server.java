package test_gradle.implementations;

import test_gradle.factories.CallbackFactory;
import test_gradle.factories.DecoderFactory;
import test_gradle.interfaces.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Server<T> implements IServer<T> {

    private final static Logger log = LogManager.getLogger(test_gradle.implementations.Server.class);
    private Selector selector;
    private ServerSocketChannel mySocket;
    private boolean running;
    private CallbackServer<T> callback;
    private CallbackFactory<T> callbackFactory;
    private DecoderFactory<T> decoderFactory;

    public Server(String IP, int port, DecoderFactory<T> decoderFactory,
                  CallbackFactory<T> callbackFactory) throws IOException{

        this.decoderFactory = decoderFactory;
        this.callbackFactory = callbackFactory;
        this.callback = callbackFactory.getCallback();

        InetAddress hostIP = InetAddress.getByName(IP);

        selector = Selector.open();
        mySocket = ServerSocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(hostIP, port);
        mySocket.socket().bind(address);

        mySocket.configureBlocking(false);
        mySocket.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void start() {
        new Thread(() -> {
            try {
                running = true;
                while (running) {

                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> i = selectedKeys.iterator();

                    while (i.hasNext()) {
                        SelectionKey key = i.next();

                        if (key.isAcceptable()) {
                            processAcceptEvent(mySocket, key);
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

    @Override
    public void close() throws IOException{
        running = false;
        selector.close();
        mySocket.close();
    }

    private void processAcceptEvent(ServerSocketChannel mySocket, SelectionKey key)
            throws IOException{

        SocketChannel newChannel = mySocket.accept();

        ClientServerSide<T> newClient =
                new ClientServerSide<>(decoderFactory.getDecoder(), callbackFactory.getCallback());

        newClient.setChannel(newChannel);
        newClient.registration();

        newClient.start();
        callback.onNewClient(newClient);
    }

    public void setCallbackFactory(CallbackFactory<T> callbackFactory) {
        this.callbackFactory = callbackFactory;
    }

    public void setDecoderFactory(DecoderFactory<T> decoderFactory) {
        this.decoderFactory = decoderFactory;
    }
}
