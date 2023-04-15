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

package net.stalemate.server.core.units.air;

import net.stalemate.server.core.AirUnit;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.animation.Animation;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.buttons.MoveButton;
import net.stalemate.server.core.buttons.ResupplyButton;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.pathfinding.Pathfinding;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.units.util.IMechanized;

public class TransportPlane extends AirUnit implements IMechanized {

    public class LoadButton implements ISelectorButtonUnit {
        @Override
        public String bind() {
            return "L";
        }

        @Override
        public String texture() {
            return "texture_missing";
        }

        @Override
        public String identifier() {
            return "button_transport_plane_load_unit";
        }

        @Override
        public void action(Unit selected_unit, Unit unit, Game gameController) {
            if (loadedUnit == null && !hasTurnEnded && !(selected_unit instanceof IBuilding)){
                gameController.rmEntity(selected_unit);
                loadedUnit = selected_unit;
                endTurn();
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
            return false;
        }

        @Override
        public boolean isUsedOnAlliedUnit() {
            return false;
        }
    }


    public class DeployButton implements ISelectorButton{
        @Override
        public String bind() {
            return "D";
        }

        @Override
        public String texture() {
            return "texture_missing";
        }

        @Override
        public String identifier() {
            return "button_transport_plane_deploy";
        }

        @Override
        public void action(int x, int y, Unit unit, Game gameController) {
            if (!hasTurnEnded)
                if (Pathfinding.isCoordPassable(x, y, gameController) && loadedUnit != null) {
                    loadedUnit.setX(x);
                    loadedUnit.setY(y);
                    gameController.addEntity(loadedUnit);
                    loadedUnit = null;
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
    }

    private Unit loadedUnit = null;

    public TransportPlane(int x, int y, Game game) {
        super(x, y, game, new Unit.UnitStats(8, 8, 0, 2, 0, 0, 25, 30, 0, 0, 0), new AnimationController(), "Transport Plane");
        Animation animation = new Animation(1);
        animation.addFrame("assets/units/transport_plane_idle.png");
        anim.addAnimation("idle",animation);
        anim.setCurrentAnimation("idle");

        fog_of_war_range = 2;
        move_amount = 2;
        turn_move_amount = 2;

    }

    @Override
    public Unit.IButton[] getButtons() {
        IButton[] buttons = new IButton[9];
        buttons[0] = new MoveButton(movement_range, Layer.AIR);

        if (loadedUnit == null){
            buttons[6] = new LoadButton();
        }
        else
            buttons[6] = new DeployButton();

        buttons[8] = new ResupplyButton();
        buttons[7] = new ResupplyButton(Layer.AIR);
        return buttons;
    }

    @Override
    public void onDeath() {
        super.onDeath();
        if (loadedUnit != null) {
            loadedUnit.setHp(-1);
        }
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();
        if (loadedUnit != null)
            p.put("ldunit", loadedUnit.getName());
        return p;
    }
}
