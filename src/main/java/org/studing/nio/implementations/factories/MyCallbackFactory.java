package org.studing.nio.implementations.factories;

import org.studing.nio.implementations.callbacks.MyCallbackServer;
import org.studing.nio.interfaces.CallbackServer;
import org.studing.nio.interfaces.IClient;
import org.studing.nio.factories.CallbackFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MyCallbackFactory<T> extends CallbackFactory<T> {

    private final List<IClient<T>> clients;
    private final BlockingQueue<T> messages;

    public MyCallbackFactory (List<IClient<T>> clients, BlockingQueue<T> messages) {
        this.clients = clients;
        this.messages = messages;
    }

    @Override
    public CallbackServer<T> getCallback() {
        return new MyCallbackServer<>(clients, messages);
    }
}
