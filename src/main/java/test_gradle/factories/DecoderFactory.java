package test_gradle.factories;

import test_gradle.interfaces.IDecoder;

public abstract class DecoderFactory<T> {

    public abstract IDecoder<T> getDecoder();

}
