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

package net.stalemate.core.controller;

import net.stalemate.core.Entity;
import net.stalemate.core.MapObject;
import net.stalemate.core.Unit;
import net.stalemate.core.event.EventListener;
import net.stalemate.core.event.EventListenerRegistry;
import net.stalemate.core.event.OnEvent;
import net.stalemate.core.gamemode.IGamemode;
import net.stalemate.core.gamemode.gamemodes.Versus;
import net.stalemate.core.properties.EntryTable;
import net.stalemate.core.units.util.IBuilding;
import net.stalemate.core.util.IGameController;
import net.stalemate.core.util.IGameControllerGamemode;
import net.stalemate.core.util.IUnitTeam;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Game implements IGameControllerGamemode {
    final ArrayList<Entity> entities = new ArrayList<>();
    ArrayList<ArrayList<MapObject>> map = new ArrayList<>();
    ArrayList<Team> teams = new ArrayList<>();
    final ArrayList<Team> already_assigned_teams = new ArrayList<>();
    private int team_doing_turn = 0;
    public ReentrantLock lock = new ReentrantLock();

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
        protected final ArrayList<Unit> to_be_removed = new ArrayList<>();
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

        public synchronized void rmUnit(Unit u){
            to_be_removed.add(u);
        }

        public synchronized void update(){
            units.addAll(to_be_added);
            to_be_added.clear();
            units.removeAll(to_be_removed);
            to_be_removed.clear();
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

        public int buildingCount(){
            int bc = 0;
            for (Unit u: units){
                if ((u instanceof IBuilding)){
                    bc++;
                }
            }
            return bc;
        }

        public int unitCount(){
            int uc = 0;
            for (Unit u: units){
                if (!(u instanceof IBuilding)){
                    uc++;
                }
            }
            return uc;
        }
    }

    /***
     * Team which automatically skips turns, and it should be ignored in victory calculations
     */
    public static class NeutralTeam extends Team{
        public NeutralTeam(Color c) {
            super(c);
            AUTO_SKIP_TURN = true;
        }
    }

    /***
     * Neutral team which is added to every instance of game when initialized
     */
    public static class StandardNeutralTeam extends NeutralTeam{
        public StandardNeutralTeam() {
            super(Color.WHITE);
        }
    }

    private final StandardNeutralTeam NEUTRAL;

    public StandardNeutralTeam getNeutralTeam(){
        return NEUTRAL;
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

    /***
     * Marks a method as a method which must be used only by <code>MapLoader</code>
     */
    public @interface MapLoaderMethod{}

    public Game(ArrayList<ArrayList<MapObject>> map, ArrayList<Team> teams, HashMap<String, Object> aparams){
        this.map = map;
        this.teams = teams;
        this.aparams = aparams;

        NEUTRAL = new StandardNeutralTeam();
        teams.add(NEUTRAL);
    }

    @MapLoaderMethod
    public void setMode(IGamemode mode){this.mode = mode;}

    @GameUnsafe
    public synchronized ArrayList<Entity> getAllEntities(){
        return entities;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<ArrayList<MapObject>> getMap(){
        return (ArrayList<ArrayList<MapObject>>) map.clone();
    }

    @SuppressWarnings("unchecked")
    public synchronized ArrayList<Entity> getAllEntitiesCopy(){
        return (ArrayList<Entity>) entities.clone();
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Team> getTeams(){
        return (ArrayList<Team>) teams.clone();
    }

    @SuppressWarnings("unchecked")
    public void update(){
        lock.lock();
        try {
            entities.removeAll(to_be_removed);
            to_be_removed.clear();
            entities.addAll(to_be_added);
            to_be_added.clear();

            for (Entity entity : entities) {
                if (entity instanceof Entity.ServerUpdateTick) {
                    ((Entity.ServerUpdateTick) entity).update();
                }
            }

            if (teams.get(team_doing_turn).endedTurn() || teams.get(team_doing_turn).AUTO_SKIP_TURN) {
                ArrayList<Unit> to_remove_dead = new ArrayList<>();

                for (Unit unit : teams.get(team_doing_turn).units) {
                    if (unit.unitStats().getHp() > 0) {
                        if (((ArrayList<Entity>) (entities.clone())).contains(unit)) {
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

            if (team_doing_turn == teams.size()) {
                for (Team team : teams) {
                    team.hasEndedItsTurn = false;
                    for (Unit unit : team.getTeamUnits()) {
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

            for (Team team : teams) {
                team.update();
            }

            mode.tick(this);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public synchronized void rmEntity(Entity entity) {
        to_be_removed.add(entity);
    }

    public synchronized void addEntity(Entity entity){
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
    public void forceRemoveEntity(Entity entity){
        entities.remove(entity);
    }

    public MapObject getMapObject(int x, int y){
        try {
            return map.get(y).get(x);
        } catch (Exception e){
            return null;
        }
    }

    @Override
    public synchronized ArrayList<Entity> getEntities(int x, int y) {
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
            if (!already_assigned_teams.contains(team) && !(team instanceof NeutralTeam)){
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

    /***
     * Thread safe Game class using lock
     */
    public class LockGame implements IGameController {

        @Override
        public void addEntity(Entity entity) {
            try {
                lock.lock();
                Game.this.addEntity(entity);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void rmEntity(Entity entity) {
            try {
                lock.lock();
                Game.this.rmEntity(entity);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public ArrayList<Entity> getEntities(int x, int y) {
            try {
                lock.lock();
                return Game.this.getEntities(x, y);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public MapObject getMapObject(int x, int y) {
            return Game.this.getMapObject(x, y);
        }

        @Override
        public ArrayList<Entity> getAllEntitiesCopy() {
            try {
                lock.lock();
                return Game.this.getAllEntitiesCopy();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public ArrayList<Entity> getAllEntities() {
            try{
                lock.lock();
                return Game.this.getAllEntities();
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        public Team getUnitsTeam(Unit u) {
            try{
                lock.lock();
                return Game.this.getUnitsTeam(u);
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        public int getMapWidth() {
            return Game.this.getMapHeight();
        }

        @Override
        public int getMapHeight() {
            return Game.this.getMapWidth();
        }

        @Override
        public EventListenerRegistry getEvReg() {
            try{
                lock.lock();
                return Game.this.getEvReg();
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        public StandardNeutralTeam getNeutralTeam() {
            try{
                lock.lock();
                return Game.this.getNeutralTeam();
            }
            finally {
                lock.unlock();
            }
        }

        public ArrayList<ArrayList<MapObject>> getMap() {
            return Game.this.map;
        }

        public ArrayList<Team> getTeams() {
            try{
                lock.lock();
                return Game.this.getTeams();
            } finally {
                lock.unlock();
            }
        }

        public Team getTeamDoingTurn() {
            try {
                lock.lock();
                return Game.this.getTeamDoingTurn();
            } finally {
                lock.unlock();
            }
        }
    }

    /***
     * Gets thread safe game
     */
    public LockGame getLockedGame(){
        return new LockGame();
    }
}
