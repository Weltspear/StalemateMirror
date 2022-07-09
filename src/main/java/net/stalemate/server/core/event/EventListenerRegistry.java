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

package net.stalemate.server.core.event;

import net.stalemate.server.core.Unit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class EventListenerRegistry {
    private final ArrayList<EventListener> eventListeners = new ArrayList<>();

    public void addEventListener(EventListener eventListener){
        eventListeners.add(eventListener);
    }

    public void triggerUnitAttackEvent(Unit attacker, Unit attacked){
        for (EventListener eventListener: eventListeners){
            for (Method m : eventListener.getClass().getMethods()){
                if (m.isAnnotationPresent(OnEvent.class)){
                    OnEvent a = m.getAnnotation(OnEvent.class);
                    if (a.type().equals(OnEvent.EventType.ON_UNIT_ATTACK)){
                        try {
                            m.invoke(eventListener, attacker, attacked);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public void triggerUnitDeathEvent(Unit dead_unit){
        for (EventListener eventListener: eventListeners){
            for (Method m : eventListener.getClass().getMethods()){
                if (m.isAnnotationPresent(OnEvent.class)){
                    OnEvent a = m.getAnnotation(OnEvent.class);
                    if (a.type().equals(OnEvent.EventType.ON_UNIT_DEATH)){
                        try {
                            m.invoke(eventListener, dead_unit);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
