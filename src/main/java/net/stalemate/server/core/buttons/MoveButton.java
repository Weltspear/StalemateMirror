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
import net.stalemate.server.core.buttons.util.Unflippable;
import net.stalemate.server.core.util.IGameController;

public class MoveButton implements Unit.ISelectorButton {
    private final int move_range;
    private final Unit.Layer l;

    public MoveButton(int move_range){
        this.move_range = move_range;
        l = Unit.Layer.GROUND;
    }

    public MoveButton(int move_range, Unit.Layer layer){
        this.move_range = move_range;
        l = layer;
    }

    @Override
    public String bind() {
        return "M";
    }

    @Override
    public String texture() {
        return "assets/ui/buttons/move_button.png";
    }

    @Override
    public String identifier() {
        return "button_move";
    }

    @Override
    public void action(int x, int y, Unit unit, IGameController gameController) {
        if (!unit.hasTurnEnded() & unit.unitStats().getSupply() - 1 > 0)
        if ((x != unit.getX()) || (y != unit.getY())) {
            if (gameController.getMapObject(x, y).isPassable) {
                boolean isPassable = true;
                for (Entity entity : gameController.getEntities(x, y)) {
                    if (!entity.isPassable()
                            && !(!(entity instanceof AirUnit) && unit instanceof AirUnit)
                            && !(entity instanceof AirUnit && !(unit instanceof AirUnit))) {
                        isPassable = false;
                        break;
                    }
                }
                if (isPassable) {

                    if ((unit.getX() - x) > 0 && !(unit instanceof Unflippable)){
                        unit.flip();
                    }
                    if ((unit.getX() - x) < 0 && !(unit instanceof Unflippable)){
                        unit.unflip();
                    }

                    unit.setX(x);
                    unit.setY(y);
                    unit.endTurn();
                    unit.consumeSupply(1);
                    unit.move();
                }
            }
        }
    }

    @Override
    public int selector_range() {
        return move_range;
    }

    @Override
    public String selector_texture() {
        return "assets/ui/selectors/ui_move.png";
    }

    @Override
    public Unit.Layer getLayer() {
        return l;
    }
}
