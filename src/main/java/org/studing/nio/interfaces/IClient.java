package org.studing.nio.interfaces;

import java.io.IOException;

public interface IClient<T> {

    void connect(String IP, int port) throws IOException;

    void close() throws IOException;

    void send(T message) throws InterruptedException;

    void start();
}
