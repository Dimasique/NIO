package org.studing.nio.implementations.callbacks;

import org.studing.nio.interfaces.CallbackClient;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyCallbackClient<T> implements CallbackClient<T> {

    private final BlockingQueue<T> queue = new ArrayBlockingQueue<T>(3);
    private final AtomicBoolean wasException = new AtomicBoolean(false);

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
        wasException.set(true);
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

    public boolean getWasException() {
        return wasException.get();
    }
}
