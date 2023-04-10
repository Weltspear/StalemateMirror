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
import net.stalemate.server.core.Entity;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.units.util.IBuilding;

public class BombButton implements Unit.IStandardButton {
    private boolean isStrategic = false;

    @Override
    public String bind() {
        return "B";
    }

    @Override
    public String texture() {
        return "texture_missing";
    }

    @Override
    public String identifier() {
        return "button_bomb";
    }

    /***
     * Makes this button deal extra damage to buildings
     */
    public BombButton makeStrategic(){
        isStrategic = true;
        return this;
    }

    @Override
    public void action(Unit unit, Game gameController) {
        if (!unit.hasTurnEnded() && unit.getSupply() - 3 > 0){
            for (Entity entity : gameController.getEntities(unit.getX(), unit.getY())){
                if (entity instanceof Unit enemy && !(entity instanceof AirUnit)){
                    if (gameController.getUnitsTeam(enemy) != gameController.getUnitsTeam(unit)){
                        if (!isStrategic)
                            enemy.damage(unit.getAtk());
                        else
                            if (enemy instanceof IBuilding){
                                enemy.damage(unit.getAtk()+2);
                            }
                            else{
                                enemy.damage(unit.getAtk()-1);
                            }
                        enemy.consumeSupply(2);
                        unit.consumeSupply(3);
                        gameController.getEvReg().triggerUnitAttackEvent(unit, enemy);
                        unit.endTurn();
                        break;
                    }
                }
            }
        }
    }
}
