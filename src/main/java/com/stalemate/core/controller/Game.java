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

package com.stalemate.core.controller;

import com.stalemate.core.Entity;
import com.stalemate.core.MapObject;
import com.stalemate.core.Unit;
import com.stalemate.core.event.EventListener;
import com.stalemate.core.event.EventListenerRegistry;
import com.stalemate.core.event.OnEvent;
import com.stalemate.core.gamemode.IGamemode;
import com.stalemate.core.gamemode.gamemodes.Versus;
import com.stalemate.core.properties.EntryTable;
import com.stalemate.core.units.buildings.MilitaryTent;
import com.stalemate.core.util.IGameControllerGamemode;
import com.stalemate.core.util.IUnitTeam;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Game implements IGameControllerGamemode {
    final ArrayList<Entity> entities = new ArrayList<>();
    ArrayList<ArrayList<MapObject>> map = new ArrayList<>();
    ArrayList<Team> teams = new ArrayList<>();
    final ArrayList<Team> already_assigned_teams = new ArrayList<>();
    private int team_doing_turn = 0;
    private volatile boolean entity_update_unsafe = false;
    private volatile boolean tbunsafe; // to be added / to be removed unsafe

    // Additional map parameters
    private HashMap<String, Object> aparams = new HashMap<>();
    public HashMap<String, Object> getAparams(){return aparams;}

    @Deprecated
    public enum Mode{
    }
    private IGamemode mode = new Versus();

    @SuppressWarnings("unused")
    public IGamemode getMode() {
        return mode;
    }

    public Team getTeamDoingTurn(){
        try {
            return teams.get(team_doing_turn);
        } catch (Exception e){
            return null;
        }
    }

    final ArrayList<Entity> to_be_removed = new ArrayList<>();
    final ArrayList<Entity> to_be_added = new ArrayList<>();

    public static class Team implements IUnitTeam {
        protected final Color teamColor;
        protected final ArrayList<Unit> units = new ArrayList<>();
        protected final ArrayList<Unit> to_be_added = new ArrayList<>();
        protected final boolean isTeamUncontrolled;
        protected boolean hasEndedItsTurn = false;
        protected EntryTable additional_params = new EntryTable();

        public EntryTable getAdditionalParams() {
            return additional_params;
        }

        /***
         * For testing purposes only
         */
        protected boolean AUTO_SKIP_TURN = false;

        /***
         * Ignore this team in victory calculation
         */
        protected final boolean isNeutral = false;

        @Deprecated
        public boolean isNeutral() {
            return isNeutral;
        }

        /***
         * For testing purposes only
         */
        public void setDev(){
            AUTO_SKIP_TURN = true;
        }

        public Team(Color teamColor){
            this.teamColor = teamColor;
            isTeamUncontrolled = true;
        }

        @Override
        public synchronized ArrayList<Unit> getTeamUnits() {
            return units;
        }

        public synchronized boolean endedTurn(){return hasEndedItsTurn;}

        public synchronized void endTurn(){hasEndedItsTurn = true;}

        public synchronized void addUnit(Unit u){
            to_be_added.add(u);
        }

        public synchronized void update(){
            units.addAll(to_be_added);
            to_be_added.clear();
        }

        public Color getTeamColor() {
            return teamColor;
        }

        protected int mp = 0;

        public synchronized void setMilitaryPoints(int mp){
            this.mp = mp;
        }

        public int getMilitaryPoints() {
            return mp;
        }
    }

    /***
     * It means method can be thread unsafe or cause a <code>ConcurrentModificationException</code> inside a <code>Game</code> class
     */
    public @interface GameUnsafe{

    }

    public static class CombatTracker implements EventListener {
        private int x_last_combat = -1;
        private int y_last_combat = -1;

        @OnEvent(type = OnEvent.EventType.ON_UNIT_ATTACK)
        public void onUnitAttack(Unit attacker, Unit attacked_unit){
            x_last_combat = attacker.getX();
            y_last_combat = attacker.getY();
        }

        public int getX() {
            return x_last_combat;
        }

        public int getY() {
            return y_last_combat;
        }
    }

    private final EventListenerRegistry evReg = new EventListenerRegistry();
    private final CombatTracker combatTracker = new CombatTracker();

    public EventListenerRegistry getEvReg() {
        return evReg;
    }

    public CombatTracker getCombatTracker() {
        return combatTracker;
    }

    @Deprecated
    public Game(){
        for (int i = 0; i < 50; i++){
            map.add(new ArrayList<>());
            for (int a = 0; a < 50; a++){
                map.get(i).add(new MapObject("assets/default.png", true));
            }
        }

        teams.add(new Team(Color.RED));
        teams.add((new Team(Color.BLUE)));

        Unit base1 = new MilitaryTent(1, 1, this);
        Unit base2 = new MilitaryTent(48, 48, this);

        teams.get(0).addUnit(base1);
        teams.get(1).addUnit(base2);

        forceAddEntity(base1);
        forceAddEntity(base2);

        evReg.addEventListener(combatTracker);
    }

    /***
     * Marks a method as a method which must be used only by <code>MapLoader</code>
     */
    public @interface MapLoaderMethod{}

    public Game(ArrayList<ArrayList<MapObject>> map, ArrayList<Team> teams, HashMap<String, Object> aparams){
        this.map = map;
        this.teams = teams;
        this.aparams = aparams;
    }

    @MapLoaderMethod
    public void setMode(IGamemode mode){this.mode = mode;}

    @GameUnsafe
    public synchronized ArrayList<Entity> getAllEntities(){
        while (entity_update_unsafe) {
            Thread.onSpinWait();
        }
        return entities;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<ArrayList<MapObject>> getMap(){
        return (ArrayList<ArrayList<MapObject>>) map.clone();
    }

    @SuppressWarnings("unchecked")
    public synchronized ArrayList<Entity> getAllEntitiesCopy(){
        while (entity_update_unsafe) {
            Thread.onSpinWait();
        }
        return (ArrayList<Entity>) entities.clone();
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Team> getTeams(){
        while (entity_update_unsafe) {
            Thread.onSpinWait();
        }
        return (ArrayList<Team>) teams.clone();
    }

    @SuppressWarnings("unchecked")
    public synchronized void update(){
        tbunsafe = true;
        entities.removeAll(to_be_removed);
        to_be_removed.clear();
        entities.addAll(to_be_added);
        to_be_added.clear();
        tbunsafe = true;

        entity_update_unsafe = true;
        for (Entity entity: entities){
            if (entity instanceof Entity.ServerUpdateTick){
                ((Entity.ServerUpdateTick) entity).update();
            }
        }
        entity_update_unsafe = false;

        if (teams.get(team_doing_turn).endedTurn() || teams.get(team_doing_turn).AUTO_SKIP_TURN){
            ArrayList<Unit> to_remove_dead = new ArrayList<>();

            for (Unit unit : teams.get(team_doing_turn).units){
                if (unit.unitStats().getHp() > 0) {
                    if (((ArrayList<Entity>)(entities.clone())).contains(unit)) {
                        unit.endTurn();
                        unit.turnUpdate();
                    }
                } else {
                    to_remove_dead.add(unit);
                }
            }
            teams.get(team_doing_turn).units.removeAll(to_remove_dead);
            teams.get(team_doing_turn).setMilitaryPoints(teams.get(team_doing_turn).getMilitaryPoints() + 1);
            team_doing_turn++;
        }

        if (team_doing_turn == teams.size()){
            for (Team team : teams){
                team.hasEndedItsTurn = false;
                for (Unit unit: team.getTeamUnits()){
                    unit.resetTurn();
                }
            }
            team_doing_turn = 0;
        }
        try {
            Thread.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Team team: teams){
            team.update();
        }
    }

    @Override
    public synchronized void rmEntity(Entity entity) {
        while (!tbunsafe) {
            Thread.onSpinWait();
        }
        to_be_removed.add(entity);
    }

    public synchronized void addEntity(Entity entity){
        while (!tbunsafe) {
            Thread.onSpinWait();
        }
        to_be_added.add(entity);
    }

    /***
     * Can be thread unsafe
     */
    @GameUnsafe
    public synchronized void forceAddEntity(Entity entity){
        entities.add(entity);
    }

    /***
     * Can be thread unsafe
     */
    @GameUnsafe
    @Deprecated
    public synchronized void forceRemoveEntity(Entity entity){
        entities.remove(entity);
    }

    public synchronized MapObject getMapObject(int x, int y){
        try {
            return map.get(y).get(x);
        } catch (Exception e){
            return null;
        }
    }

    @Override
    public synchronized ArrayList<Entity> getEntities(int x, int y) {
        while (entity_update_unsafe) {
            Thread.onSpinWait();
        }
        ArrayList<Entity> ent_clone = this.getAllEntitiesCopy();
        ArrayList<Entity> ent = new ArrayList<>();

        for (Entity entity : ent_clone) {
            if (entity.getX() == x & entity.getY() == y) {
                ent.add(entity);
            }
        }

        return ent;
    }

    public synchronized Team getUnassignedTeam(){
        for (Team team : teams){
            if (!already_assigned_teams.contains(team)){
                already_assigned_teams.add(team);
                return team;
            }
        }
        return null;
    }

    public int getSizeY(){
        return map.size();
    }

    public int getSizeX(int y){
        return map.get(y).size();
    }

    public Team getUnitsTeam(Unit u){
        for (Team team: teams){
            if (team.getTeamUnits().contains(u)){
                return team;
            }
        }
        return null;
    }

    @Override
    public int getMapWidth() {
        if (getMapHeight() >= 1){
            return map.get(0).size();
        }
        return 0;
    }

    @Override
    public int getMapHeight() {
        return map.size();
    }

    // Victory related code

    public boolean hasGameEnded(){
        return mode.hasGameEnded(this);
    }

    public Team getVictoriousTeam(){
        return mode.getVictoriousTeam(this);
    }
}
