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
    protected SocketChannel socketChannel = null;
    protected IDecoder<T> decoder;
    protected Selector selector;
    protected final ByteBuffer buffer = ByteBuffer.allocate(1024);
    protected final BlockingQueue<T> queue = new ArrayBlockingQueue<>(2);
    protected boolean readingPreviousMessage;
    protected int indexBegin;
    protected InetSocketAddress myAddress;

    @Override
    public void send(T message) {
        queue.add(message);
        SelectionKey key = socketChannel.keyFor(selector);

        key.interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }

}
