package com.geldata.driver.binary.protocol.v1.receivables;

import org.jetbrains.annotations.NotNull;
import org.joou.UShort;

import com.geldata.driver.binary.PacketReader;
import com.geldata.driver.binary.protocol.Receivable;
import com.geldata.driver.binary.protocol.ServerMessageType;
import com.geldata.driver.binary.protocol.common.Annotation;

public class RestoreReady implements Receivable {
    public final Annotation @NotNull [] annotations;
    public final @NotNull UShort jobs;

    public RestoreReady(@NotNull PacketReader reader) {
        annotations = reader.readAnnotations();
        jobs = reader.readUInt16();
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.RESTORE_READY;
    }
}
