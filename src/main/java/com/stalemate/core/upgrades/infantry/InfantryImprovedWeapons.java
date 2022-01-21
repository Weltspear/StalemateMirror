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

package com.stalemate.core.upgrades.infantry;

import com.stalemate.core.units.Infantry;
import com.stalemate.core.upgrade.Upgrade;

public class InfantryImprovedWeapons implements Upgrade<Infantry> {
    public InfantryImprovedWeapons(){

    }

    @Override
    public void upgradeUnit(Infantry unit) {
        unit.setDf(unit.getDf() + 1);
        unit.setAtk(unit.getAtk() + 1);
    }

    @Override
    public String name() {
        return "upgrade_infantry_improved_weapons";
    }

    @Override
    public String texture() {
        return null;
    }

    @Override
    public long id() {
        return 0;
    }
}
