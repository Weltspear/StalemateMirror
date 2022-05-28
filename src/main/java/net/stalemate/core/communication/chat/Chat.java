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

import net.stalemate.networking.client.config.ButtonTooltips;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.stalemate.log.MakeLog.makeLog;

public class Chat {
    private final ArrayList<Message> chat = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private static final Logger LOGGER = makeLog(Logger.getLogger(Chat.class.getName()));

    public Chat(){

    }

    public void pushMsg(Message msg){
        lock.lock();
        chat.add(msg);
        LOGGER.log(Level.INFO, "[Chat: " + this.hashCode() + "] [" + msg.author + "]: " + msg.message);
        lock.unlock();
    }

    public ArrayList<String> read(){
        lock.lock();
        try {
            if (chat.size() > 0) {
                ArrayList<String> c1 = new ArrayList<>();
                for (int i = 0; i < 10 && i < chat.size(); i++) {
                    if (chat.get(i).getTimesRead() >= 200) {
                        chat.remove(chat.get(i));
                        if (i > chat.size()) {
                            break;
                        }
                        if (i == 0 && chat.size() == 0) {
                            break;
                        }
                    }
                    c1.add(chat.get(i).read());
                }
                return c1;
            }
            return new ArrayList<>();
        } finally {
            lock.unlock();
        }
    }
}
