package org.studing.nio.factories;

import org.studing.nio.interfaces.CallbackServer;

public abstract class CallbackFactory<T> {

    public abstract CallbackServer<T> getCallback();

}
