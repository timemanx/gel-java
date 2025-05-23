package com.geldata.driver.binary.codecs.common;

import org.jetbrains.annotations.NotNull;

import com.geldata.driver.binary.BinaryEnum;

public enum RangeFlags implements BinaryEnum<Byte> {
    EMPTY (1),
    INCLUDE_LOWER_BOUNDS (1 << 1),
    INCLUDE_UPPER_BOUNDS (1 << 2),
    INFINITE_LOWER_BOUNDS (1 << 3),
    INFINITE_UPPER_BOUNDS (1 << 4);
    private final byte value;

    RangeFlags(int value) {
        this.value = (byte)value;
    }

    @Override
    public @NotNull Byte getValue() {
        return this.value;
    }
}
