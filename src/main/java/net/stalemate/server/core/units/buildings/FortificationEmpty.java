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
import net.stalemate.server.core.buttons.util.Unflippable;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.units.Infantry;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.units.util.IConstructableBuilding;

public class FortificationEmpty extends Unit implements IBuilding, Unflippable, IConstructableBuilding {

    public static class LoadInfantryButton implements ISelectorButtonUnit{
        @Override
        public String bind() {
            return "L";
        }

        @Override
        public String texture() {
            return "assets/ui/buttons/load_fortification.png";
        }

        @Override
        public String identifier() {
            return "button_fortification_load_infantry";
        }

        @Override
        public void action(Unit selected_unit, Unit unit, Game gameController) {
            if (!selected_unit.hasTurnEnded() && selected_unit instanceof Infantry selected_unit_i){
                gameController.rmEntity(unit);
                gameController.rmEntity(selected_unit);
                Fortification f = new Fortification(unit.getX(), unit.getY(), gameController, selected_unit_i);
                f.setSupplyLevel((float)selected_unit.getSupply()/(float)selected_unit.getMaxSupply());
                gameController.getUnitsTeam(selected_unit).addUnit(f);
                f.endTurn();
                gameController.addEntity(f);
            }
        }

        @Override
        public int selector_range() {
            return 1;
        }

        @Override
        public String selector_texture() {
            return "assets/ui/selectors/ui_move.png";
        }

        @Override
        public boolean isUsedOnOurUnit() {
            return true;
        }

        @Override
        public boolean isUsedOnEnemy() {
            return true;
        }

        @Override
        public boolean isUsedOnAlliedUnit() {
            return false;
        }
    }

    public FortificationEmpty(int x, int y, Game game) {
        super(x, y, game, new UnitStats(15, 15, 0, 0, 0, 0, 0, -1, 2, 0, 0), new AnimationController(), "Fortification");

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/fortification.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");

        move_amount = -1;
    }

    @Override
    public IButton[] getButtons() {
        return getButtonsEnemy();
    }

    @Override
    public IButton[] getButtonsEnemy() {
        IButton[] buttons = new IButton[9];
        buttons[0] = new LoadInfantryButton();
        return buttons;
    }

    @Override
    public AnimationController underConstructionAC() {
        AnimationController animationController = new AnimationController();
        Animation idle = new Animation(1);
        idle.addFrame("assets/units/fortification.png");
        animationController.addAnimation("idle", idle);
        animationController.setCurrentAnimation("idle");
        return animationController;
    }
}
