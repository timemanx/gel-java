package com.geldata.driver.binary;

import org.jetbrains.annotations.NotNull;

import com.geldata.driver.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;

public interface BinaryEnum<T extends Number> extends SerializableData {
    T getValue();

    default void write(final @NotNull PacketWriter writer) throws OperationNotSupportedException {
        writer.writePrimitive(getValue());
    }

    default int getSize() {
        return BinaryProtocolUtils.sizeOf(getValue().getClass());
    }
}
