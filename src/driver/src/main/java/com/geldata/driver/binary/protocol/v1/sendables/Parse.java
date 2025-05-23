package com.geldata.driver.binary.protocol.v1.sendables;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import com.geldata.driver.Capabilities;
import com.geldata.driver.binary.PacketWriter;
import com.geldata.driver.binary.protocol.ClientMessageType;
import com.geldata.driver.binary.protocol.Sendable;
import com.geldata.driver.binary.protocol.common.Cardinality;
import com.geldata.driver.binary.protocol.common.CompilationFlags;
import com.geldata.driver.binary.protocol.common.IOFormat;
import com.geldata.driver.util.BinaryProtocolUtils;

import javax.naming.OperationNotSupportedException;
import java.util.EnumSet;
import java.util.UUID;

public class Parse extends Sendable {
    private final EnumSet<Capabilities> capabilities;
    private final EnumSet<CompilationFlags> compilationFlags;
    private final IOFormat format;
    private final Cardinality cardinality;
    private final String query;
    private final long implicitLimit;
    private final UUID stateTypeDescriptorId;
    private final ByteBuf stateData;

    public Parse(
            EnumSet<Capabilities> capabilities,
            EnumSet<CompilationFlags> compilationFlags,
            IOFormat format,
            Cardinality cardinality,
            String query,
            long implicitLimit,
            UUID stateTypeDescriptorId,
            ByteBuf stateData
    ) {
        super(ClientMessageType.PARSE);
        this.capabilities = capabilities;
        this.compilationFlags = compilationFlags;
        this.format = format;
        this.cardinality = cardinality;
        this.query = query;
        this.implicitLimit = implicitLimit;
        this.stateTypeDescriptorId = stateTypeDescriptorId;
        this.stateData = stateData;
    }

    @Override
    public int getDataSize() {
        return
                BinaryProtocolUtils.SHORT_SIZE +
                BinaryProtocolUtils.LONG_SIZE +
                BinaryProtocolUtils.LONG_SIZE +
                BinaryProtocolUtils.LONG_SIZE +
                BinaryProtocolUtils.BYTE_SIZE +
                BinaryProtocolUtils.BYTE_SIZE +
                BinaryProtocolUtils.sizeOf(query) +
                BinaryProtocolUtils.UUID_SIZE +
                BinaryProtocolUtils.sizeOf(stateData);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected void buildPacket(@NotNull PacketWriter writer) throws OperationNotSupportedException {
        if(query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        writer.write((short) 0); // annotations (zero of them)

        writer.writeEnumSet(capabilities, Long.TYPE);
        writer.writeEnumSet(compilationFlags, Long.TYPE);
        writer.write(implicitLimit);
        writer.write(format);
        writer.write(cardinality);
        writer.write(query);

        writer.write(stateTypeDescriptorId);
        writer.writeArray(stateData);
    }
}
