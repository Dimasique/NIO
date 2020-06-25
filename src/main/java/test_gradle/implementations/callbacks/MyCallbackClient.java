package test_gradle.implementations.callbacks;

import test_gradle.interfaces.CallbackClient;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class MyCallbackClient<T> implements CallbackClient<T> {

    private final BlockingQueue<T> queue = new ArrayBlockingQueue<T>(3);

    @Override
    public void onMessageReceive(T message) {
        try {
            queue.put(message);
        } catch (InterruptedException e) {
            onException(e);
        }
    }

    @Override
    public void onException(Exception e) {


    }

    @Override
    public void onDisconnect() {

    }

    public T poll() {
        try {
            return queue.poll(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            onException(e);
        }

        return null;
    }
}
