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
import net.stalemate.server.core.buttons.AttackButton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/***
 * Marks a method as a method to be called when certain event is triggered
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnEvent {
    /***
     * Marks EventType as unimplemented
     */
    @interface UnimplementedEvent{

    }

    enum EventType{
        // General events
        /***
         * <code>method(Unit attacker, Unit attacked_unit)</code>
         * Triggered when unit attacks another unit
         * @see AttackButton
         */
        ON_UNIT_ATTACK,

        /***
         * <code>method(Unit dead_unit)</code>
         * Triggered on unit death
         * @see Unit#update()
         */
        ON_UNIT_DEATH,

        @UnimplementedEvent
        ON_HP_SACRIFICE,
        @UnimplementedEvent
        ON_MOVE,
        @UnimplementedEvent
        ON_BASE_UNIT_DEPLOY,
        @UnimplementedEvent
        ON_TANK_FACTORY_UNIT_DEPLOY,
        @UnimplementedEvent
        ON_SUPPLY_STATION_RESUPPLY,
        @UnimplementedEvent
        ON_MOTORIZED_UNIT_RESUPPLY,
        @UnimplementedEvent
        ON_BASE_DESTRUCTION,
        @UnimplementedEvent
        ON_BASE_BUILD,

        // Misc events
        @UnimplementedEvent
        ON_GAME_END

    }

    EventType type();

}
