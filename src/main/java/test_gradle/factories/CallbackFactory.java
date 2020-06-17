package test_gradle.factories;

import test_gradle.interfaces.CallbackServer;

public abstract class CallbackFactory<T> {

    public abstract CallbackServer<T> getCallback();

}
