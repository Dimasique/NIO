package org.studing.nio.implementations.callbacks;

import org.studing.nio.interfaces.CallbackServer;
import org.studing.nio.interfaces.IClient;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyCallbackServer<T> implements CallbackServer<T> {
    private final List<IClient<T>> clients;
    private final BlockingQueue<T> messages;
    private final AtomicBoolean wasException;

    public MyCallbackServer(List<IClient<T>> clients, BlockingQueue<T> messages, AtomicBoolean wasException) {
        this.clients = clients;
        this.messages = messages;
        this.wasException = wasException;
    }

    @Override
    public void onMessageReceive(T message, IClient<T> client) {
        try {
            messages.put(message);
        } catch (InterruptedException e) {
            onException(e);
        }
    }

    @Override
    public void onNewClient(IClient<T> client) {
        clients.add(client);
    }

    @Override
    public void onQuitClient(IClient<T> client) {

    }

    @Override
    public void onException(Exception e) {
        wasException.set(true);
    }

    public boolean getWasException() {
        return wasException.get();
    }

}
