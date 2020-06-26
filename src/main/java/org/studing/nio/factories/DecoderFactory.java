package org.studing.nio.factories;

import org.studing.nio.interfaces.IDecoder;

public abstract class DecoderFactory<T> {

    public abstract IDecoder<T> getDecoder();

}
