package com.geldata.driver.binary.protocol.v2.descriptors;

import org.jetbrains.annotations.NotNull;
import org.joou.UShort;

import com.geldata.driver.binary.PacketReader;
import com.geldata.driver.binary.codecs.Codec;
import com.geldata.driver.binary.protocol.TypeDescriptor;
import com.geldata.driver.binary.protocol.TypeDescriptorInfo;
import com.geldata.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.geldata.driver.binary.protocol.common.descriptors.TypeOperation;

import java.util.UUID;
import java.util.function.Function;

public final class CompoundTypeDescriptor implements TypeDescriptor, MetadataDescriptor {
    public final UUID id;
    public final String name;
    public final boolean isSchemaDefined;
    public final TypeOperation operation;
    public final UShort[] components;

    public CompoundTypeDescriptor(UUID id, PacketReader reader) {
        this.id = id;
        this.name = reader.readString();
        this.isSchemaDefined = reader.readBoolean();
        this.operation = reader.readEnum(TypeOperation.class, Byte.TYPE);
        this.components = reader.readArrayOf(UShort.class, PacketReader::readUInt16, UShort.class);
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public @NotNull CodecMetadata getMetadata(
            Function<Integer, Codec<?>> getRelativeCodec,
            Function<Integer, TypeDescriptorInfo<?>> getRelativeDescriptor
    ) {
        return new CodecMetadata(name, isSchemaDefined);
    }
}
