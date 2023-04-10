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
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.units.air.*;
import net.stalemate.server.core.units.util.IBase;
import net.stalemate.server.core.units.util.IConstructableBuilding;
import org.jetbrains.annotations.Nullable;

public class Airport extends AbstractFactoryBuilding implements IConstructableBuilding, IBase {
    public Airport(int x, int y, Game game) {
         super(x, y, game, new UnitStats(10, 10, 0, 0, 0, 0, -1, -1, 0, 0, 0),
                 new AnimationController(), "Airport");
        Animation idle = new Animation(1);
        idle.addFrame("texture_missing");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");

        layer = Layer.AIR;
    }

    @Override
    public @Nullable IButton[] getButtons() {
        boolean isProductionBlocked = checkForBlockage();
        IButton[] buttons = new IButton[9];

        if (!isProductionBlocked) {
            buttons[0] = new TrainButton(Fighter.class, 1, 3) {
                @Override
                public String bind() {
                    return "F";
                }

                @Override
                public String texture() {
                    return "texture_missing";
                }

                @Override
                public String identifier() {
                    return "button_train_fighter";
                }
            };
            buttons[1] = new TrainButton(CloseAirSupportBomber.class, 2, 3) {
                @Override
                public String bind() {
                    return "B";
                }

                @Override
                public String texture() {
                    return "texture_missing";
                }

                @Override
                public String identifier() {
                    return "button_train_cas_bomber";
                }
            };
            buttons[2] = new TrainButton(HeavyFighter.class, 2, 3) {
                @Override
                public String bind() {
                    return "H";
                }

                @Override
                public String texture() {
                    return "texture_missing";
                }

                @Override
                public String identifier() {
                    return "button_train_heavy_fighter";
                }
            };
            buttons[3] = new TrainButton(StrategicBomber.class, 2, 3) {
                @Override
                public String bind() {
                    return "T";
                }

                @Override
                public String texture() {
                    return "texture_missing";
                }

                @Override
                public String identifier() {
                    return "button_train_strategic_bomber";
                }
            };
            buttons[4] = new TrainButton(TransportPlane.class, 2, 4) {
                @Override
                public String bind() {
                    return "P";
                }

                @Override
                public String texture() {
                    return "texture_missing";
                }

                @Override
                public String identifier() {
                    return "button_train_transport_plane";
                }
            };
        }

        buttons[6] = new DefaultCancelButton();
        buttons[7] = new DefaultChangeDeploymentPointButton();
        buttons[8] = new Scrap();
        return buttons;
    }

    @Override
    public AnimationController underConstructionAC() {
        AnimationController anim = new AnimationController();
        Animation idle = new Animation(1);
        idle.addFrame("texture_missing");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
        return anim;
    }
}