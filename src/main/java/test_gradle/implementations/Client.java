package test_gradle.implementations;

import javafx.util.Pair;
import test_gradle.AbstractClient;
import test_gradle.interfaces.CallbackClient;
import test_gradle.interfaces.IDecoder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Client<T> extends AbstractClient<T> {

    private final static Logger log = LogManager.getLogger(test_gradle.implementations.Client.class);
    private CallbackClient<T> callback;

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

                            if (!readingPreviousMessage) {
                                buffer.clear();
                                indexBegin = 0;
                            }
                            int receiveData = client.read(buffer);
                            if (receiveData == 0 || receiveData == -1) {
                                connected = false;
                                callback.onDisconnect();
                                break;
                            }

                            Pair<T, Integer> decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            while(decoded != null && indexBegin < buffer.capacity()) {
                                callback.onMessageReceive(decoded.getKey());
                                indexBegin = decoded.getValue() + 1;
                                decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            }

                            readingPreviousMessage = indexBegin < buffer.position();
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

    public void setCallback(CallbackClient<T> callback) {
        this.callback = callback;
    }

}