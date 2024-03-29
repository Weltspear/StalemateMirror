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

package net.stalemate.server.core.controller;

import net.libutils.etable.EntryTable;
import net.stalemate.server.core.Entity;
import net.stalemate.server.core.MapObject;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.event.EventListenerRegistry;
import net.stalemate.server.core.gamemode.IGamemode;
import net.stalemate.server.core.gamemode.gamemodes.Versus;
import net.stalemate.server.core.minimap.AttackTracker;
import net.stalemate.server.core.name.UnitNameGen;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.util.IUnitTeam;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Game {
    private final ArrayList<Entity> entities = new ArrayList<>();
    private final ArrayList<ArrayList<MapObject>> map;
    private final ArrayList<Team> teams;
    final ArrayList<Team> already_assigned_teams = new ArrayList<>();
    private int team_doing_turn = 0;
    public ReentrantLock lock = new ReentrantLock();

    // Additional map parameters
    private final HashMap<String, Object> aparams;
    private final UnitNameGen ng = new UnitNameGen();
    public HashMap<String, Object> getAparams(){return aparams;}

    @Deprecated
    public enum Mode{
    }
    private IGamemode mode = new Versus();

    private final AttackTracker attackTracker = new AttackTracker();

    @SuppressWarnings("unused")
    public IGamemode getMode() {
        return mode;
    }

    public UnitNameGen getUnitNameGen(){return ng;}

    public Team getTeamDoingTurn(){
        try {
            return teams.get(team_doing_turn);
        } catch (Exception e){
            return null;
        }
    }

    private final ArrayList<Entity> to_be_removed = new ArrayList<>();
    private final ArrayList<Entity> to_be_added = new ArrayList<>();

    public static class Team implements IUnitTeam {
        protected final Color teamColor;
        protected final ArrayList<Unit> units = new ArrayList<>();
        protected final ArrayList<Unit> to_be_added = new ArrayList<>();
        protected final ArrayList<Unit> to_be_removed = new ArrayList<>();
        protected String teamName;
        protected final boolean isTeamUncontrolled;
        protected boolean hasEndedItsTurn = false;
        protected EntryTable additional_params = new EntryTable();

        public EntryTable getAdditionalParams() {
            return additional_params;
        }

        protected boolean AUTO_SKIP_TURN = false;

        public void disableTurn(){
            AUTO_SKIP_TURN = true;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean getIsDisabledTurn(){
            return AUTO_SKIP_TURN;
        }

        public Team(Color teamColor){
            this.teamColor = teamColor;
            isTeamUncontrolled = true;
        }

        @Override
        public ArrayList<Unit> getTeamUnits() {
            return units;
        }

        public boolean endedTurn(){return hasEndedItsTurn;}

        public void endTurn(){hasEndedItsTurn = true;}

        public void addUnit(Unit u){
            to_be_added.add(u);
        }

        public void rmUnit(Unit u){
            to_be_removed.add(u);
        }

        public void update(){
            units.addAll(to_be_added);
            to_be_added.clear();
            units.removeAll(to_be_removed);
            to_be_removed.clear();
        }

        public Color getTeamColor() {
            return teamColor;
        }

        protected int mp = 2;

        public void setMilitaryPoints(int mp){
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

        public String getTeamName() {
            return teamName;
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }
    }

    /***
     * Team which automatically skips turns, and it should be ignored in victory calculations
     */
    public static class NeutralTeam extends Team{
        public NeutralTeam(Color c) {
            super(c);
            AUTO_SKIP_TURN = true;
            teamName = "neutral";
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

    private final EventListenerRegistry evReg = new EventListenerRegistry();

    public EventListenerRegistry getEvReg() {
        return evReg;
    }

    /***
     * Marks a method as a method which must be used only by <code>MapLoader</code>
     */
    public @interface MapLoaderMethod{}

    public Game(ArrayList<ArrayList<MapObject>> map, ArrayList<Team> teams, HashMap<String, Object> aparams){
        this.map = map;
        this.teams = teams;
        this.aparams = aparams;

        evReg.addEventListener(attackTracker);
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

            for (Team team : teams) {
                team.update();
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
                attackTracker.turnUpdate();
                evReg.triggerTeamTurnEnd(teams.get(team_doing_turn));
                team_doing_turn++;
            }

            if (team_doing_turn == teams.size()) {
                for (Team team : teams) {
                    team.hasEndedItsTurn = false;
                    for (Unit unit : team.getTeamUnits()) {
                        unit.allTeamTurnUpdate();
                    }
                }
                team_doing_turn = 0;
                mode.onTurnEnd();
            }

            mode.tick(this);

            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    public synchronized void rmEntity(Entity entity) {
        to_be_removed.add(entity);
    }

    public synchronized void addEntity(Entity entity){
        to_be_added.add(entity);
    }

    /***
     * Can cause ConcurrentModificationException
     */
    @GameUnsafe
    public synchronized void forceAddEntity(Entity entity){
        entities.add(entity);
    }

    /***
     * Can cause ConcurrentModificationException
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

    public AttackTracker getAttackTracker(){
        return attackTracker;
    }

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

    public int getMapWidth() {
        if (getMapHeight() >= 1){
            return map.get(0).size();
        }
        return 0;
    }

    public int getMapHeight() {
        return map.size();
    }

    // Victory related code

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasGameEnded(){
        return mode.hasGameEnded(this);
    }

    public Team getVictoriousTeam(){
        return mode.getVictoriousTeam(this);
    }

    /***
     * @return <code>ArrayList<code/> of entities which are going to be added to game in next tick.
     */
    public ArrayList<Entity> getEntitiesToBeAdded(){
        return to_be_added;
    }
}
