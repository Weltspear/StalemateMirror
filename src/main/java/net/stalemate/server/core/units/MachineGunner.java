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

package net.stalemate.server.core.units;

import net.stalemate.server.core.Unit;
import net.stalemate.server.core.animation.Animation;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.buttons.AttackButton;
import net.stalemate.server.core.buttons.HPSacrificeSU;
import net.stalemate.server.core.buttons.MoveButton;
import net.stalemate.server.core.util.IGameController;

import java.util.ArrayList;

public class MachineGunner extends Unit{

    private volatile boolean mode = true;

    public class OffensiveModeButton implements IStandardButton{
        @Override
        public String bind() {
            return "O";
        }

        @Override
        public String texture() {
            return "assets/ui/buttons/machine_gunner_offensive.png";
        }

        @Override
        public String identifier() {
            return "button_machine_gunner_offensive";
        }

        @Override
        public void action(Unit unit, IGameController gameController) {
            if (!hasTurnEnded){
                mode = true;
                endTurn();
            }
        }
    }

    public class DefensiveModeButton implements IStandardButton{
        @Override
        public String bind() {
            return "D";
        }

        @Override
        public String texture() {
            return "assets/ui/buttons/machine_gunner_defensive.png";
        }

        @Override
        public String identifier() {
            return "button_machine_gunner_defensive";
        }

        @Override
        public void action(Unit unit, IGameController gameController) {
            if (!hasTurnEnded){
                mode = false;
                endTurn();
            }
        }
    }

    public MachineGunner(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(10, 10, 1, 1, 3, 0, 20, 20, 0, 1, 2), new AnimationController(), "Heavy Infantry");

        Animation idle = new Animation(20);
        idle.addFrame("assets/units/machine_gunner_idle_1.png");
        idle.addFrame("assets/units/machine_gunner_idle_2.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/machine_gunner_fire_1.png");
        attack.addFrame("assets/units/machine_gunner_fire_2.png");
        attack.addFrame("assets/units/machine_gunner_fire_3.png");
        attack.addFrame("assets/units/machine_gunner_fire_1.png");
        attack.addFrame("assets/units/machine_gunner_fire_2.png");
        attack.addFrame("assets/units/machine_gunner_fire_3.png");
        attack.addFrame("assets/units/machine_gunner_fire_1.png");
        attack.addFrame("assets/units/machine_gunner_fire_2.png");
        attack.addFrame("assets/units/machine_gunner_fire_3.png");
        anim.addAnimation("attack", attack);
        anim.addShift("attack", "idle");
    }

    @Override
    public void update() {
        super.update();

        if (mode){
            df = 0;
            atk = 3;
        }
        else{
            df = 3;
            atk = 2;
        }

    }

    @Override
    public ArrayList<Unit.IButton> getButtons() {
        ArrayList<Unit.IButton> buttons = new ArrayList<>();
        if (supply > 5) {
            buttons.add(new AttackButton(attack_range));
            buttons.add(new MoveButton(movement_range));
            buttons.add(null);
        }
        else {
            buttons.add(new AttackButton(attack_range));
            buttons.add(new MoveButton(movement_range));
            for (int i = 0; i < 6; i++)
                buttons.add(null);
            buttons.add(new HPSacrificeSU());
        }

        if (!mode){
            buttons.set(2, new OffensiveModeButton());
            buttons.set(1, null);
        }
        else{
            buttons.set(2, new DefensiveModeButton());
        }
        return buttons;
    }
}
