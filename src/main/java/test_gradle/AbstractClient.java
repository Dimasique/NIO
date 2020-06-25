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
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractClient<T> implements IClient<T> {
    protected SocketChannel socketChannel;
    protected IDecoder<T> decoder;
    protected Selector selector;
    protected final ByteBuffer buffer = ByteBuffer.allocate(1024);
    protected final BlockingQueue<T> queue = new ArrayBlockingQueue<>(3);
    protected boolean readingPreviousMessage;
    protected int indexBegin;
    protected InetSocketAddress myAddress;
    protected final AtomicBoolean connected = new AtomicBoolean();

    @Override
    public void send(T message) throws InterruptedException {

        if (connected.get()) {
            queue.put(message);
            selector.wakeup();
        }

    }

}
