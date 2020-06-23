package test_gradle;

import test_gradle.implementations.ClientServerSide;
import test_gradle.interfaces.CallbackServer;
import test_gradle.interfaces.IClient;
import test_gradle.interfaces.IDecoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMessageHandler<T> {
    private boolean running;
    private IDecoder<T> decoder;
    private Selector selector;
    private CallbackServer<T> callback;

    private final Map<SocketChannel, IClient<T>> clients = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ByteBuffer> buffers = new ConcurrentHashMap<>();

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
    }

    public Selector getSelector() {
        return selector;
    }

    public void start() {
        running = true;

        new Thread(() -> {
            try {
                while (running) {
                    selector.select();

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> i = selectedKeys.iterator();

                    while (i.hasNext()) {
                        SelectionKey key = i.next();
                        SocketChannel socketChannel = (SocketChannel)key.channel();

                        ByteBuffer buffer = buffers.get(socketChannel);
                        IClient<T> client = clients.get(socketChannel);

                        if (key.isConnectable()) {
                            socketChannel.finishConnect();
                        } else if (key.isReadable()) {

                            if (!((ClientServerSide<T>)client).isReadingPreviousMessage()) {
                                buffer.clear();
                            }

                            int receiveData = socketChannel.read(buffer);
                            if (receiveData == 0 || receiveData == -1) {
                                client.close();
                                callback.onQuitClient(client);
                                clients.remove(socketChannel);
                                break;
                            }

                            int indexBegin = 0;
                            Pair<T, Integer> decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            while(decoded != null && indexBegin < buffer.capacity()) {
                                callback.onMessageReceive(decoded.getKey(), client);
                                indexBegin = decoded.getValue() + 1;
                                decoded = decoder.decode(buffer, indexBegin, buffer.position());
                            }

                            shiftBuffer(buffer, indexBegin);

                            //key.interestOps(SelectionKey.OP_WRITE);
                        } else if (key.isWritable()) {
                            T line = ((ClientServerSide<T>)client).poll();
                            if (line != null) {
                                socketChannel.write(decoder.encode(line));

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

    public void close() {
        running = false;
        try {
            for (SocketChannel channel : clients.keySet()) {
                channel.close();
            }
        }
        catch (IOException e) {
            callback.onException(e);
        }
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
