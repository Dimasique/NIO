package test_gradle.implementations.factories;

import test_gradle.factories.DecoderFactory;
import test_gradle.implementations.Coder;
import test_gradle.interfaces.IDecoder;

public class MyDecoderFactory extends DecoderFactory<String> {
    @Override
    public IDecoder<String> getDecoder() {
        return new Coder();
    }
}
