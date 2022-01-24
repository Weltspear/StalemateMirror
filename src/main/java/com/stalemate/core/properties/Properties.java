package com.stalemate.core.properties;

import java.util.ArrayList;

public class Properties {
    private final ArrayList<Property> properties = new ArrayList<>();

    public Properties(){

    }

    public void put(String key, String value){
        properties.add(new Property(key, value));
    }

    public ArrayList<String[]> getProperties(){
        ArrayList<String[]> properties = new ArrayList<>();
        for (Property p: this.properties){
            properties.add(p.asStringArray());
        }
        return properties;
    }
}
