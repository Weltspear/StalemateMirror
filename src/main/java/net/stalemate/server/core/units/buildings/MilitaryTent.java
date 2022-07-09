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
import net.stalemate.server.core.units.*;
import net.stalemate.server.core.units.util.IBase;
import net.stalemate.server.core.units.util.IConstructableBuilding;
import net.stalemate.server.core.util.IGameController;

import java.util.ArrayList;

public class MilitaryTent extends AbstractFactoryBuilding implements IConstructableBuilding, IBase {

    public MilitaryTent(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(13, 13, 0, 0, 0, 0, 5, 5, 0, 0, 0), new AnimationController(), "Base");

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/building_military_tent.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
    }

    @Override
    public ArrayList<IButton> getButtons() {
        boolean isProductionBlocked = checkForBlockage();
        ArrayList<IButton> buttons = new ArrayList<>();

        for (int i = 0; i < 9; i++){
            buttons.add(null);
        }

        if (!isProductionBlocked){
            buttons.set(0, new TrainButton(Infantry.class, 2, 2) {
                @Override
                public String bind() {
                    return "I";
                }

                @Override
                public String texture() {
                    return "assets/ui/buttons/train_infantry_button.png";
                }

                @Override
                public String identifier() {
                    return "button_train_infantry";
                }
            });
            buttons.set(1, new TrainButton(Artillery.class, 3, 3) {
                @Override
                public String bind() {
                    return "A";
                }

                @Override
                public String texture() {
                    return "assets/ui/buttons/train_artillery.png";
                }

                @Override
                public String identifier() {
                    return "button_train_artillery";
                }
            });
            buttons.set(2, new TrainButton(AntiTank.class, 3, 3) {
                @Override
                public String bind() {
                    return "T";
                }

                @Override
                public String texture() {
                    return "assets/ui/buttons/train_anti_tank.png";
                }

                @Override
                public String identifier() {
                    return "button_train_anti_tank";
                }
            });
            buttons.set(3, new TrainButton(EngineerUnit.class, 1, 1) {
                @Override
                public String bind() {
                    return "E";
                }

                @Override
                public String texture() {
                    return "assets/ui/buttons/train_engineer_button.png";
                }

                @Override
                public String identifier() {
                    return "button_train_engineer_unit";
                }
            });
            buttons.set(4, new TrainButton(MotorizedUnit.class, 2, 2) {
                @Override
                public String bind() {
                    return "M";
                }

                @Override
                public String texture() {
                    return "assets/ui/buttons/train_motorized.png";
                }

                @Override
                public String identifier() {
                    return "button_train_motorized_unit_unit";
                }
            });
            buttons.set(5, new TrainButton(MachineGunner.class, 2, 3) {
                @Override
                public String bind() {
                    return "G";
                }

                @Override
                public String texture() {
                    return "assets/ui/buttons/train_machine_gunner.png";
                }

                @Override
                public String identifier() {
                    return "button_train_machine_gunner";
                }
            });
        }

        buttons.set(6, new DefaultCancelButton());
        buttons.set(7, new DefaultChangeDeploymentPointButton());
        buttons.set(8, new Scrap());

        return buttons;
    }

    @Override
    public void turnUpdate(){
        super.turnUpdate();
        this.supply -= 1;
    }

    @Override
    public AnimationController underConstructionAC() {
        AnimationController anim = new AnimationController();
        Animation idle = new Animation(1);
        idle.addFrame("assets/units/military_tent_build.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
        return anim;
    }
}
