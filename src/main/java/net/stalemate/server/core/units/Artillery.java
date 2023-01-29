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

import net.stalemate.server.core.Entity;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.animation.Animation;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.buttons.AttackButton;
import net.stalemate.server.core.buttons.MotorizeButton;
import net.stalemate.server.core.buttons.MoveButton;
import net.stalemate.server.core.buttons.util.IUnitMoveAmount;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IMechanized;
import net.stalemate.server.core.util.IGameController;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Artillery extends Unit implements IMechanized, IUnitMoveAmount {
    @SuppressWarnings("all")
    public static class ShellingButton implements ISelectorButton{
        private final int attack_range;

        public ShellingButton(int attack_range){
            this.attack_range = attack_range;
        }

        @Override
        public String bind() {
            return "S";
        }

        @Override
        public String texture() {
            return "texture_missing.png";
        }

        @Override
        public String identifier() {
            return "button_artillery_shelling";
        }

        @Override
        public void action(int x, int y, Unit unit, IGameController gameController) {
            if (!unit.hasTurnEnded() && unit.unitStats().getSupply() - 5 > 0 && x != unit.getX() && y != unit.getY()) {
                ArrayList<int[]> coords_to_hit = new ArrayList<>();

                coords_to_hit.add(new int[]{x + 1, y});
                coords_to_hit.add(new int[]{x - 1, y});

                coords_to_hit.add(new int[]{x, y - 1});
                coords_to_hit.add(new int[]{x, y + 1});

                coords_to_hit.add(new int[]{x - 1, y - 1});
                coords_to_hit.add(new int[]{x + 1, y + 1});

                coords_to_hit.add(new int[]{x + 1, y - 1});
                coords_to_hit.add(new int[]{x - 1, y + 1});

                for (int[] coord : coords_to_hit) {
                    boolean doShelling = ThreadLocalRandom.current().nextBoolean();

                    if (doShelling){
                        for (Entity entity: gameController.getEntities(coord[0], coord[1])){
                            if (entity instanceof Unit u){
                                u.damage(2);
                            }
                        }
                    }
                }

                unit.endTurn();
                unit.consumeSupply(5);
                unit.getAnimationController().setCurrentAnimation("attack");
            }
        }

        @Override
        public int selector_range() {
            return attack_range;
        }

        @Override
        public String selector_texture() {
            return "assets/ui/selectors/ui_attack.png";
        }
    }

    public Artillery(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(5, 5, 3, 1, 2, 0, 15, 15, 0, 1, 2), new AnimationController(), "Artillery");

        Animation idle = new Animation(15);
        idle.addFrame("assets/units/artillery_idle.png");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/artillery_attack_1.png");
        attack.addFrame("assets/units/artillery_attack_2.png");
        attack.addFrame("assets/units/artillery_attack_3.png");
        attack.addFrame("assets/units/artillery_attack_4.png");

        anim.addAnimation("idle", idle);
        anim.addAnimation("attack", attack);
        anim.addShift("attack", "idle");

        anim.setCurrentAnimation("idle");

        fog_of_war_range = 3;
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();
        buttons.add(new AttackButton(attack_range));
        buttons.add(new MoveButton(movement_range));
        buttons.add(new MotorizeButton());
        // buttons.add(new ShellingButton(attack_range));
        return buttons;
    }

    private int move_amount = 2;

    @Override
    public void setMoveAmount(int m) {
        move_amount = m;
    }

    @Override
    public int getTurnMoveAmount() {
        return 2;
    }

    @Override
    public int getMoveAmount() {
        return move_amount;
    }

    @Override
    public void turnUpdate() {
        super.turnUpdate();
        setMoveAmount(getTurnMoveAmount());
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();
        IUnitMoveAmount.addMoveAmountProperty(move_amount, hasTurnEnded, p);
        return p;
    }
}
