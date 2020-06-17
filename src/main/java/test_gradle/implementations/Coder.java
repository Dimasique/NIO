package test_gradle.implementations;

import test_gradle.interfaces.IDecoder;

public class Coder implements IDecoder<String> {

    @Override
    public byte[] encode(String message) {
        return message.getBytes();
    }

    @Override
    public String decode(byte[] bytes) {
        return new String(bytes);
    }

}
