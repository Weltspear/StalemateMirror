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
import net.stalemate.server.core.buttons.RecoverButton;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.buildings.*;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.units.util.IConstructableBuilding;
import net.stalemate.server.core.units.util.IMechanized;
import net.stalemate.server.core.units.util.IUnitName;
import net.stalemate.server.core.controller.Game;

import java.lang.reflect.InvocationTargetException;

public class EngineerUnit extends Unit implements IUnitName{

    MotorizeButton motbutton = new MotorizeButton();

    public static class RepairButton implements ISelectorButtonUnit{
        @Override
        public String bind() {
            return "R";
        }

        @Override
        public String texture() {
            return "assets/ui/buttons/repair_button.png";
        }

        @Override
        public String identifier() {
            return "button_repair";
        }

        @Override
        public void action(Unit selected_unit, Unit unit, Game gameController) {
            if (unit.unitStats().getSupply() >= 14 && !unit.hasTurnEnded() && selected_unit != unit){
                if ((selected_unit instanceof IMechanized || selected_unit instanceof IBuilding) && selected_unit.unitStats().getMaxHp() != selected_unit.unitStats().getHp()){
                    selected_unit.setHp(selected_unit.getHp() + 3);
                    if (selected_unit.unitStats().getHp() > selected_unit.unitStats().getMaxHp()){
                        selected_unit.setHp(selected_unit.unitStats().getMaxHp());
                    }
                    unit.endTurn();
                    unit.consumeSupply(4);
                }
            }
        }

        @Override
        public int selector_range() {
            return 1;
        }

        @Override
        public String selector_texture() {
            return "assets/ui/selectors/ui_repair.png";
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
            return true;
        }
    }

    // Build menu things

    public static class BuildMenuButton implements IStandardButton{
        @Override
        public String bind() {
            return "B";
        }

        @Override
        public String texture() {
            return "assets/ui/buttons/build_yes.png";
        }

        @Override
        public String identifier() {
            return "button_engineer_unit_build_menu";
        }

        @Override
        public void action(Unit unit, Game gameController) {
            if (unit instanceof EngineerUnit eu){
                eu.isInBuildingMode = true;
            }
        }

        @Override
        public boolean canBeUsedWhenOtherTeamsTurn(){
            return true;
        }

    }

    public static class ExitBuildMenuButton implements IStandardButton{
        @Override
        public String bind() {
            return "B";
        }

        @Override
        public String texture() {
            return "assets/ui/buttons/build_no.png";
        }

        @Override
        public String identifier() {
            return "button_engineer_unit_build_menu_exit";
        }

        @Override
        public void action(Unit unit, Game gameController) {
            if (unit instanceof EngineerUnit eu){
                eu.isInBuildingMode = false;
            }
        }

        @Override
        public boolean canBeUsedWhenOtherTeamsTurn(){
            return true;
        }
    }

    public abstract static class ConstructBuildingButton implements ISelectorButton{
        private final Class<? extends Unit> b;
        private final int constructionTime;
        private final int cost;
        private final boolean isNeutral;

        public Class<? extends Unit> getUnitBuilt() {
            return b;
        }

        public int getConstructionTime() {
            return constructionTime;
        }

        public int getCost() {
            return cost;
        }

        public ConstructBuildingButton(Class<? extends Unit> building, int constructionTime, int cost, boolean isNeutral){
            b = building;
            this.constructionTime = constructionTime;
            this.cost = cost;
            this.isNeutral = isNeutral;
        }

        @Override
        public void action(int x, int y, Unit unit, Game gameController) {
            if (!unit.hasTurnEnded()) {

                for (Entity entity : gameController.getEntities(x, y)) {
                    if (entity instanceof Unit) {
                        return;
                    }
                }

                if (!(gameController.getUnitsTeam(unit).getMilitaryPoints()-cost >= 0)){
                    return;
                }

                Unit building = null;

                try {
                    building = b.getConstructor(int.class, int.class, Game.class).newInstance(x, y, gameController);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
                assert building != null;
                if (!isNeutral)
                    gameController.getUnitsTeam(unit).addUnit(building);
                else
                    gameController.getNeutralTeam().addUnit(building);

                if (!(building instanceof IConstructableBuilding)){
                    throw new IllegalArgumentException("Building must be constructable!");
                }

                UnderConstructionBuilding ucb = new UnderConstructionBuilding(x, y, gameController, building, constructionTime, ((IConstructableBuilding) building).underConstructionAC());
                if (!isNeutral)
                    gameController.getUnitsTeam(unit).addUnit(ucb);
                else
                    gameController.getNeutralTeam().addUnit(ucb);
                gameController.addEntity(ucb);

                gameController.getUnitsTeam(unit).setMilitaryPoints(gameController.getUnitsTeam(unit).getMilitaryPoints() - cost);

                unit.endTurn();

            }
        }

        @Override
        public int selector_range() {
            return 1;
        }
    }

    private boolean isInBuildingMode = false;

