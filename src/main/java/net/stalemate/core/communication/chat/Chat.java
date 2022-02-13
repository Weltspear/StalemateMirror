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

package net.stalemate.core.communication.chat;

import java.util.ArrayList;

public class Chat {
    private final ArrayList<Message> chat = new ArrayList<>();
    private String log = "";

    public Chat(){

    }

    public void pushMsg(Message msg){
        chat.add(msg);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Message> read(){
        ArrayList<Message> c1 = (ArrayList<Message>) chat.clone();
        for (Message msg: chat){
            log = log + "[" + msg.author + "]: " + msg.message + "\n";
        }
        chat.clear();
        return c1;
    }
}
