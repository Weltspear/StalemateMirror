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

import net.stalemate.server.core.AirUnit;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.controller.Game;

public class ProtectUnitButton implements Unit.ISelectorButtonUnit{
    private final Unit.Layer l;

    @Override
    public String bind() {
        return "P";
    }

    @Override
    public String texture() {
        return "texture_missing";
    }

    @Override
    public String identifier() {
        return "button_protect";
    }

    public ProtectUnitButton(Unit.Layer l){
        this.l = l;
    }

    @Override
    public void action(Unit selected_unit, Unit unit, Game gameController) {
        if (selected_unit != unit)
        if (!unit.hasTurnEnded()){
            if (gameController.getUnitsTeam(selected_unit) == gameController.getUnitsTeam(unit)){
                if (selected_unit.getProtector() == null){
                    if ((l == Unit.Layer.GROUND && !(selected_unit instanceof AirUnit))||
                            (l == Unit.Layer.AIR && (selected_unit instanceof AirUnit))) {
                        unit.endTurn();
                        selected_unit.protectUnitWith(unit);
                    }
                }
            }
        }
    }

    @Override
    public int selector_range() {
        return 1;
    }

    @Override
    public String selector_texture() {
        return "assets/ui/selectors/ui_move.png";
    }

    @Override
    public boolean isUsedOnOurUnit() {
        return true;
    }

    @Override
    public boolean isUsedOnEnemy() {
        return false;
    }

    @Override
    public boolean isUsedOnAlliedUnit() {
        return true;
    }


}
