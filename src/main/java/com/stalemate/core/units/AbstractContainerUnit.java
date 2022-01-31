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

package com.stalemate.core.units;

import com.stalemate.core.Unit;
import com.stalemate.core.animation.AnimationController;
import com.stalemate.core.util.IGameController;

public abstract class AbstractContainerUnit<T extends Unit> extends Unit{

    protected T[] contained_units;

    /***
     * NOTE: If you don't want unit to have supply set <code>UnitStats.supply</code> to -1
     */
    public AbstractContainerUnit(int x, int y, IGameController game, UnitStats unitStats, AnimationController anim,
                                 String name, int size) {
        super(x, y, game, unitStats, anim, name);
        contained_units = (T[])(new Unit[size]);
    }
}
