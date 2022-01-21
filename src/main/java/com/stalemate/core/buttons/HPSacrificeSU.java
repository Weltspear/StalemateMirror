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

package com.stalemate.core.buttons;

import com.stalemate.core.Unit;
import com.stalemate.core.util.IGameController;

public class HPSacrificeSU implements Unit.IStandardButton {
    @Override
    public String bind() {
        return "S";
    }

    @Override
    public String texture() {
        return "assets/ui/buttons/sacrifice_button.png";
    }

    @Override
    public String identifier() {
        return "button_hpsacrificesu";
    }

    @Override
    public void action(Unit unit, IGameController gameController) {
        if (!unit.hasTurnEnded()){
            if (unit.unitStats().getHp() - 1 > 0){
                unit.consumeSupply(-2);
                unit.damage(1);
                unit.endTurn();
            }
        }
    }
}
