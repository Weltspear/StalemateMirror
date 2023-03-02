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

package net.stalemate.server.core.buttons;

import net.stalemate.server.core.Unit;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.controller.Game;

public class Scrap implements Unit.IStandardButton {
    @Override
    public String bind() {
        return "S";
    }

    @Override
    public String texture() {
        return "assets/ui/buttons/scrap_button.png";
    }

    @Override
    public String identifier() {
        return "button_building_scrap";
    }

    @Override
    public void action(Unit unit, Game gameController) {
        if (unit instanceof IBuilding && !unit.hasTurnEnded()){
            unit.setHp(-1);
        }
    }
}
