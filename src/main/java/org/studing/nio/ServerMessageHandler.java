package org.studing.nio;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import org.studing.nio.implementations.clients.ClientServerSide;
import org.studing.nio.interfaces.IClient;
import org.studing.nio.interfaces.CallbackServer;
import org.studing.nio.interfaces.IDecoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMessageHandler<T> {
    private final AtomicBoolean running = new AtomicBoolean();
    private IDecoder<T> decoder;
    private Selector selector;
    private CallbackServer<T> callback;
    private Thread thread;

    private final Map<SocketChannel, IClient<T>> clients = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ByteBuffer> buffers = new ConcurrentHashMap<>();

    private final static Logger log = LogManager.getLogger(ServerMessageHandler.class);

    public ServerMessageHandler(IDecoder<T> decoder, CallbackServer<T> callback) {
        try {
            this.decoder = decoder;
            this.callback = callback;
            this.selector = Selector.open();
        }
        catch (IOException e){
            callback.onException(e);
        }
    }

    public void addClient(SocketChannel socketChannel, IClient<T> client) {
        clients.put(socketChannel, client);
        buffers.put(socketChannel, ByteBuffer.allocate(1024));
        selector.wakeup();
    }

    public Selector getSelector() {
        return selector;
    }

    public void start() {
        running.set(true);

        thread = new Thread(() -> {
            try {
                while (running.get()) {
                    selector.select();
                    if (!running.get())
                        break;

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> i = selectedKeys.iterator();

                    while (i.hasNext()) {
                        SelectionKey key = i.next();
                        SocketChannel socketChannel = (SocketChannel) key.channel();

                        ByteBuffer buffer = buffers.get(socketChannel);
                        IClient<T> client = clients.get(socketChannel);

                        if (key.isConnectable()) {
                            socketChannel.finishConnect();

                        } else if (key.isReadable()) {

                            //log.log(Level.INFO, "trying to read message from client...");


                            if (!((ClientServerSide<T>) client).isReadingPreviousMessage()) {
                                buffer.clear();
                            }

                            int receiveData = socketChannel.read(buffer);
                            if (receiveData == 0 || receiveData == -1) {
                                client.close();
                                callback.onQuitClient(client);
                                clients.remove(socketChannel);
                                continue;
                            }

                            int indexBegin = 0;
                            Pair<T, Integer> decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            while (decoded != null && indexBegin < buffer.capacity()) {
                                callback.onMessageReceive(decoded.getKey(), client);
                                log.log(Level.INFO, "got message from client: " + decoded.getKey().toString());
                                indexBegin = decoded.getValue() + 1;
                                decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            }

                            shiftBuffer(buffer, indexBegin);

                            //key.interestOps(SelectionKey.OP_WRITE);
                        } else if (key.isWritable()) {

                            T line = ((ClientServerSide<T>) client).poll();
                            if (line != null) {
                                socketChannel.write(decoder.encode(line));
                                log.info("message sent to client: " + line.toString());
                            }
                            //key.interestOps(SelectionKey.OP_READ);
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

    public void close() {
        running.set(false);

        try {
            for (SocketChannel channel : clients.keySet()) {
                channel.close();
            }
        } catch (IOException e) {
            callback.onException(e);
        }

        selector.wakeup();

        log.log(Level.INFO, "all clients are closed");

        try {
            thread.join();
        } catch (InterruptedException e) {
            callback.onException(e);
        }

        log.log(Level.INFO, "handler is closed");
    }

    private void shiftBuffer(ByteBuffer buffer, int end) {
        int index = 0;
        for(int i = end; i < buffer.position(); i++) {
            buffer.put(index++, buffer.get(i));
            buffer.put(i, (byte)0);
        }
        buffer.position(index);
    }
}
