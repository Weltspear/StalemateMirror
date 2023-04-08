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
import net.stalemate.server.core.buttons.AttackButton;
import net.stalemate.server.core.buttons.MoveButton;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.gamemode.IGamemode;
import net.stalemate.server.core.gamemode.IGamemodeAI;
import net.stalemate.server.core.pathfinding.Pathfinding;
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

        public record AlreadySpawnedCoord(int x, int y){}

        public static class UnitAction {
            public enum Action{
                MOVE,
                ATTACK,
            }

            private ArrayList<Pathfinding.Node> path;
            private Action action;
            private Unit target;

            public UnitAction(Unit target, ArrayList<Pathfinding.Node> path){
                this.target = target;
                this.path = path;
                action = Action.ATTACK;
            }


            public UnitAction(ArrayList<Pathfinding.Node> path){
                this.target = null;
                this.path = path;
                action = Action.MOVE;
            }

            public void attack(Unit target, ArrayList<Pathfinding.Node> path){
                this.target = target;
                this.path = path;
                action = Action.ATTACK;
            }

            public void move(ArrayList<Pathfinding.Node> path){
                this.target = null;
                this.path = path;
                action = Action.MOVE;
            }
        }

        private int turnsDone = 5;

        private int waveSize = 1;

        private Unit targetBase = null;

        private final HashMap<Unit, UnitAction> actions = new HashMap<>();

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

        private int infantryAmount(){
            return (int) Math.ceil(((double) (waveSize))*1.5f);
        }

        private int infantryAmount2(){
            return (int) Math.ceil(-(waveSize/3f - 4)*(waveSize/3f - 4) + 15);
        }

        private int artilleryAmount(){
            return (int) Math.ceil(((double) (waveSize-3))*1.5f);
        }

        private int tankAmount(){
            return (int) Math.ceil(((double) (waveSize-4))*2f);
        }

        private int heavyTankAmount(){
            return (int) Math.ceil(waveSize-4);
        }

        private void switchToAttackMode(Unit actor, UnitAction uaction){

            boolean found = false;

            for (Entity entity: g.getAllEntities()){
                if (entity instanceof Unit u){
                    if (!t.getTeamUnits().contains(u)){
                        if (Math.abs(u.getX()-actor.getX()) < 10 && Math.abs(u.getY()-actor.getY()) < 10){
                            uaction.attack(u, Pathfinding.a_star(u.getX(), u.getY(), actor.getX(), actor.getY(), g, true));
                            found = true;
                            if (uaction.path != null)
                                if (uaction.path.size() > 0)
                                    break;
                        }
                    }
                }
            }

            if (!found){
                for (Entity entity: g.getAllEntities()){
                    if (entity instanceof Unit u){
                        if (!t.getTeamUnits().contains(u)){
                            uaction.attack(u, Pathfinding.a_star(u.getX(), u.getY(), actor.getX(), actor.getY(), g, true));
                            if (uaction.path != null)
                                if (uaction.path.size() > 0)
                                    break;
                        }
                    }
                }
            }
        }

        private void spawnUnits(SpawnRect rect, int uamount, ArrayList<AlreadySpawnedCoord> alreadySpawnedCoords, int type){
            if (t.getTeamUnits().size()+infantryAmount2()+artilleryAmount()+tankAmount() < 100)
            for (int i = 0; i < uamount; i++){
                AlreadySpawnedCoord sp = new AlreadySpawnedCoord(rect.x + RND.nextInt(rect.x2 - rect.x),
                        rect.y + RND.nextInt(rect.y2 - rect.y));

                Unit unit =  type == 1 ? new Infantry(sp.x, sp.y, g): type == 2 ? new Artillery(sp.x, sp.y, g)
                        : type == 3 ? new LightTank(sp.x, sp.y, g) : new HeavyTank(sp.x, sp.y, g);

                unit.setSupply(unit.getSupply()+20);

                actions.put(unit, new UnitAction(Pathfinding.a_star(targetBase.getX(), targetBase.getY(), unit.getX(), unit.getY(), g, true)));

                t.addUnit(unit);

                alreadySpawnedCoords.add(sp);
                g.addEntity(unit);
            }
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

                spawnUnits(rect, infantryAmount2(), alreadySpawnedCoords, 1);
                spawnUnits(rect, artilleryAmount(), alreadySpawnedCoords, 2);
                spawnUnits(rect, tankAmount(), alreadySpawnedCoords, 3);
                spawnUnits(rect, heavyTankAmount(), alreadySpawnedCoords, 4);

                waveSize++;
            }

            ArrayList<Unit> stageForRemoval = new ArrayList<>();

            for (Map.Entry<Unit, UnitAction> entry: actions.entrySet()){
                Unit actor = entry.getKey();

                if (actor.getHp() <= 0){
                    stageForRemoval.add(entry.getKey());
                    continue;
                }

                UnitAction uaction = entry.getValue();

                attackIfCan(actor);

                if (uaction.target != null){
                    if (uaction.target.getHp() <= 0){
                        switchToAttackMode(actor, uaction);
                    }
                }

                if (uaction.path != null) {
                    if (uaction.path.size() > 0) {

                        int nidx = actor.unitStats().getMovementRange()-1;

                        while (!actor.hasTurnEnded()) {
                            if (nidx >= uaction.path.size()){
                                nidx = uaction.path.size()-1;
                            }

                            Pathfinding.Node next = uaction.path.get(nidx);

                            if (Pathfinding.isCoordPassable(next.x, next.y, g)) {
                                int cur_move_amount = actor.getMoveAmount();

                                MoveButton moveButton = new MoveButton(1);

                                moveButton.action(next.x, next.y, entry.getKey(), g);

                                if (cur_move_amount > actor.getMoveAmount() || actor.hasTurnEnded()){
                                    uaction.path = new ArrayList<>(uaction.path.subList(nidx+1,uaction.path.size()));

                                    if (!actor.hasTurnEnded()){
                                        nidx = actor.unitStats().getMovementRange();
                                    }
                                }

                                attackIfCan(actor);
                            } else {
                                fixPath(actor, uaction);
                                break;
                            }

                            nidx--;
                        }
                    }
                    else {
                        switchToAttackMode(actor, uaction);
                    }
                }
                else {
                    fixPath(actor, uaction);
                }
            }

            for (Unit unit : stageForRemoval){
                actions.remove(unit);
            }

            turnsDone++;
        }

        private void fixPath(Unit actor, UnitAction uaction) {
            if (uaction.action == UnitAction.Action.MOVE) {

                uaction.path = Pathfinding.a_star(targetBase.getX(), targetBase.getY(),
                        actor.getX(), actor.getY(), g, true);
            }
            else{
                uaction.path = Pathfinding.a_star(uaction.target.getX(), uaction.target.getY(),
                        actor.getX(), actor.getY(), g, true);

            }
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
}
