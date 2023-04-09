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

package net.stalemate.server.core.ai;

import net.stalemate.server.core.Entity;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.buttons.AttackButton;
import net.stalemate.server.core.buttons.MoveButton;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.pathfinding.Pathfinding;
import net.stalemate.server.core.units.Artillery;
import net.stalemate.server.core.units.SelfPropelledArtillery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BattleGroup {
    private static final Random RND = new Random();

    ArrayList<Unit> attack_force = new ArrayList<>();
    ArrayList<Unit> artillery = new ArrayList<>();

    private HashMap<Unit, UnitOrder> orders = new HashMap<>();

    private boolean isAutonomous = false;

    private final Game g;
    private final Game.Team t;

    public enum BattleGroupOrder{
        //DEFEND,
        IDLE,
        ATTACK,
        //RETREAT
    }

    private BattleGroupOrder global_order = null;
    private int global_order_x;
    private int global_order_y;
    private boolean hasOrderChanged;

    public void attack(int x, int y){
        global_order = BattleGroupOrder.ATTACK;
        global_order_x = x;
        global_order_y = y;
        hasOrderChanged = true;
    }

    public BattleGroup(Game g, Game.Team t){
        this.g = g;
        this.t = t;
    }

    public interface UnitOrder{

    }

    public interface UnitOrderWithPath extends UnitOrder{
        ArrayList<Pathfinding.Node> getPath();
        void setPath(ArrayList<Pathfinding.Node> path);
    }

    public static class AttackOrder implements UnitOrderWithPath{
        public Unit target;
        public ArrayList<Pathfinding.Node> path;

        public void setPath(ArrayList<Pathfinding.Node> path) {
            this.path = path;
        }

        public ArrayList<Pathfinding.Node> getPath() {
            return path;
        }

        public AttackOrder(Unit target, ArrayList<Pathfinding.Node> path){
            this.target = target;
            this.path = path;
        }
    }


    public static class MoveOrder implements UnitOrderWithPath{
        public int gt_x;
        public int gt_y;
        public ArrayList<Pathfinding.Node> path;

        public void setPath(ArrayList<Pathfinding.Node> path) {
            this.path = path;
        }

        public ArrayList<Pathfinding.Node> getPath() {
            return path;
        }

        public MoveOrder(int gt_x, int gt_y, ArrayList<Pathfinding.Node> path){
            this.gt_x = gt_x;
            this.gt_y = gt_y;
            this.path = path;
        }
    }


    public static class FollowOrder implements UnitOrderWithPath{
        public int gt_x;
        public int gt_y;
        public ArrayList<Pathfinding.Node> path;
        public Unit target;

        public void setPath(ArrayList<Pathfinding.Node> path) {
            this.path = path;
        }

        public ArrayList<Pathfinding.Node> getPath() {
            return path;
        }

        public FollowOrder(int gt_x, int gt_y, ArrayList<Pathfinding.Node> path, Unit target){
            this.gt_x = gt_x;
            this.gt_y = gt_y;
            this.path = path;
            this.target = target;
        }
    }

    private boolean isCloseToOrder(int x, int y){
        return (global_order_x-x) * (global_order_x - x) + (global_order_y - y) * (global_order_y - y) <= 10*10;
    }

    @SuppressWarnings("unchecked")
    public void doTurn(){
        removeDeadUnits(attack_force);
        removeDeadUnits(artillery);

        if (hasOrderChanged){
            orders = new HashMap<>();
            hasOrderChanged = false;
        }

        if (attack_force.size() == 0){
            attack_force.addAll(artillery);
            artillery = new ArrayList<>();
        }

        if (global_order == BattleGroupOrder.ATTACK) {
            for (Unit unit : attack_force) {
                if (!orders.containsKey(unit)) {
                    if (isCloseToOrder(unit.getX(), unit.getY())){
                        orders.put(unit, switchToAttackMode(unit));
                    }
                    else{
                        orders.put(unit, new MoveOrder(global_order_x, global_order_y,
                                Pathfinding.a_star(global_order_x, global_order_y, unit.getX(), unit.getY(), g,
                                        true)));
                    }
                }
            }

            for (Unit unit : artillery){
                if (!orders.containsKey(unit)) {
                    int atk_force_idx = RND.nextInt(attack_force.size());

                    Unit attack_force_unit = attack_force.get(atk_force_idx);

                    orders.put(unit, new FollowOrder(attack_force_unit.getX(), attack_force_unit.getY(),
                            Pathfinding.a_star(attack_force_unit.getX(), attack_force_unit.getY(), unit.getX(), unit.getY(), g,
                                    true), attack_force_unit));
                }
            }
        }

        for (Map.Entry<Unit, UnitOrder> entry: ((HashMap<Unit, UnitOrder>)orders.clone()).entrySet()){
            Unit actor = entry.getKey();

            UnitOrder uorder = entry.getValue();

            attackIfCan(actor);

            if (uorder == null){
                if (isCloseToOrder(actor.getX(), actor.getY()) && !artillery.contains(actor)){
                    orders.put(actor, switchToAttackMode(actor));
                }
                else{
                    orders.put(actor, new MoveOrder(global_order_x, global_order_y,
                            Pathfinding.a_star(global_order_x, global_order_y, actor.getX(), actor.getY(), g,
                                    true)));
                }
            }

            if (uorder instanceof FollowOrder followOrder){
                if (followOrder.target.getHp() <= 0){
                    makeFollowOrder(actor);
                }
            }

            if (uorder instanceof AttackOrder attackOrder){
                if (attackOrder.target.getHp() <= 0){
                    uorder = switchToAttackMode(actor);
                    orders.put(actor, uorder);
                }
            }

            if (uorder instanceof UnitOrderWithPath uorder_path)
            if (uorder_path.getPath() != null) {
                if (uorder_path.getPath().size() > 0) {
                    moveThroughPath(actor, uorder_path);
                }
                else {
                    if (!artillery.contains(actor))
                        orders.put(actor, switchToAttackMode(actor));
                    else
                        orders.remove(actor);
                }
            }
            else {
                fixPath(actor, uorder_path);
            }
        }
    }

    public void makeFollowOrder(Unit actor){
        if (attack_force.size() != 0 && artillery.contains(actor)) {
            int atk_force_idx = RND.nextInt(attack_force.size());

            Unit attack_force_unit = attack_force.get(atk_force_idx);

            orders.put(actor, new FollowOrder(attack_force_unit.getX(), attack_force_unit.getY(),
                    Pathfinding.a_star(attack_force_unit.getX(), attack_force_unit.getY(), actor.getX(), actor.getY(), g,
                            true), attack_force_unit));
        } else{
            if (global_order == BattleGroupOrder.ATTACK){
                orders.put(actor, new MoveOrder(global_order_x, global_order_y,
                        Pathfinding.a_star(global_order_x, global_order_y, actor.getX(), actor.getY(), g,
                                true)));
            }
        }
    }

    private void removeDeadUnits(ArrayList<Unit> unitArrayList) {
        ArrayList<Unit> to_be_removed_artillery = new ArrayList<>();

        for (Unit unit : unitArrayList){
            if (unit.getHp() <= 0 || unit.getSupply() <= 0){
                to_be_removed_artillery.add(unit);
            }
        }

        unitArrayList.removeAll(to_be_removed_artillery);
    }

    private void attackIfCan(Unit actor){
        for (Entity entity: g.getAllEntities()){
            if (entity instanceof Unit u){
                if (!t.getTeamUnits().contains(u)){
                    if (Math.abs(u.getX()-actor.getX()) <= actor.unitStats().attack_range() && Math.abs(u.getY()-actor.getY()) <= actor.unitStats().attack_range()){
                        AttackButton attackButton = new AttackButton(3);

                        attackButton.action(u, actor, g);
                    }
                }
            }
        }
    }

    private void fixPath(Unit actor, UnitOrder uorder) {
        if (uorder instanceof MoveOrder moveOrder) {
            moveOrder.path = Pathfinding.a_star(moveOrder.gt_x, moveOrder.gt_y,
                    actor.getX(), actor.getY(), g, true);
        }
        else if (uorder instanceof AttackOrder attackOrder){
            attackOrder.path = Pathfinding.a_star(attackOrder.target.getX(), attackOrder.target.getY(),
                    actor.getX(), actor.getY(), g, true);
        }
        else if (uorder instanceof FollowOrder followOrder){
            fixFollowOrder(actor, followOrder);
        }
    }

    private UnitOrder switchToAttackMode(Unit actor){

        boolean found = false;

        for (Entity entity: g.getAllEntities()){
            if (entity instanceof Unit u){
                if (!t.getTeamUnits().contains(u)){
                    if (Math.abs(u.getX()-actor.getX()) < 10 && Math.abs(u.getY()-actor.getY()) < 10){
                        AttackOrder attackOrder = new AttackOrder(u, Pathfinding.a_star(u.getX(), u.getY(), actor.getX(), actor.getY(), g, true));
                        found = true;
                        if (attackOrder.path != null)
                            if (attackOrder.path.size() > 0)
                                return attackOrder;
                    }
                }
            }
        }

        if (!found && isAutonomous){
            for (Entity entity: g.getAllEntities()){
                if (entity instanceof Unit u) {
                    if (!t.getTeamUnits().contains(u)) {

                        global_order = BattleGroupOrder.ATTACK;
                        global_order_x = u.getX();
                        global_order_y = u.getY();


                        AttackOrder attackOrder = new AttackOrder(u, Pathfinding.a_star(u.getX(), u.getY(), actor.getX(), actor.getY(), g, true));
                        if (attackOrder.path != null)
                            if (attackOrder.path.size() > 0) {
                                return attackOrder;
                            }
                    }
                }
            }
        }

        return null;
    }

    public void addUnit(Unit unit){
        if (unit instanceof Artillery || unit instanceof SelfPropelledArtillery){
            artillery.add(unit);
        }
        else{
            attack_force.add(unit);
        }
    }

    public void fixFollowOrder(Unit actor, FollowOrder followOrder){
        if (followOrder.target.getX() != followOrder.gt_x ||
            followOrder.target.getY() != followOrder.gt_y){
            followOrder.path = Pathfinding.a_star(followOrder.target.getX(), followOrder.target.getY(),
                    actor.getX(), actor.getY(), g, true);
        }
    }

    private void moveThroughPath(Unit actor, UnitOrderWithPath uorder){
        int nidx = actor.unitStats().getMovementRange()-1;

        // it is possible that target will be away from (gt_x, gt_y)
        if (uorder instanceof FollowOrder followOrder){
            fixPath(actor, followOrder);
        }

        while (!actor.hasTurnEnded()) {
            if (uorder.getPath().size() == 0){
                fixPath(actor, uorder);
                if (uorder.getPath() != null){
                    if (uorder.getPath().size() == 0){
                        break;
                    }
                }
                else{
                    break;
                }
            }

            if (nidx >= uorder.getPath().size()){
                nidx = uorder.getPath().size()-1;
            }

            if (nidx < 0){
                break;
            }

            Pathfinding.Node next = uorder.getPath().get(nidx);

            if (Pathfinding.isCoordPassable(next.x, next.y, g)) {
                int cur_move_amount = actor.getMoveAmount();

                MoveButton moveButton = new MoveButton(1);

                moveButton.action(next.x, next.y, actor, g);

                if (cur_move_amount > actor.getMoveAmount() || actor.hasTurnEnded()){
                    uorder.setPath(new ArrayList<>(uorder.getPath().subList(nidx+1,uorder.getPath().size())));

                    if (!actor.hasTurnEnded()){
                        nidx = actor.unitStats().getMovementRange();
                    }
                }

                attackIfCan(actor);
            } else {
                fixPath(actor, uorder);
                break;
            }

            nidx--;
        }
    }

    public int getUnitAmount(){
        return artillery.size()+attack_force.size();
    }

    public void mergeWithOther(BattleGroup other){
        artillery.addAll(other.artillery);
        attack_force.addAll(other.attack_force);
    }

    /***
     * Enables BattleGroup's autonomous decision-making
     */
    public void makeAutonomous(){
        isAutonomous = true;
    }
}
