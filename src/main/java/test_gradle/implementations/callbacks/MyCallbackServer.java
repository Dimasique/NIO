package test_gradle.implementations.callbacks;

import test_gradle.interfaces.CallbackServer;
import test_gradle.interfaces.IClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MyCallbackServer<T> implements CallbackServer<T> {
    private List<IClient<T>> clients;
    private BlockingQueue<T> messages;

    public MyCallbackServer(List<IClient<T>> clients, BlockingQueue<T> messages) {
        this.clients = clients;
        this.messages = messages;
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
    }

}
