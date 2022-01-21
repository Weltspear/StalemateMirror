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

package com.stalemate.core.units.buildings;

import com.stalemate.core.Entity;
import com.stalemate.core.Unit;
import com.stalemate.core.animation.Animation;
import com.stalemate.core.animation.AnimationController;
import com.stalemate.core.buttons.Scrap;
import com.stalemate.core.units.HeavyTank;
import com.stalemate.core.units.LightTank;
import com.stalemate.core.units.util.IConstructableBuilding;
import com.stalemate.core.util.IGameController;

import java.util.ArrayList;

public class TankFactory extends AbstractFactoryBuilding implements IConstructableBuilding {
    public TankFactory(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(15, 15, 0, 0, 0, 0, 0, -1, 1), new AnimationController(), "Tank Factory");

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/tank_factory.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
    }

    @Override
    public ArrayList<IButton> getButtons() {
        boolean isProductionBlocked = false;
        for (Entity entity: game.getEntities(x+deployment_x, y+deployment_y)){
            if (entity instanceof Unit){
                isProductionBlocked = true;
                break;
            }
        }
        ArrayList<IButton> buttons = new ArrayList<>();

        for (int i = 0; i < 9; i++){
            buttons.add(null);
        }

        if (!isProductionBlocked){
            buttons.set(0, new TrainButton(LightTank.class, 4, 6) {
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
            });
            buttons.set(1, new TrainButton(HeavyTank.class, 4, 7) {
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
            });
        }

        buttons.set(6, new CancelTrain() {
            @Override
            public String bind() {
                return "C";
            }

            @Override
            public String texture() {
                return "texture_missing.png";
            }

            @Override
            public String identifier() {
                return "button_cancel_train";
            }
        });
        buttons.set(7, new ChangeDeploymentPoint() {
            @Override
            public String selector_texture() {
                return "assets/ui/selectors/ui_move.png";
            }

            @Override
            public String bind() {
                return "D";
            }

            @Override
            public String texture() {
                return "texture_missing.png";
            }

            @Override
            public String identifier() {
                return "button_set_deployment_point";
            }
        });
        buttons.set(8, new Scrap());

        return buttons;
    }

    @Override
    public UnitQueue getUnitQueue() {
        UnitQueue queue = new UnitQueue();
        if (currently_processed_unit != null){
            queue.addQueueMember(new UnitQueue.QueueMember(currently_processed_unit.unit.getTextureFileName(), currently_processed_unit.time_in_production));
        }
        for (UnitProductionTime upt: production_queue) {
            queue.addQueueMember(new UnitQueue.QueueMember(upt.unit.getTextureFileName(), upt.time_in_production));
        }

        return queue;
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