    public EngineerUnit(int x, int y, Game game) {
        super(x, y, game, new UnitStats(10, 10, 1, 1, 1, 0, 20, 20, 0, 0, 0), new AnimationController(), "Engineer");

        Animation idle = new Animation(20);
        idle.addFrame("assets/units/engineer_idle_1.png");
        idle.addFrame("assets/units/engineer_idle_2.png");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/engineer_fire_1.png");
        attack.addFrame("assets/units/engineer_fire_2.png");
        attack.addFrame("assets/units/engineer_fire_3.png");
        attack.addFrame("assets/units/engineer_fire_4.png");
        attack.addFrame("assets/units/engineer_fire_5.png");

        anim.addAnimation("idle", idle);
        anim.addAnimation("attack", attack);
        anim.addShift("attack", "idle");
        anim.setCurrentAnimation("idle");

        move_amount = 2;
        turn_move_amount = 2;
    }

    public void makeBuildingMode(IButton[] buttons){
        buttons[0] = (new ConstructBuildingButton(MilitaryTent.class, 1, 3, false) {
            @Override
            public String selector_texture() {
                return "assets/ui/selectors/ui_repair.png";
            }

            @Override
            public String bind() {
                return "M";
            }

            @Override
            public String texture() {
                return "assets/ui/buttons/build_base.png";
            }

            @Override
            public String identifier() {
                return "button_engineer_unit_build_menu_build_military_tent";
            }
        });
        buttons[1] = (new ConstructBuildingButton(TankFactory.class, 2, 3, false) {
            @Override
            public String selector_texture() {
                return "assets/ui/selectors/ui_repair.png";
            }

            @Override
            public String bind() {
                return "T";
            }

            @Override
            public String texture() {
                return "assets/ui/buttons/build_factory.png";
            }

            @Override
            public String identifier() {
                return "button_engineer_unit_build_menu_build_tank_factory";
            }
        });
        buttons[2] = (new ConstructBuildingButton(SupplyStation.class, 2, 3, false) {
            @Override
            public String selector_texture() {
                return "assets/ui/selectors/ui_repair.png";
            }

            @Override
            public String bind() {
                return "S";
            }

            @Override
            public String texture() {
                return "assets/ui/buttons/build_supply_station.png";
            }

            @Override
            public String identifier() {
                return "button_engineer_unit_build_menu_build_supply_station";
            }
        });
        buttons[3] = (new ConstructBuildingButton(FortificationEmpty.class, 2, 3, true) {
            @Override
            public String selector_texture() {
                return "assets/ui/selectors/ui_repair.png";
            }

            @Override
            public String bind() {
                return "F";
            }

            @Override
            public String texture() {
                return "assets/ui/buttons/build_fortification.png";
            }

            @Override
            public String identifier() {
                return "button_engineer_unit_build_menu_build_fortification";
            }
        });
        buttons[4] = (new ConstructBuildingButton(Radar.class, 1, 2, false) {
            @Override
            public String selector_texture() {
                return "assets/ui/selectors/ui_repair.png";
            }

            @Override
            public String bind() {
                return "A";
            }

            @Override
            public String texture() {
                return "assets/ui/buttons/build_radar.png";
            }

            @Override
            public String identifier() {
                return "button_engineer_unit_build_menu_build_radar";
            }
        });
        buttons[5] = (new ConstructBuildingButton(Factory.class, 2, 3, false) {
            @Override
            public String selector_texture() {
                return "assets/ui/selectors/ui_repair.png";
            }

            @Override
            public String bind() {
                return "E";
            }

            @Override
            public String texture() {
                return "assets/ui/buttons/build_factory2.png";
            }

            @Override
            public String identifier() {
                return "button_engineer_unit_build_menu_build_factory";
            }
        });
        buttons[6] = (new ConstructBuildingButton(Airport.class, 3, 3, false) {
            @Override
            public String selector_texture() {
                return "assets/ui/selectors/ui_repair.png";
            }

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
                return "button_engineer_unit_build_menu_build_airport";
            }
        });
        buttons[7] = (new ConstructBuildingButton(AntiAirBuilding.class, 2, 3, false) {
            @Override
            public String selector_texture() {
                return "assets/ui/selectors/ui_repair.png";
            }

            @Override
            public String bind() {
                return "R";
            }

            @Override
            public String texture() {
                return "texture_missing";
            }

            @Override
            public String identifier() {
                return "button_engineer_unit_build_menu_build_antiair";
            }
        });

        buttons[8] = new ExitBuildMenuButton();
    }

    @Override
    public IButton[] getButtons() {
        IButton[] buttons = new IButton[9];
        if (!isInBuildingMode) {
            buttons[0] = new MoveButton(movement_range);
            buttons[1] = new AttackButton(attack_range);
            buttons[2] = motbutton;
            buttons[3] = new RepairButton();

            buttons[8] = new BuildMenuButton();

            if (hp < max_hp && supply > 3){
                buttons[7] = new RecoverButton();
            }
        }
        else{
            makeBuildingMode(buttons);
        }

        return buttons;
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();

        if (uname.isEmpty()){
            uname = game.getUnitNameGen().genName(name);
        }

        IUnitName.addNameProperty(uname, p);
        return p;
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
