package com.geldata.driver.binary.codecs;

public interface RuntimeCodec<T> extends Codec<T> {
    ComplexCodec<?> getBroker();
}
