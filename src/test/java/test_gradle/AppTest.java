/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package test_gradle;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import test_gradle.factories.CallbackFactory;
import test_gradle.factories.DecoderFactory;
import test_gradle.implementations.Client;
import test_gradle.implementations.Coder;
import test_gradle.implementations.Server;
import test_gradle.interfaces.CallbackClient;
import test_gradle.interfaces.CallbackServer;
import test_gradle.interfaces.IClient;
import test_gradle.interfaces.IDecoder;

import java.io.IOException;


public class AppTest {

    private static final String IP = "localhost";
    private static final int PORT = 9999;

    public static String messageFrom = "";
    public static String messageGot = "";

    private static final Logger log = LogManager.getLogger(AppTest.class);


    @Test
    public void testSendingFromClient() throws Exception {
        System.setProperty("log4j.configurationFile","log4j2.properties");
        messageFrom = "Hello!";

        DecoderFactory<String> decoderFactory = new DecoderFactory<>() {

            @Override
            public IDecoder<String> getDecoder() {
                return new Coder();
            }
        };
        CallbackFactory<String> callbackFactory = new CallbackFactory<>() {
            @Override
            public CallbackServer<String> getCallback() {
                return new CallbackServer<>() {

                    @Override
                    public void onMessageReceive(String message, IClient<String> client) {
                        messageGot = message;
                    }

                    @Override
                    public void onNewClient(IClient<String> client) {
                        //client.send("Welcome!");
                    }

                    @Override
                    public void onQuitClient(IClient<String> client) {
                        assert false;
                    }

                    @Override
                    public void onException(Exception e) {
                        assert false;
                    }
                };
            }
        };

        Server<String> server = new Server<>(IP, PORT, decoderFactory, callbackFactory);
        server.start();


        Client<String> client = new Client<>(new Coder(), new CallbackClient<>() {
            @Override
            public void onMessageReceive(String message) {
                assert false;
            }

            @Override
            public void onException(Exception e) {
                assert false;
            }

            @Override
            public void onDisconnect() {
                assert false;
            }
        });

        client.connect(IP, PORT);
        client.start();

        Thread.sleep(750);
        client.send(messageFrom);
        Thread.sleep(3000);

        //assert messageFrom.equals(messageGot);
    }

    @Test
    public void testSendingFromServer() throws Exception {
        messageFrom = "Welcome!";
        messageGot = "";
        System.setProperty("log4j.configurationFile","log4j2.properties");

        try {
            DecoderFactory<String> decoderFactory = new DecoderFactory<>() {

                @Override
                public IDecoder<String> getDecoder() {
                    return new Coder();
                }
            };
            CallbackFactory<String> callbackFactory = new CallbackFactory<>() {
                @Override
                public CallbackServer<String> getCallback() {
                    return new CallbackServer<>() {

                        @Override
                        public void onMessageReceive(String message, IClient<String> client) {
                            try {
                                log.log(Level.INFO, "got message from client: " + message + ", echoing it back");
                                client.send(message);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onNewClient(IClient<String> client) {
                            try {
                                client.send("Hello!");
                                client.send("How are you?");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onQuitClient(IClient<String> client) {
                            assert false;
                        }

                        @Override
                        public void onException(Exception e) {
                            assert false;
                        }
                    };
                }
            };

            Server<String> server = new Server<>(IP, PORT, decoderFactory, callbackFactory);
            server.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        Client<String> client = new Client<>(new Coder(), new CallbackClient<>() {
            @Override
            public void onMessageReceive(String message) {
                //messageGot = message;
                log.log(Level.INFO, "got message from server: " + message);
            }

            @Override
            public void onException(Exception e) {
                assert false;
            }

            @Override
            public void onDisconnect() {
                assert false;
            }
        });

        client.connect(IP, PORT);
        client.start();


        String[] messages = {"aaaaaaaaaaaaaaaaa!", "bbbbbbb!", "cccc!"};
        for (String message : messages) {
            client.send(message);
        }

        Thread.sleep(75);
    }

    @Test
    public void testClientDisconnect() throws Exception {
        System.setProperty("log4j.configurationFile","log4j2.properties");
        try {

            DecoderFactory<String> decoderFactory = new DecoderFactory<>() {

                @Override
                public IDecoder<String> getDecoder() {
                    return new Coder();
                }
            };

            CallbackFactory<String> callbackFactory = new CallbackFactory<>() {
                @Override
                public CallbackServer<String> getCallback() {
                    return new CallbackServer<>() {

                        @Override
                        public void onMessageReceive(String message, IClient<String> client) {

                            try {
                                client.send(message);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onNewClient(IClient<String> client)
                        {
                        }

                        @Override
                        public void onQuitClient(IClient<String> client) {
                            assert false;
                        }

                        @Override
                        public void onException(Exception e) {
                            assert false;
                        }
                    };
                }
            };

            Server<String> server = new Server<>(IP, PORT, decoderFactory, callbackFactory);
            server.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        Client<String> client = new Client<>(new Coder(), new CallbackClient<>() {
            @Override
            public void onMessageReceive(String message) {
                System.out.println("got message: " + message);
            }

            @Override
            public void onException(Exception e) {
                assert false;
            }

            @Override
            public void onDisconnect() {assert false;}
        });

        client.connect(IP, PORT);
        client.start();

        String[] messages = {"aaaaaaaaaaaaaaaaa!", "bbbbbbb!", "cccc!"};
        for (String message : messages) {
            client.send(message);
        }

        //client.send("and another one");
        Thread.sleep(750);
        //System.out.println(messageFrom + " = " + messageGot);
    }

}
