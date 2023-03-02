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
import net.stalemate.server.core.units.MotorizedUnitOther;
import net.stalemate.server.core.controller.Game;

public class MotorizeButton implements Unit.IStandardButton {
    @Override
    public String bind() {
        return "Q";
    }

    @Override
    public String texture() {
        return "assets/ui/buttons/motorize.png";
    }

    @Override
    public String identifier() {
        return "button_motorize";
    }

    @Override
    public void action(Unit unit, Game gameController) {
        if (!unit.hasTurnEnded() && unit.getSupply()-3>0){
            unit.setSupply(unit.getSupply()-3);
            unit.setEntrenchment(0);
            unit.protectUnitWith(null);
            gameController.rmEntity(unit);
            MotorizedUnitOther u = new MotorizedUnitOther(unit.getX(), unit.getY(), gameController, unit);
            gameController.getUnitsTeam(unit).addUnit(u);
            gameController.addEntity(u);
            u.endTurn();
        }
    }
}
