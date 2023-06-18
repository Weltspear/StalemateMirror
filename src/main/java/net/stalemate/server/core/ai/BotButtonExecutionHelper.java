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

package net.stalemate.server.core.ai;

import net.stalemate.server.core.Unit;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.units.EngineerUnit;

public class BotButtonExecutionHelper {

    public void build(Class<? extends Unit> building, int x, int y, Unit.IButton[] buttons, Unit executor, Game g){
        for (Unit.IButton b: buttons){
            if (b instanceof EngineerUnit.ConstructBuildingButton cb){
                if (cb.getUnitBuilt() == building){
                    cb.action(x, y, executor, g);
                }
            }
        }
    }

    public int getBuildingCost(Class<? extends Unit> building, Unit.IButton[] buttons){
        for (Unit.IButton b: buttons){
            if (b instanceof EngineerUnit.ConstructBuildingButton cb){
                if (cb.getUnitBuilt() == building){
                    return cb.getCost();
                }
            }
        }
        return -1;
    }

    public int getBuildingBuildTime(Class<? extends Unit> building, Unit.IButton[] buttons){
        for (Unit.IButton b: buttons){
            if (b instanceof EngineerUnit.ConstructBuildingButton cb){
                if (cb.getUnitBuilt() == building){
                    return cb.getConstructionTime();
                }
            }
        }
        return -1;
    }
}
