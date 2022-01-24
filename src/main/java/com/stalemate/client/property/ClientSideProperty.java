package com.stalemate.client.property;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record ClientSideProperty(String key, String value){
    public ClientSideProperty(String[] property){
        this(property[0], property[1]);
    }

    @Contract(value = " -> new", pure = true)
    public String @NotNull [] asStringArray(){
        return new String[]{key, value};
    }
}
