package com.stalemate.core.properties;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Property(String key, String value){
    @Contract(value = " -> new", pure = true)
    public String @NotNull [] asStringArray(){
        return new String[]{key, value};
    }
}
