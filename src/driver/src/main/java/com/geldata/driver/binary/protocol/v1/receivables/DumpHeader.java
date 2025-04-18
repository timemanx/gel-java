package com.geldata.driver.binary.protocol.v1.receivables;

import org.jetbrains.annotations.NotNull;
import org.joou.UInteger;
import org.joou.UShort;

import com.geldata.driver.binary.PacketReader;
import com.geldata.driver.binary.protocol.Receivable;
import com.geldata.driver.binary.protocol.ServerMessageType;
import com.geldata.driver.binary.protocol.common.DumpObjectDescriptor;
import com.geldata.driver.binary.protocol.common.DumpTypeInfo;
import com.geldata.driver.binary.protocol.common.KeyValue;

public class DumpHeader implements Receivable {
    public final KeyValue @NotNull [] attributes;
    public final @NotNull UShort majorVersion;
    public final @NotNull UShort minorVersion;
    public final @NotNull String schemaDDL;
    public final DumpTypeInfo @NotNull [] typeInfo;
    public final DumpObjectDescriptor @NotNull [] descriptors;

    public DumpHeader(@NotNull PacketReader reader) {
        attributes = reader.readAttributes();
        majorVersion = reader.readUInt16();
        minorVersion = reader.readUInt16();
        schemaDDL = reader.readString();
        typeInfo = reader.readArrayOf(DumpTypeInfo.class, DumpTypeInfo::new, UInteger.class);
        descriptors = reader.readArrayOf(DumpObjectDescriptor.class, DumpObjectDescriptor::new, UInteger.class);
    }

    @Override
    public void close() throws Exception {
        release(attributes);
        release(descriptors);
    }

    @Override
    public @NotNull ServerMessageType getMessageType() {
        return ServerMessageType.DUMP_HEADER;
    }
}
