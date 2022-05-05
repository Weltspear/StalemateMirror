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

package net.stalemate.core.units;

import net.stalemate.core.Entity;
import net.stalemate.core.Unit;
import net.stalemate.core.animation.Animation;
import net.stalemate.core.animation.AnimationController;
import net.stalemate.core.buttons.AttackButton;
import net.stalemate.core.buttons.MotorizeButton;
import net.stalemate.core.buttons.MoveButton;
import net.stalemate.core.units.buildings.*;
import net.stalemate.core.units.util.IConstructableBuilding;
import net.stalemate.core.units.util.IMechanized;
import net.stalemate.core.util.IGameController;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class EngineerUnit extends Unit {
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
        public void action(Unit selected_unit, Unit unit, IGameController gameController) {
            if (unit.unitStats().getSupply() >= 14 && !unit.hasTurnEnded() && selected_unit != unit){
                if (selected_unit instanceof IMechanized && selected_unit.unitStats().getMaxHp() != selected_unit.unitStats().getHp()){
                    selected_unit.setHp(selected_unit.getHp() + 3);
                    if (selected_unit.unitStats().getHp() > selected_unit.unitStats().getMaxHp()){
                        selected_unit.setHp(selected_unit.unitStats().getHp() - selected_unit.unitStats().getMaxHp());
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
        public void action(Unit unit, IGameController gameController) {
            if (unit instanceof EngineerUnit eu){
                eu.isInBuildingMode = true;
            }
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
        public void action(Unit unit, IGameController gameController) {
            if (unit instanceof EngineerUnit eu){
                eu.isInBuildingMode = false;
            }
        }
    }

    public abstract static class ConstructBuildingButton implements ISelectorButton{
        private final Class<? extends Unit> b;
        private final int constructionTime;
        private final int cost;
        private final boolean isNeutral;

        public ConstructBuildingButton(Class<? extends Unit> building, int constructionTime, int cost, boolean isNeutral){
            b = building;
            this.constructionTime = constructionTime;
            this.cost = cost;
            this.isNeutral = isNeutral;
        }

        @Override
        public void action(int x, int y, Unit unit, IGameController gameController) {
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
                    building = b.getConstructor(int.class, int.class, IGameController.class).newInstance(x, y, gameController);
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

    public EngineerUnit(int x, int y, IGameController game) {
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
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();
        if (!isInBuildingMode) {
            buttons.add(new MoveButton(movement_range));
            buttons.add(new AttackButton(attack_range));
            buttons.add(new MotorizeButton());
            buttons.add(new RepairButton());

            for (int i = 0; i < 4; i++){
                buttons.add(null);
            }
            buttons.add(new BuildMenuButton());
        }
        else{
            buttons.add(new ConstructBuildingButton(MilitaryTent.class, 3, 3, false) {
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
            buttons.add(new ConstructBuildingButton(TankFactory.class, 4, 3, false) {
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
            buttons.add(new ConstructBuildingButton(SupplyStation.class, 3, 4, false) {
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
            buttons.add(new ConstructBuildingButton(FortificationEmpty.class, 3, 4, true) {
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
            buttons.add(new ConstructBuildingButton(Radar.class, 4, 4, false) {
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

            for (int i = 0; i < 3; i++){
                buttons.add(null);
            }
            buttons.add(new ExitBuildMenuButton());
        }

        return buttons;
    }
}
