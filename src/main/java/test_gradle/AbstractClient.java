package test_gradle;

import test_gradle.interfaces.IClient;
import test_gradle.interfaces.IDecoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class AbstractClient<T> implements IClient<T> {
    protected SocketChannel client = null;
    protected IDecoder<T> decoder;
    protected Selector selector;
    protected final ByteBuffer buffer = ByteBuffer.allocate(1024);
    protected final BlockingQueue<T> queue = new ArrayBlockingQueue<>(2);
    protected boolean readingPreviousMessage;

    protected boolean connected;
    protected InetSocketAddress myAddress;

    @Override
    public void registration() throws IOException{
        this.client = SocketChannel.open(myAddress);
        this.client.configureBlocking(false);
        this.client.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    public void close() throws IOException{
        client.close();
        connected = false;
    }

    public void send(T message) {
        queue.add(message);
        SelectionKey key = client.keyFor(selector);
        key.interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    protected void shiftBuffer(int end) {
        int index = 0;
        for(int i = end; i < buffer.position(); i++) {
            buffer.put(index++, buffer.get(i));
            buffer.put(i, (byte)0);
        }
        buffer.position(index);
    }
}
