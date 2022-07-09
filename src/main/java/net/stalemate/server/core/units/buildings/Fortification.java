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

package net.stalemate.server.core.units.buildings;

import net.stalemate.server.core.Unit;
import net.stalemate.server.core.animation.Animation;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.buttons.AttackButton;
import net.stalemate.server.core.buttons.Scrap;
import net.stalemate.server.core.buttons.util.Unflippable;
import net.stalemate.server.core.units.Infantry;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.util.IGameController;
import net.stalemate.server.core.util.PriorityTurnUpdate;

import java.util.ArrayList;

public class Fortification extends Unit implements IBuilding, Unflippable, PriorityTurnUpdate {
    private final Infantry unitInside;

    public Fortification(int x, int y, IGameController game, Infantry unit) {
        super(x, y, game, new UnitStats(15, 15, 2, 0, 2, 1, 30, 30, 2, 0, 0), new AnimationController(), "Fortification");

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/fortification.png");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/fortification_attack_1.png");
        attack.addFrame("assets/units/fortification_attack_2.png");
        attack.addFrame("assets/units/fortification_attack_3.png");
        attack.addFrame("assets/units/fortification_attack_4.png");
        attack.addFrame("assets/units/fortification_attack_5.png");
        attack.addFrame("assets/units/fortification_attack_2.png");
        attack.addFrame("assets/units/fortification_attack_3.png");
        attack.addFrame("assets/units/fortification_attack_4.png");
        attack.addFrame("assets/units/fortification_attack_5.png");
        attack.addFrame("assets/units/fortification_attack_6.png");
        attack.addFrame("assets/units/fortification_attack_7.png");

        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
        anim.addAnimation("attack", attack);
        anim.addShift("attack", "idle");

        unitInside = unit;
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();
        buttons.add(new AttackButton(attack_range));
        buttons.add(new Scrap());
        return buttons;
    }

    public void setSupplyLevel(float supplyLevel){
        supply = (int) (supply * supplyLevel);
    }

    @Override
    public void onDeath() {
        unitInside.setSupply(
                (int) (((float)getSupply()/(float)getMaxSupply())
                        * (float)unitInside.getMaxSupply()));
        unitInside.setX(x);
        unitInside.setY(y);
        unitInside.endTurn();
        game.addEntity(unitInside);
    }


}
