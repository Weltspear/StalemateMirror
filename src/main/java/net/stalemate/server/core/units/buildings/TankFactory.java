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

import net.stalemate.server.core.animation.Animation;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.buttons.Scrap;
import net.stalemate.server.core.units.HeavyTank;
import net.stalemate.server.core.units.LightTank;
import net.stalemate.server.core.units.Tankette;
import net.stalemate.server.core.units.util.IConstructableBuilding;
import net.stalemate.server.core.util.IGameController;

import java.util.ArrayList;

public class TankFactory extends AbstractFactoryBuilding implements IConstructableBuilding {
    public TankFactory(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(15, 15, 0, 0, 0, 0, -1, -1, 1, 0, 0), new AnimationController(), "Tank Factory");

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/tank_factory.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
    }

    @Override
    public IButton[] getButtons() {
        boolean isProductionBlocked = checkForBlockage();
        IButton[] buttons = new IButton[9];

        if (!isProductionBlocked){
            buttons[0] = new TrainButton(Tankette.class, 2, 2) {
                @Override
                public String bind() {
                    return "T";
                }

                @Override
                public String texture() {
                    return "assets/ui/buttons/train_tankette.png";
                }

                @Override
                public String identifier() {
                    return "button_train_tankette";
                }
            };
            buttons[1] = new TrainButton(LightTank.class, 2, 3) {
                @Override
                public String bind() {
                    return "L";
                }

                @Override
                public String texture() {
                    return "assets/ui/buttons/train_light_tank_button.png";
                }

                @Override
                public String identifier() {
                    return "button_train_light_tank";
                }
            };
            buttons[2] = new TrainButton(HeavyTank.class, 2, 3) {
                @Override
                public String bind() {
                    return "H";
                }

                @Override
                public String texture() {
                    return "assets/ui/buttons/train_heavy_tank.png";
                }

                @Override
                public String identifier() {
                    return "button_train_heavy_tank";
                }
            };
        }

        buttons[6] = new DefaultCancelButton();
        buttons[7] = new DefaultChangeDeploymentPointButton();
        buttons[8] = new Scrap();

        return buttons;
    }

    @Override
    public void turnUpdate(){
        super.turnUpdate();
    }

    @Override
    public AnimationController underConstructionAC() {
        AnimationController anim = new AnimationController();
        Animation idle = new Animation(1);
        idle.addFrame("assets/units/tank_factory_build.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
        return anim;
    }
}
