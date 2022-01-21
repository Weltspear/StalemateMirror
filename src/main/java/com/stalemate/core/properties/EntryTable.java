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

package com.stalemate.core.properties;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

@SuppressWarnings("unchecked")
public class EntryTable {

    protected HashMap<String,Object> table = new HashMap<>();

    public EntryTable(){
        table.put("entryTableMark", true);
    }

    public void setString(String key, String var){
        table.put(key, var);
    }

    public String getString(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof String s) {
                return s;
            }
        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public void setLong(String key, long var){
        table.put(key, var);
    }

    public Long getLong(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof Long s) {
                return s;
            }
        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public void setInt(String key, int var){
        table.put(key, var);
    }

    public Integer getInteger(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof Integer s) {
                return s;
            }
        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public void setDouble(String key, double var){
        table.put(key, var);
    }

    public Double getDouble(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof Double s) {
                return s;
            }
        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public void setFloat(String key, float var){
        table.put(key, var);
    }

    public Float getFloat(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof Float s) {
                return s;
            }
            else if (table.get(key) instanceof Double s){
                double s1 = s;
                return (float)s1;
            }
        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public EntryTable getEntryTable(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof HashMap s) {
                if (s.containsKey("entryTableMark")){
                    EntryTable table = new EntryTable();
                    table.table = s;
                    return table;
                }
            }
            else if (table.get(key) == null){
                return null;
            }
        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public void setEntryTable(String key, EntryTable var){
        this.table.put(key, var != null ? var.table: null);
    }

    public ArrayList<EntryTable> getEntryTableArrayList(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof ArrayList t) {
                ArrayList<EntryTable> entryTables = new ArrayList<>();
                for (Object wildcard: t){
                    if (wildcard == null){
                        entryTables.add(null);
                    }
                    else if (wildcard instanceof HashMap map){
                        if (map.containsKey("entryTableMark")){
                            EntryTable entryTable = new EntryTable();
                            entryTable.table = map;
                            entryTables.add(entryTable);
                        }
                    }
                }
                return entryTables;
            }

        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public void setEntryTableArrayList(String key, ArrayList<EntryTable> var){
        ArrayList<HashMap<String, Object>> tables = new ArrayList<>();
        for (EntryTable entryTable: var){
            tables.add(entryTable != null ? entryTable.table : null);
        }
        table.put(key, tables);
    }

    public ArrayList<String> getStringArrayList(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof ArrayList t) {
                ArrayList<String> strings = new ArrayList<>();
                for (Object wildcard: t){
                    if (wildcard instanceof String str){
                        strings.add(str);
                    }
                }
                return strings;
            }
        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public void setStringArrayList(String key, ArrayList<String> var){
        table.put(key, var);
    }

    public Boolean getBoolean(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof Boolean s) {
                return s;
            }
        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public void setBoolean(String key, Boolean var){
        table.put(key, var);
    }

    public Character getChar(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof String s && s.length() == 1) {
                return s.charAt(0);
            }
        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public void setChar(String key, char var){
        table.put(key, var);
    }

    public Color getColor(String key){
        EntryTable t = getEntryTable(key);
        return new Color(t.getInteger("r"), t.getInteger("g"), t.getInteger("b"));
    }

    public void setColor(String key, Color var){
        EntryTable t = new EntryTable();
        t.setInt("r", var.getRed());
        t.setInt("g", var.getGreen());
        t.setInt("b", var.getBlue());
        setEntryTable(key, t);
    }

    public ArrayList<ArrayList<EntryTable>> get2DEntryTableArrayList(String key){
        if (table.containsKey(key)){
            if (table.get(key) instanceof ArrayList t) {
                ArrayList<ArrayList<EntryTable>> entryTables = new ArrayList<>();
                int y = 0;
                for (Object wildcard: t){
                    entryTables.add(new ArrayList<>());
                    if (wildcard instanceof ArrayList t2){
                        for (Object wildcard2 : t2){
                            if (wildcard2 == null){
                                entryTables.get(y).add(null);
                            }
                            else if (wildcard2 instanceof HashMap map){
                                if (map.containsKey("entryTableMark")){
                                    EntryTable entryTable = new EntryTable();
                                    entryTable.table = map;
                                    entryTables.get(y).add(entryTable);
                                }
                            }
                        }
                    }
                    y++;
                }
                return entryTables;
            }

        }
        throw new NoSuchElementException("Element " + key + " not found");
    }

    public void set2DEntryTableArrayList(String key, ArrayList<ArrayList<EntryTable>> ar2D){
        ArrayList<ArrayList<HashMap<String, Object>>> ar2DH = new ArrayList<>();
        int y = 0;
        for (ArrayList<EntryTable> row: ar2D){
            ar2DH.add(new ArrayList<>());
            for (EntryTable point: row){
                if (point != null)
                    ar2DH.get(y).add(point.table);
                else
                    ar2DH.get(y).add(null);
            }
            y++;
        }
        this.table.put(key, ar2DH);
    }

    public HashMap<String, Object> getTable() {
        return table;
    }

    public EntryTable(HashMap<String, Object> table){
        this.table = table;
    }
}

