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
import net.stalemate.server.core.buttons.RecoverButton;
import net.stalemate.server.core.buttons.util.NoMoveAttack;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IUnitName;
import net.stalemate.server.core.controller.Game;

public class MachineGunner extends Unit implements NoMoveAttack, IUnitName {

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
        public void action(Unit unit, Game gameController) {
            if (!hasTurnEnded){
                mode = true;
                attack_range--;
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
        public void action(Unit unit, Game gameController) {
            if (!hasTurnEnded){
                mode = false;
                attack_range++;
                endTurn();
            }
        }
    }

    public MachineGunner(int x, int y, Game game) {
        super(x, y, game, new UnitStats(12, 12, 1, 1, 3, 0, 20, 20, 0, 1, 2), new AnimationController(), "Heavy Infantry");

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

        move_amount = 2;
        turn_move_amount = 2;
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
    public IButton[] getButtons() {
        IButton[] buttons = new IButton[9];
        buttons[0] = new AttackButton(attack_range);
        buttons[1] = new MoveButton(movement_range);
        if (supply <= 5) {
            buttons[8] = new HPSacrificeSU();
        }

        if (!mode){
            buttons[2] = new OffensiveModeButton();
            buttons[1] = null;
        }
        else{
            buttons[2] = new DefensiveModeButton();
        }

        if (hp < max_hp && supply > 3){
            buttons[8] = new RecoverButton();

        }
        return buttons;
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();

        if (uname.isEmpty()){
            uname = game.getUnitNameGen().genName(name);
        }

        IUnitName.addNameProperty(uname, p);
        return p;
    }

    private String uname = "";

    @Override
    public String getUnitName() {
        return uname;
    }

    @Override
    public void setUnitName(String n) {
        uname = n;
    }
}
