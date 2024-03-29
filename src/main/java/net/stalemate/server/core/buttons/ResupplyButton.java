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
import net.stalemate.server.core.controller.Game;

public class ResupplyButton implements Unit.ISelectorButtonUnit {

    private Unit.Layer layer = Unit.Layer.GROUND;

    public ResupplyButton(){

    }

    public ResupplyButton(Unit.Layer l){
        this.layer = l;
    }

    @Override
    public String bind() {
        return layer == Unit.Layer.GROUND ? "S" : "R";
    }

    @Override
    public String texture() {
        return "assets/ui/buttons/resupply_button.png";
    }

    @Override
    public String identifier() {
        return layer == Unit.Layer.GROUND ? "button_resupply_ground" : "button_resupply_air";
    }

    @Override
    public void action(Unit selected_unit, Unit unit, Game gameController) {
        if (!unit.hasTurnEnded() && unit != selected_unit){
            int needed_supply = selected_unit.unitStats().getMaxSupply() - selected_unit.unitStats().getSupply();
            if (unit.getSupply() - needed_supply > 1){
                unit.endTurn();
                selected_unit.consumeSupply(-needed_supply);
                unit.consumeSupply(needed_supply);
            }
        }
    }

    @Override
    public int selector_range() {
        return 1;
    }

    @Override
    public String selector_texture() {
        return "assets/ui/selectors/ui_resupply.png";
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

    @Override
    public Unit.Layer getLayer() {
        return layer;
    }
}
