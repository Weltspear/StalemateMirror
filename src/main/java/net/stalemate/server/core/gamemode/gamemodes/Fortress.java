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

package net.stalemate.server.core.gamemode.gamemodes;

import net.stalemate.server.core.Entity;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.ai.BattleGroup;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.gamemode.IGamemode;
import net.stalemate.server.core.gamemode.IGamemodeAI;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.Artillery;
import net.stalemate.server.core.units.HeavyTank;
import net.stalemate.server.core.units.Infantry;
import net.stalemate.server.core.units.LightTank;
import net.stalemate.server.core.units.buildings.MilitaryTent;
import net.stalemate.singleplayer.AITurn;

import java.util.*;

public class Fortress implements IGamemode, IGamemodeAI, EventListener {

    private int turns_passed = 0;


    private record SpawnRect(int x, int y, int x2, int y2){

    }

    ArrayList<SpawnRect> spawnRects = null;

    FortressAI ai = null;


    @Override
    public void tick(Game g) {

    }

    @Override
    public boolean hasGameEnded(Game g) {
        return getVictoriousTeam(g)!=null;
    }

    private boolean hasAtLeastOneBase(Game.Team t){
        boolean b = false;

        for (Entity entity : t.getTeamUnits()){
            if (entity instanceof MilitaryTent){
                b = true;
                break;
            }
        }

        return b;
    }

