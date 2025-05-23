package com.geldata.driver.binary.protocol.common;

import org.jetbrains.annotations.NotNull;

import com.geldata.driver.binary.BinaryEnum;

public enum CompilationFlags implements BinaryEnum<Long> {
    NONE                (0),
    IMPLICIT_TYPE_IDS   (1),
    IMPLICIT_TYPE_NAMES (1 << 1),
    EXPLICIT_OBJECT_IDS (1 << 2);

    private final long value;

    CompilationFlags(long value) {
        this.value = value;
    }

    @Override
    public @NotNull Long getValue() {
        return this.value;
    }
}
