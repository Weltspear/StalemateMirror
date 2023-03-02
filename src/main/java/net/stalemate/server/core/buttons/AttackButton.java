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
import net.stalemate.server.core.buttons.util.NoMoveAttack;
import net.stalemate.server.core.buttons.util.Invulnerable;
import net.stalemate.server.core.buttons.util.Unflippable;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.controller.Game;

public class AttackButton implements Unit.ISelectorButtonUnit { // todo: create more attack buttons for bombardment etc.
    protected final int attack_range;
    protected boolean anti_tank_mode = false;
    private final Unit.Layer l;

    public AttackButton(int attack_range){
        this.attack_range = attack_range;
        l = Unit.Layer.GROUND;
    }

    public AttackButton(int attack_range, Unit.Layer layer){
        this.attack_range = attack_range;
        l = layer;
    }

    public AttackButton enableAT(){
        anti_tank_mode = true;
        return this;
    }

    @Override
    public String bind() {
        return "A";
    }

    @Override
    public String texture() {
        return "assets/ui/buttons/attack_button.png";
    }

    @Override
    public String identifier() {
        return "button_attack";
    }

    private void flipProtector(Unit u, boolean flip){
        if (u.getProtector() != null){
            if (!(u instanceof Unflippable)){
                if (flip)
                    u.getProtector().flip();
                else
                    u.getProtector().unflip();
            }
        }
    }

    @Override
    public void action(Unit selected_unit, Unit unit, Game gameController) {
        if (!unit.hasTurnEnded() && unit.unitStats().getSupply() - 2 > 0 && !(selected_unit instanceof Invulnerable)){
            if (selected_unit.unitStats().getHp() > 0){
                unit.getAnimationController().setCurrentAnimation("attack");
                if (anti_tank_mode && selected_unit.getProtector() == null){
                    if (selected_unit.unitStats().getArmor() == 1){
                        selected_unit.damage(unit.getAtk() + 1);
                    } else if (selected_unit.unitStats().getArmor() == 2){
                        selected_unit.damage(unit.getAtk() + 3);
                    } else{
                        selected_unit.damage(unit.getAtk()-3);
                    }
                } else if (anti_tank_mode && selected_unit.getProtector() != null){
                    if (selected_unit.getProtector().unitStats().getArmor() == 1){
                        selected_unit.getProtector().damage(unit.getAtk() + 1);
                    } else if (selected_unit.getProtector().unitStats().getArmor() == 2){
                        selected_unit.getProtector().damage(unit.getAtk() + 3);
                    } else{
                        selected_unit.getProtector().damage(unit.getAtk()-3);
                    }
                }
                else if (selected_unit.getProtector() == null) {
                    selected_unit.damage(unit.getAtk());
                }
                else {
                    selected_unit.getProtector().damage(unit.getAtk());
                }

                gameController.getEvReg().triggerUnitAttackEvent(unit, selected_unit);

                if ((selected_unit.getX()-selected_unit.unitStats().getAttackRange() <= unit.getX()) &&
                        (selected_unit.getX()+selected_unit.unitStats().getAttackRange() >= unit.getX())) {
                    if ((selected_unit.getY()-selected_unit.unitStats().getAttackRange() <= unit.getY()) &&
                            (selected_unit.getY()+selected_unit.unitStats().getAttackRange() >= unit.getY())) {
                        if (selected_unit.getDf() > 0) {
                            unit.damage(selected_unit.getDf());
                            selected_unit.getAnimationController().setCurrentAnimation("attack");

                            if (selected_unit.getProtector() != null){
                                unit.damage(selected_unit.getProtector().getDf());
                                selected_unit.getProtector().getAnimationController().setCurrentAnimation("attack");
                            }
                        }
                    }
                }
                unit.endTurn();

                if ((unit.getX() - selected_unit.getX()) < 0 && !(selected_unit instanceof Unflippable)){
                    selected_unit.flip();
                    flipProtector(selected_unit, true);
                }
                if ((unit.getX() - selected_unit.getX()) > 0 && !(selected_unit instanceof Unflippable)){
                    selected_unit.unflip();
                    flipProtector(selected_unit, false);
                }

                if ((selected_unit.getX() - unit.getX()) < 0 && !(unit instanceof Unflippable)){
                    unit.flip();
                }
                if ((selected_unit.getX() - unit.getX()) > 0 && !(unit instanceof Unflippable)){
                    unit.unflip();
                }

                unit.consumeSupply(2);
                if (!(selected_unit instanceof IBuilding))
                selected_unit.consumeSupply(2);

                if (selected_unit.getProtector() != null && !(selected_unit instanceof IBuilding)){
                    selected_unit.getProtector().consumeSupply(2);
                }

                if (selected_unit.getEntrenchment()>0)
                selected_unit.setEntrenchment(selected_unit.getEntrenchment()-1);

                if (selected_unit.getHp() <= 0){
                    gameController.rmEntity(selected_unit);
                    if (Math.abs(selected_unit.getX() - unit.getX()) <= 1 && Math.abs(selected_unit.getY() - unit.getY()) <= 1 && !(unit instanceof NoMoveAttack)){
                        unit.setX(selected_unit.getX());
                        unit.setY(selected_unit.getY());

                        if (selected_unit.getMaxSupply() != -1 && selected_unit.getSupply() > 0){
                            unit.setSupply((int) (unit.getSupply() + (0.2*selected_unit.getSupply())));

                            if (unit.getSupply() > unit.getMaxSupply()){
                                unit.setSupply(unit.getMaxSupply());
                            }
                        }
                        unit.protectUnitWith(null);
                        unit.move();
                    }
                }
            }
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

    @Override
    public boolean isUsedOnOurUnit() {
        return false;
    }

    @Override
    public boolean isUsedOnEnemy() {
        return true;
    }

    @Override
    public boolean isUsedOnAlliedUnit() {
        return false;
    }

    @Override
    public Unit.Layer getLayer() {
        return l;
    }
}
