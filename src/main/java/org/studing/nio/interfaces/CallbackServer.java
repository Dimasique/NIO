package org.studing.nio.interfaces;

public interface CallbackServer<T> {

    void onMessageReceive(T message, IClient<T> client);

    void onNewClient(IClient<T> client);

    void onQuitClient(IClient<T> client);

    void onException(Exception e);
}
