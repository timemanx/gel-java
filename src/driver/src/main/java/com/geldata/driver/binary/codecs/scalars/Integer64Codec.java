package com.geldata.driver.binary.codecs.scalars;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.geldata.driver.binary.PacketReader;
import com.geldata.driver.binary.PacketWriter;
import com.geldata.driver.binary.codecs.CodecContext;
import com.geldata.driver.binary.protocol.common.descriptors.CodecMetadata;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public final class Integer64Codec extends ScalarCodecBase<Long> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000105");
    public Integer64Codec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, Long.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Long value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public @NotNull Long deserialize(@NotNull PacketReader reader, CodecContext context) {
        return reader.readInt64();
    }
}
