package com.geldata.driver.binary.codecs;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.geldata.driver.binary.PacketReader;
import com.geldata.driver.binary.PacketWriter;
import com.geldata.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.geldata.driver.exceptions.GelException;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Type;
import java.util.UUID;

public interface Codec<T> {
    UUID getId();

    @Nullable CodecMetadata getMetadata();

    void serialize(final PacketWriter writer, final @Nullable T value, final CodecContext context) throws OperationNotSupportedException, GelException;
    @Nullable T deserialize(final PacketReader reader, final CodecContext context) throws GelException, OperationNotSupportedException;

    Class<T> getConvertingClass();
    boolean canConvert(Type type);

    static <T> ByteBuf serializeToBuffer(@NotNull Codec<T> codec, final @Nullable T value, final CodecContext context) throws OperationNotSupportedException, GelException {
        try (var writer = new PacketWriter()) {
            codec.serialize(writer, value, context);
            return writer.getBuffer();
        }
    }

    static <T> @Nullable T deserializeFromBuffer(@NotNull Codec<T> codec, final @NotNull ByteBuf buffer, final CodecContext context) throws GelException, OperationNotSupportedException {
        var reader = new PacketReader(buffer);
        return codec.deserialize(reader, context);
    }
}
