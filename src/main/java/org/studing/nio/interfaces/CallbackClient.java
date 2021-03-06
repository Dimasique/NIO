package org.studing.nio.interfaces;

public interface CallbackClient<T> {

    void onMessageReceive(T message);

    void onException(Exception e);

    void onDisconnect();
}
