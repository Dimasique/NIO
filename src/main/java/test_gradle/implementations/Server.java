package test_gradle.implementations;

import javafx.util.Pair;
import test_gradle.interfaces.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Server<T> implements IServer<T> {

    private final static Logger log = LogManager.getLogger(test_gradle.implementations.Server.class);
    private IDecoder<T> decoder;
    private Selector selector = null;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private Map<String, SocketChannel> map;
    private ServerSocketChannel mySocket;
    private Queue<Pair<SocketChannel, T>> queue = new ArrayDeque<>(2);
    private CallbackServer<T> callback;

    public Server(IDecoder<T> decoder, String IP, int port, CallbackServer<T> callback) {
        this.decoder = decoder;
        this.callback = callback;
        try {
            InetAddress hostIP = InetAddress.getByName(IP);

            selector = Selector.open();
            mySocket = ServerSocketChannel.open();
            InetSocketAddress address = new InetSocketAddress(hostIP, port);
            mySocket.socket().bind(address);

            mySocket.configureBlocking(false);
            mySocket.register(selector, SelectionKey.OP_ACCEPT);

            map = new HashMap<>();
        }
        catch (IOException e) {
            log.error(e.getMessage());
        }

        new Thread(() -> {
            try {
                while (true) {

                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> i = selectedKeys.iterator();

                    while (i.hasNext()) {
                        SelectionKey key = i.next();

                        if (key.isAcceptable()) {
                            processAcceptEvent(mySocket, key);
                        } else if (key.isReadable()) {
                            SocketChannel client = (SocketChannel)key.channel();
                            buffer.clear();
                            client.read(buffer);
                            T data = decoder.decode(Arrays.copyOf(buffer.array(), buffer.position()));
                            if (data != null && !data.toString().trim().isEmpty()) {
                                log.info("got message from client: " + data.toString());
                                T response = callback.callingback(data);

                                queue.add(new Pair<>(client, response));
                                SelectionKey keyTemp = client.keyFor(selector);
                                keyTemp.interestOps(SelectionKey.OP_WRITE);

                            }
                            else {
                                client.close();
                            }

                        } else if(key.isWritable()) {
                            Pair<SocketChannel, T> top = queue.poll();
                            if (top != null) {
                                SocketChannel socketChannel = top.getKey();
                                T message = top.getValue();
                                socketChannel.write(ByteBuffer.wrap(decoder.encode(message)));
                            }
                            key.interestOps(SelectionKey.OP_READ);
                        }
                        i.remove();
                    }
                }
            }
            catch (IOException e) {
                log.error(e.getMessage());
            }
        }).start();
    }

    private void processAcceptEvent(ServerSocketChannel mySocket, SelectionKey key)
            throws IOException{
        SocketChannel myClient = mySocket.accept();
        myClient.configureBlocking(false);
        myClient.register(selector, SelectionKey.OP_READ);
        ByteBuffer myBuffer = ByteBuffer.allocate(1024);
    }

}
