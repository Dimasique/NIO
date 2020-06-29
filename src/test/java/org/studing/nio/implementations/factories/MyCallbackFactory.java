package org.studing.nio.implementations.factories;

import org.studing.nio.implementations.callbacks.MyCallbackServer;
import org.studing.nio.interfaces.CallbackServer;
import org.studing.nio.interfaces.IClient;
import org.studing.nio.factories.CallbackFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyCallbackFactory<T> extends CallbackFactory<T> {

    private final List<IClient<T>> clients;
    private final BlockingQueue<T> messages;
    private final AtomicBoolean wasException;

    public MyCallbackFactory (List<IClient<T>> clients, BlockingQueue<T> messages, AtomicBoolean wasException) {
        this.clients = clients;
        this.messages = messages;
        this.wasException = wasException;
    }

    @Override
    public CallbackServer<T> getCallback() {
        return new MyCallbackServer<>(clients, messages, wasException);
    }
}
