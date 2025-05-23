package com.geldata.driver.namingstrategies;

import org.jetbrains.annotations.NotNull;

import com.geldata.driver.util.StringsUtil;

final class CamelCaseNamingStrategy implements NamingStrategy {
    public static final CamelCaseNamingStrategy INSTANCE = new CamelCaseNamingStrategy();

    @Override
    public @NotNull String convert(@NotNull String name) {
        if (StringsUtil.isNullOrEmpty(name)) {
            return name;
        }

        return (Character.toLowerCase(name.charAt(0)) + name.substring(1)).replaceAll("_", "");
    }
}
