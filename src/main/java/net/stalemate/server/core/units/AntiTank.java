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
import net.stalemate.server.core.buttons.MotorizeButton;
import net.stalemate.server.core.buttons.MoveButton;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IMechanized;
import net.stalemate.server.core.units.util.IUnitName;
import net.stalemate.server.core.controller.Game;

public class AntiTank extends Unit implements IMechanized, IUnitName {

    MotorizeButton motbutton = new MotorizeButton();

    public AntiTank(int x, int y, Game game) {
        super(x, y, game, new UnitStats(6, 6, 2, 1, 4, 0, 18, 18, 0, 1, 2), new AnimationController(), "Anti-Tank");

        Animation idle = new Animation(2);
        idle.addFrame("assets/units/anti_tank_idle.png");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/anti_tank_attack_1.png");
        attack.addFrame("assets/units/anti_tank_attack_2.png");
        attack.addFrame("assets/units/anti_tank_attack_3.png");
        attack.addFrame("assets/units/anti_tank_attack_4.png");

        anim.addAnimation("idle", idle);
        anim.addAnimation("attack", attack);
        anim.addShift("attack", "idle");
        anim.setCurrentAnimation("idle");

        move_amount = 2;
        turn_move_amount = 2;
    }

    @Override
    public IButton[] getButtons() {
        IButton[] buttons = new IButton[9];

        buttons[0] = (new AttackButton(attack_range)).enableAT();
        buttons[1] = new MoveButton(movement_range);
        buttons[2] = motbutton;
        return buttons;
    }

    @Override
    public Properties getProperties() {
        Properties properties = new Properties();
        properties.put("name", this.getName());
        properties.put("hp", this.unitStats().getHp() + "/" + this.unitStats().getMaxHp());
        properties.put("su", this.unitStats().getSupply() + "/" + this.unitStats().getMaxSupply());
        properties.put("df", this.unitStats().getDf() + (entrenchment>1?"(+"+((int)(0.5*entrenchment))+")":""));
        properties.put("atk", (this.unitStats().getAtk() - 3) + (entrenchment>1?"(+"+((int)(0.5*entrenchment))+")":""));
        properties.put("atk_ar_1", (this.unitStats().getAtk() + 1) + (entrenchment>1?"(+"+((int)(0.5*entrenchment))+")":""));
        properties.put("atk_ar_2", (this.unitStats().getAtk() + 3) + (entrenchment>1?"(+"+((int)(0.5*entrenchment))+")":""));

        properties.put("atk_range", String.valueOf(this.unitStats().getAttackRange()));
        properties.put("mov_range", String.valueOf(this.unitStats().getMovementRange()));
        properties.put("move_amount", String.valueOf(move_amount));

        properties.put("ended_turn", this.hasTurnEnded ? "Yes": "No");
        if (this.entrenchment > 0)
            properties.put("et", String.valueOf(this.entrenchment));

        if (uname.isEmpty()){
            uname = game.getUnitNameGen().genName(name);
        }

        IUnitName.addNameProperty(uname, properties);
        return properties;
    }

    @Override
    public void turnUpdate() {
        super.turnUpdate();
        setMoveAmount(getTurnMoveAmount());
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

    @Override
    public Unit shiftSelectionOnRemoval() {
        if (hp > 0)
            return motbutton.getShift();
        else
            return null;
    }
}
