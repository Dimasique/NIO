/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package org.studing.nio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.studing.nio.factories.CallbackFactory;
import org.studing.nio.factories.DecoderFactory;
import org.studing.nio.implementations.Coder;
import org.studing.nio.implementations.Server;
import org.studing.nio.implementations.callbacks.MyCallbackClient;
import org.studing.nio.implementations.callbacks.MyCallbackServer;
import org.studing.nio.implementations.clients.Client;
import org.studing.nio.implementations.factories.MyCallbackFactory;
import org.studing.nio.implementations.factories.MyDecoderFactory;
import org.studing.nio.interfaces.CallbackClient;
import org.studing.nio.interfaces.IClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class AppTest {

    private static final String IP = "localhost";
    private static int port = 8080;
    private static Server<String> server;
    private static Client<String> client;
    private static MyCallbackClient<String> callbackClient;

    private static List<IClient<String>> clientsOnServer;
    private static BlockingQueue<String> messages;
    private static AtomicBoolean wasException;


    private static final Logger log = LogManager.getLogger(AppTest.class);

    @Before
    public void initialisation() throws Exception{
        //System.setProperty("log4j.configurationFile","log4j2.properties");

        clientsOnServer = Collections.synchronizedList(new ArrayList<>());
        messages = new ArrayBlockingQueue<>(100);
        wasException = new AtomicBoolean(false);

        DecoderFactory<String> decoderFactory = new MyDecoderFactory();
        CallbackFactory<String> callbackFactory = new MyCallbackFactory<>(clientsOnServer, messages, wasException);

        callbackClient = new MyCallbackClient<>();
        client = new Client<>(new Coder(), callbackClient);

        server = new Server<>(IP, port, decoderFactory, callbackFactory);
    }

    @After
    public void terminate() {
        client.close();
        server.close();
        port++;
    }


    @Test
    public void testSendingFromServer() throws Exception {

        server.start();

        client.connect(IP, port);
        client.start();

        Thread.sleep(75);
        String message = "Welcome!";
        clientsOnServer.get(0).send(message);

        String messageReceived = callbackClient.poll();

        assertEquals(message, messageReceived);

        Thread.sleep(75);
        assertFalse(wasException.get());
        assertFalse(callbackClient.getWasException());
    }


    @Test
    public void testSendingFromClient() throws Exception {
        server.start();

        client.connect(IP, port);
        client.start();

        String message = "Welcome!";
        Thread.sleep(50);
        client.send(message);

        String messageReceived = messages.poll(4, TimeUnit.SECONDS);

        Thread.sleep(75);
        assertEquals(message, messageReceived);
        assertFalse(wasException.get());
        assertFalse(callbackClient.getWasException());
    }

    @Test
    public void sendMessageToManyClients() throws Exception {
        server.start();
        client.connect(IP, port);
        client.start();

        int clientCount = 100;
        List<Client<String>> clients = new ArrayList<>();
        List<CallbackClient<String>> callbackClients = new ArrayList<>();

        for(int i = 0; i < clientCount; ++i) {
            CallbackClient<String> callbackClient = new MyCallbackClient<>();
            callbackClients.add(callbackClient);
            Client<String> client = new Client<>(new Coder(), callbackClient);
            clients.add(client);
            client.connect(IP, port);
            client.start();
        }

        Thread.sleep(20);
        String messageSend = "Welcome ";

        for(int i = 1; i < clientsOnServer.size(); ++i) {
            clientsOnServer.get(i).send(messageSend + (i - 1));
        }


        for(int i = 0; i < clientCount; ++i) {
            String messageGot = ((MyCallbackClient<String>)callbackClients.get(i)).poll();
            assertEquals(messageGot, messageSend + i);
        }


        Thread.sleep(75);
        int idx = 0;
        for(IClient<String> client: clients) {
            client.close();
            assertFalse(((MyCallbackClient<String>)callbackClients.get(idx)).getWasException());
        }

        assertFalse(wasException.get());
        assertFalse(callbackClient.getWasException());
    }

    @Test
    public void sendingMessagesFromManyClients() throws Exception{
        server.start();
        client.connect(IP, port);
        client.start();

        int clientCount = 10;
        //List<Client<String>> clients = new ArrayList<>();
        List<MyCallbackClient<String>> callbackClients = new ArrayList<>();

        String messageSend = "Hello";
        for(int i = 0; i < clientCount; ++i) {
            MyCallbackClient<String> callbackClient = new MyCallbackClient<>();
            callbackClients.add(callbackClient);

            Client<String> client = new Client<>(new Coder(), callbackClient);
            client.connect(IP, port);
            client.start();

            Thread.sleep(75);
            client.send(messageSend);
        }

        String messageReceived = messages.poll(3, TimeUnit.SECONDS);
        int count = 0;
        while(messageReceived != null) {
            assertEquals(messageReceived, messageSend);
            ++count;
            messageReceived = messages.poll(3, TimeUnit.SECONDS);
        }
        assertEquals(clientCount, count);

        Thread.sleep(75);
        for(MyCallbackClient<String> callbackClient : callbackClients) {
            assertFalse(callbackClient.getWasException());
        }
    }
}
