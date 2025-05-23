package com.geldata.driver.binary.codecs.scalars;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.geldata.driver.binary.PacketReader;
import com.geldata.driver.binary.PacketWriter;
import com.geldata.driver.binary.codecs.CodecContext;
import com.geldata.driver.binary.protocol.common.descriptors.CodecMetadata;

import javax.naming.OperationNotSupportedException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class LocalTimeCodec extends ScalarCodecBase<LocalTime> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000010D");

    public LocalTimeCodec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, LocalTime.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable LocalTime value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(ChronoUnit.MICROS.between(LocalTime.MIDNIGHT, value));
        }
    }

    @Override
    public @NotNull LocalTime deserialize(@NotNull PacketReader reader, CodecContext context) {
        return LocalTime.MIDNIGHT.plus(reader.readInt64(), ChronoUnit.MICROS);
    }
}
