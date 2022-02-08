/*
 * Stalemate Game
 * Copyright (C) 2022 Weltspear
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.stalemate.core.properties;

import net.panic.ErrorResult;
import net.panic.Expect;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

@SuppressWarnings("unchecked")
public class EntryTable {
    public static class EntryTableGetFailure implements ErrorResult {
        @Override
        public String message() {
            return "Object not found";
        }
    }

    protected HashMap<String,Object> table = new HashMap<>();

    public EntryTable(){
    }

    public<T> Expect<T, EntryTableGetFailure> get(String key){
        if (table.containsKey(key)){
            try {
                return new Expect<>((T) table.get(key));
            } catch (ClassCastException e){
                return new Expect<>((T) table.get(key));
            }
        } else{
            return new Expect<>(new EntryTableGetFailure());
        }
    }

    public<T> void set(String key, T var){
        table.put(key, var);
    }

    public void setString(String key, String var){
        set(key, var);
    }

    public Expect<String, EntryTableGetFailure> getString(String key){
        return get(key);
    }

    public void setLong(String key, long var){
        set(key, var);
    }

    public Expect<Long, EntryTableGetFailure> getLong(String key){
        return get(key);
    }

    public void setInt(String key, int var){
        set(key, var);
    }

    public Expect<Integer, EntryTableGetFailure> getInteger(String key){
        return get(key);
    }

    public void setDouble(String key, double var){
        set(key, var);
    }

    public Expect<Double, EntryTableGetFailure> getDouble(String key){
        return get(key);
    }

    public void setFloat(String key, float var){
        set(key, var);
    }

    public Expect<Float, EntryTableGetFailure> getFloat(String key){
        Expect<Double, EntryTableGetFailure> d = get(key);
        if (d.isNone()){
            return new Expect<>(d.getResult());
        }

        return new Expect<>(d.unwrap().floatValue());
    }

    public Expect<Boolean, EntryTableGetFailure> getBoolean(String key){
        return get(key);
    }

    public void setBoolean(String key, Boolean var){
        set(key, var);
    }

    public Expect<Character, EntryTableGetFailure> getChar(String key){
        return get(key);
    }

    public void setChar(String key, char var){
        table.put(key, var);
    }

    public HashMap<String, Object> getTable() {
        return table;
    }

    public EntryTable(HashMap<String, Object> table){
        this.table = table;
    }
}

