package test_gradle.implementations;

import test_gradle.interfaces.CallbackClient;
import test_gradle.interfaces.IClient;
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

public class Client<T> implements IClient<T> {

    private final static Logger log = LogManager.getLogger(test_gradle.implementations.Client.class);
    private SocketChannel client = null;
    private IDecoder<T> decoder;
    private Selector selector = null;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final Queue<T> queue = new ArrayDeque<>(2);
    private CallbackClient<T> callback;

    public Client(IDecoder<T> decoder, CallbackClient<T> callback) {
        try {
            this.decoder = decoder;
            this.selector = Selector.open();
            this.callback = callback;
        }
        catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void connect(String IP, int port) {
        try {
            InetAddress hostIP = InetAddress.getByName(IP);
            InetSocketAddress myAddress =
                    new InetSocketAddress(hostIP, port);

            log.info(myAddress.toString());

            this.client = SocketChannel.open(myAddress);
            this.client.configureBlocking(false);
            this.client.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            this.client.configureBlocking(false);

            new Thread(() -> {
                try {
                    while (true) {
                        selector.select();

                        Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        Iterator<SelectionKey> i = selectedKeys.iterator();

                        while (i.hasNext()) {
                            SelectionKey key = i.next();

                            if (key.isConnectable()) {
                                client.finishConnect();
                            } else if (key.isReadable()) {
                                buffer.clear();
                                client.read(buffer);
                                T data = decoder.decode(Arrays.copyOf(buffer.array(), buffer.position()));
                                if (data != null && !data.toString().trim().isEmpty()) {
                                    log.info("Received from server: " + data.toString());
                                    callback.callingback(data);
                                }
                                else {
                                    log.error("Server is not available now. Closing client...");
                                    System.exit(0);
                                }

                                key.interestOps(SelectionKey.OP_WRITE);
                            } else if (key.isWritable()) {
                                T line = queue.poll();
                                if (line != null) {
                                    client.write(ByteBuffer.wrap(decoder.encode(line)));
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
        catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            client.close();
        }
        catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void send(T message) {
        queue.add(message);
        SelectionKey key = client.keyFor(selector);
        key.interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    public void setCallback(CallbackClient<T> callback) {
        this.callback = callback;
    }
}