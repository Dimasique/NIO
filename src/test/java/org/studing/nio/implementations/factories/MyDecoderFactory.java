package org.studing.nio.implementations.factories;

import org.studing.nio.factories.DecoderFactory;
import org.studing.nio.implementations.Coder;
import org.studing.nio.interfaces.IDecoder;

public class MyDecoderFactory extends DecoderFactory<String> {
    @Override
    public IDecoder<String> getDecoder() {
        return new Coder();
    }
}
