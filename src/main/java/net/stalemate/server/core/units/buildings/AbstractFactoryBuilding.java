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

import net.stalemate.server.core.Entity;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.units.util.IUnitName;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.util.PriorityTurnUpdate;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;

/***
 * This building produces other units
 */
public abstract class AbstractFactoryBuilding extends Unit implements IBuilding, PriorityTurnUpdate {
    public static class UnitProductionTime{
        public final Unit unit;
        public int time_in_production;

        public UnitProductionTime(Unit unit, int time_in_production){
            this.unit = unit;
            this.time_in_production = time_in_production;
        }

    }

    final ArrayDeque<UnitProductionTime> production_queue = new ArrayDeque<>();
    UnitProductionTime currently_processed_unit = null;

    protected int deployment_x = 1;
    protected int deployment_y = 0;

    /***
     * This is a button which adds a unit to queue
     */
    public abstract class TrainButton implements IStandardButton{
        protected final int productionTime;
        protected final int cost;

        private final Class<?> unitclass;

        /***
         * @param productionTime in turns
         */
        public TrainButton(Class<?> toProduce, int productionTime, int cost){
            unitclass = toProduce;
            this.productionTime = productionTime;
            this.cost = cost;
        }

        @Override
        public void action(Unit unit, Game gameController) {
            if (gameController.getUnitsTeam(AbstractFactoryBuilding.this).getMilitaryPoints() - cost >= 0) {
                gameController.getUnitsTeam(AbstractFactoryBuilding.this).setMilitaryPoints(gameController.getUnitsTeam(unit).getMilitaryPoints() - cost);
                try {
                    AbstractFactoryBuilding.this.addUnitToQueue((Unit) unitclass.getConstructor(int.class, int.class, Game.class).newInstance(unit.getX() + deployment_x, unit.getY() + deployment_y, gameController), productionTime);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public abstract class CancelTrain implements IStandardButton{
        @Override
        public void action(Unit unit, Game gameController) {
            if (AbstractFactoryBuilding.this.production_queue.size() > 0){
                production_queue.removeLast();
            }
            else if (AbstractFactoryBuilding.this.currently_processed_unit != null){
                AbstractFactoryBuilding.this.currently_processed_unit = null;
            }
        }
    }

    /***
     * This button uses default texture and bind.
     */
    public class DefaultCancelButton extends CancelTrain{
        @Override
        public String bind() {
            return "C";
        }

        @Override
        public String texture() {
            return "assets/ui/buttons/scrap_unit_queue.png";
        }

        @Override
        public String identifier() {
            return "button_cancel_train";
        }
    }

    public abstract class ChangeDeploymentPoint implements ISelectorButton{
        @Override
        public int selector_range() {
            return 1;
        }

        @Override
        public void action(int x, int y, Unit unit, Game gameController) {
            if (x == AbstractFactoryBuilding.this.x && y == AbstractFactoryBuilding.this.y){
                return;
            }

            int d_x = x - AbstractFactoryBuilding.this.x;
            int d_y = y - AbstractFactoryBuilding.this.y;

            AbstractFactoryBuilding.this.deployment_x = d_x;
            AbstractFactoryBuilding.this.deployment_y = d_y;
        }

        @Override
        public boolean canBeUsedWhenOtherTeamsTurn(){
            return true;
        }
    }

    /***
     * This button uses default texture and bind.
     */
    public class DefaultChangeDeploymentPointButton extends ChangeDeploymentPoint{
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
            return "assets/ui/buttons/set_deployment_point.png";
        }

        @Override
        public String identifier() {
            return "button_set_deployment_point";
        }
    }

    public AbstractFactoryBuilding(int x, int y, Game game, UnitStats unitStats, AnimationController anim, String name) {
        super(x, y, game, unitStats, anim, name);
        move_amount = -1;
    }

    public void addUnitToQueue(Unit unit, int production_time){
        if (production_queue.size() != 8) {
            production_queue.add(new UnitProductionTime(unit, production_time));
        }
    }

    protected void deploy(){
        if (currently_processed_unit != null){
            if (currently_processed_unit.time_in_production != 0)
            currently_processed_unit.time_in_production -= 1;
            currently_processed_unit.unit.setX(x+deployment_x);
            currently_processed_unit.unit.setY(y+deployment_y);
            if (currently_processed_unit.time_in_production <= 0){
                boolean isBlocked = false;
                for (Entity entity: game.getEntities(x+deployment_x, y+deployment_y)){
                    if (entity instanceof Unit){
                        isBlocked = true;
                        break;
                    }
                }
                if (!(x+deployment_x < game.getMapWidth()) || x+deployment_x < 0){
                    isBlocked = true;
                }
                if (!(y+deployment_y < game.getMapHeight()) || y+deployment_y < 0){
                    isBlocked = true;
                }
                if (!game.getMapObject(x+deployment_x, y+deployment_y).isPassable()){
                    isBlocked = true;
                }

                if (!isBlocked) {
                    game.getUnitsTeam(this).addUnit(currently_processed_unit.unit);
                    if (currently_processed_unit.unit instanceof IUnitName uname){
                        uname.setUnitName(game.getUnitNameGen().genName(currently_processed_unit.unit.getName()));
                    }
                    game.addEntity(currently_processed_unit.unit);
                    currently_processed_unit = null;
                }
            }
        }
    }

    @Override
    public void update() {
        super.update();

        if (currently_processed_unit == null && !production_queue.isEmpty()){
            currently_processed_unit = production_queue.poll();
        }
    }

    @Override
    public void turnUpdate() {
        super.endTurn();
        deploy();
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();
        p.rm("ended_turn");
        return p;
    }

    public boolean checkForBlockage(){
        boolean isProductionBlocked = false;
        for (Entity entity: game.getEntities(x+deployment_x, y+deployment_y)){
            if (entity instanceof Unit){
                isProductionBlocked = true;
                break;
            }
        }
        return isProductionBlocked;
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
}
