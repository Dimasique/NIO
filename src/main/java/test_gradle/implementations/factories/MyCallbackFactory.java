package test_gradle.implementations.factories;

import test_gradle.factories.CallbackFactory;
import test_gradle.implementations.callbacks.MyCallbackServer;
import test_gradle.interfaces.CallbackServer;
import test_gradle.interfaces.IClient;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MyCallbackFactory<T> extends CallbackFactory<T> {

    private List<IClient<T>> clients;
    private BlockingQueue<T> messages;

    public MyCallbackFactory (List<IClient<T>> clients, BlockingQueue<T> messages) {
        this.clients = clients;
        this.messages = messages;
    }

    @Override
    public CallbackServer<T> getCallback() {
        return new MyCallbackServer<>(clients, messages);
    }
}