    @Override
    public Game.Team getVictoriousTeam(Game g) {
        if (turns_passed >= 40) {
            for (Game.Team team : g.getTeams()) {
                if (!Objects.equals(team.getTeamName(), null) && !Objects.equals(team.getTeamName(), "neutral")
                    && !Objects.equals(team.getTeamName(), "fortressAI")) {
                    return team;
                }
            }
        }
        else{
            for (Game.Team team : g.getTeams()){
                if (!Objects.equals(team.getTeamName(), null) && !Objects.equals(team.getTeamName(), "neutral")
                        && !Objects.equals(team.getTeamName(), "fortressAI") && !hasAtLeastOneBase(team)) {
                    for (Game.Team team2 : g.getTeams()){
                        if (Objects.equals(team2.getTeamName(), "fortressAI")){
                            return team2;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String gmName() {
        return "Fortress";
    }

    public static class FortressAI implements AITurn{
        private final Game g;
        private final Game.Team t;
        private final ArrayList<SpawnRect> rects;

        private static final Random RND = new Random();

        ArrayList<BattleGroup> battleGroups = new ArrayList<>();

        public record AlreadySpawnedCoord(int x, int y){}

        private int turnsDone = 0;

        private int waveSize = 1;

        private Unit targetBase = null;

        public FortressAI(Game g, Game.Team t, ArrayList<SpawnRect> rects){
            this.g = g;
            this.t = t;
            this.rects = rects;

            for (Entity entity : g.getAllEntities()){
                if (entity instanceof MilitaryTent base){
                    if (!t.getTeamUnits().contains(base)){
                        targetBase = base;
                    }
                }
            }

            t.setTeamName("fortressAI");
        }

        private int infantryAmount2(){
            return (int) Math.ceil(-(waveSize/3f - 4)*(waveSize/3f - 4) + 15);
        }

        private int artilleryAmount(){
            return (int) Math.ceil(((double) (waveSize-3))*1.5f);
        }

        private int tankAmount(){
            return (int) Math.ceil(((double) (waveSize-5))*2f);
        }

        private int heavyTankAmount(){
            return (int) Math.ceil(waveSize-5);
        }

        private ArrayList<Unit> spawnUnits(SpawnRect rect, int uamount, ArrayList<AlreadySpawnedCoord> alreadySpawnedCoords, int type){
            ArrayList<Unit> units = new ArrayList<>();

            if (t.getTeamUnits().size()+infantryAmount2()+artilleryAmount()+tankAmount() < 100)
            for (int i = 0; i < uamount; i++){
                AlreadySpawnedCoord sp = new AlreadySpawnedCoord(rect.x + RND.nextInt(rect.x2 - rect.x),
                        rect.y + RND.nextInt(rect.y2 - rect.y));

                Unit unit =  type == 1 ? new Infantry(sp.x, sp.y, g): type == 2 ? new Artillery(sp.x, sp.y, g)
                        : type == 3 ? new LightTank(sp.x, sp.y, g) : new HeavyTank(sp.x, sp.y, g);

                unit.setSupply(unit.getSupply()+20);

                t.addUnit(unit);

                alreadySpawnedCoords.add(sp);
                g.addEntity(unit);

                units.add(unit);
            }

            return units;
        }

        @Override
        public void doTurn() {

            if (turnsDone == 5){
                turnsDone = 0;

                SpawnRect rect;

                if (rects.size() == 1) {
                    rect = rects.get(0);
                } else {
                    rect = rects.get(RND.nextInt(rects.size()));
                }

                ArrayList<AlreadySpawnedCoord> alreadySpawnedCoords = new ArrayList<>();

                ArrayList<Unit> spawnedUnits = new ArrayList<>();

                spawnedUnits.addAll(spawnUnits(rect, infantryAmount2(), alreadySpawnedCoords, 1));
                spawnedUnits.addAll(spawnUnits(rect, artilleryAmount(), alreadySpawnedCoords, 2));
                spawnedUnits.addAll(spawnUnits(rect, tankAmount(), alreadySpawnedCoords, 3));
                spawnedUnits.addAll(spawnUnits(rect, heavyTankAmount(), alreadySpawnedCoords, 4));

                Collections.shuffle(spawnedUnits);

                while (spawnedUnits.size() > 0){
                    BattleGroup battleGroup = new BattleGroup(g, t);
                    for (int i = 0; i < 10 && spawnedUnits.size() > 0; i++){
                        Unit u = spawnedUnits.get(0);
                        spawnedUnits.remove(0);

                        battleGroup.addUnit(u);
                    }
                    battleGroup.attack(targetBase.getX(), targetBase.getY());

                    battleGroups.add(battleGroup);
                }

                waveSize++;
            }

            ArrayList<BattleGroup> to_be_removed = new ArrayList<>();

            ArrayList<BattleGroup> to_be_merged = new ArrayList<>();

            for (BattleGroup battleGroup: battleGroups){
                if (battleGroup.getUnitAmount() == 0){
                    to_be_removed.add(battleGroup);
                }
                if (battleGroup.getUnitAmount() < 10){
                    to_be_merged.add(battleGroup);
                    to_be_removed.add(battleGroup);
                }
            }

            battleGroups.removeAll(to_be_removed);

            if (battleGroups.size() == 0){
                battleGroups.addAll(to_be_merged);
                to_be_merged.clear();
            }

            if (to_be_merged.size() > 0)
            for (BattleGroup battleGroup: battleGroups){
                if (to_be_merged.size() == 0){
                    break;
                }

                if (battleGroup.getUnitAmount() < 20){
                    BattleGroup m = to_be_merged.get(0);
                    to_be_merged.remove(m);
                    battleGroup.mergeWithOther(m);
                }
            }

            // if there are any left try to merge with any battlegroup
            if (to_be_merged.size() > 0)
                for (BattleGroup battleGroup: battleGroups){
                    if (to_be_merged.size() == 0){
                        break;
                    }

                    BattleGroup m = to_be_merged.get(0);
                    to_be_merged.remove(m);
                    battleGroup.mergeWithOther(m);
                }

            for (BattleGroup battleGroup: battleGroups){
                battleGroup.doTurn();
            }

            turnsDone++;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public AITurn getAI(Game g, Game.Team t) {
        if (spawnRects == null){
            if (g.getAparams().containsKey("spawnrects")){
                spawnRects = new ArrayList<>();
                ArrayList<ArrayList<Integer>> spawnrs = (ArrayList<ArrayList<Integer>>) g.getAparams().get("spawnrects");
                for (ArrayList<Integer> rect: spawnrs){
                    spawnRects.add(new SpawnRect(rect.get(0), rect.get(1), rect.get(2), rect.get(3)));
                }
            }
        }

        ai = new FortressAI(g, t, spawnRects);

        return ai;
    }

    @Override
    public void onTurnEnd() {
        turns_passed++;
    }


    @Override
    public boolean isSingleplayerExclusive() {
        return true;
    }

    public String fortressInfo(){
        return "Turn: " + turns_passed + ' ' + "Wave: " + (ai.waveSize-1);
    }

    @Override
    public Properties getProperties() {
        Properties properties = new Properties();
        properties.put("wave", "" + (ai.waveSize-1));
        properties.put("turn", "" + turns_passed);
        return properties;
    }
}
