package test_gradle.implementations;

import javafx.util.Pair;
import test_gradle.AbstractClient;
import test_gradle.interfaces.CallbackClient;
import test_gradle.interfaces.CallbackServer;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ClientServerSide<T> extends AbstractClient<T> {

    private final static Logger log = LogManager.getLogger(test_gradle.implementations.Client.class);
    private CallbackServer<T> callback;

    public ClientServerSide(IDecoder<T> decoder, CallbackServer<T> callback) {
        try {
            this.decoder = decoder;
            this.selector = Selector.open();
            this.callback = callback;
        }
        catch (IOException e){
            callback.onException(e);
        }
    }

    public void setChannel(SocketChannel channel) {
        this.client = channel;
    }

    @Override
    public void registration() {
        try {
            this.client.configureBlocking(false);
            this.client.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
        catch (IOException e) {
            callback.onException(e);
        }
    }

    @Override
    public void connect(String IP, int port) throws IOException {
        throw new IOException("This client is already connected");
    }

    @Override
    public void start() {

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
                            client.finishConnect();
                        } else if (key.isReadable()) {
                            if (!readingPreviousMessage)
                                buffer.clear();

                            int receiveData = client.read(buffer);
                            if (receiveData == 0 || receiveData == -1) {
                                connected = false;
                                callback.onQuitClient(this);
                                break;
                            }

                            int indexBegin = 0;
                            Pair<T, Integer> decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            while(decoded != null && indexBegin < buffer.capacity()) {
                                callback.onMessageReceive(decoded.getKey(), this);
                                indexBegin = decoded.getValue() + 1;
                                decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            }

                            readingPreviousMessage = indexBegin < buffer.capacity();
                            shiftBuffer(indexBegin);

                            key.interestOps(SelectionKey.OP_WRITE);
                        } else if (key.isWritable()) {
                            T line = queue.poll();
                            if (line != null) {
                                client.write(decoder.encode(line));
                            }
                            key.interestOps(SelectionKey.OP_READ);
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

    public void setCallback(CallbackServer<T> callback) {
        this.callback = callback;
    }
}