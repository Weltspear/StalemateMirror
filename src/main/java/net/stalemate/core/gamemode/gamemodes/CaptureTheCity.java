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

package net.stalemate.core.gamemode.gamemodes;

import net.stalemate.core.Entity;
import net.stalemate.core.Unit;
import net.stalemate.core.event.EventListener;
import net.stalemate.core.event.OnEvent;
import net.stalemate.core.gamemode.IGamemode;
import net.stalemate.core.units.HeavyTank;
import net.stalemate.core.units.Infantry;
import net.stalemate.core.units.LightTank;
import net.stalemate.core.util.IGameController;
import net.stalemate.core.util.IGameControllerGamemode;
import net.stalemate.core.controller.Game;

import java.util.ArrayList;
import java.util.HashMap;

// todo: make it ignore neutral team
public class CaptureTheCity implements IGamemode {
    public static class RebelUnit extends Infantry {

        public RebelUnit(int x, int y, IGameController game) {
            super(x, y, game);
            name = "Rebel";
        }
    }

    public class FlagEntity extends Entity{

        Game.Team capturing_team = null;
        int capture_progress = 0;
        int turns_held_full_capture = 0;

        public FlagEntity(int x, int y, IGameController game) {
            super(x, y, game);
            isInvisible = true;
        }

        @Override
        public String getTextureFileName() {
            return null;
        }

        @Override
        public void turnUpdate() {
            for (Entity e: game.getEntities(x, y)){
                if (e instanceof Unit u){
                    if (capturing_team == null){
                        capturing_team = game.getUnitsTeam(u);
                        capture_progress += 20;
                        CaptureTheCity.this.resistance += 5;
                    }
                    if (capturing_team == game.getUnitsTeam(u)){
                        if (capture_progress < 100){
                            capture_progress += 20;
                            CaptureTheCity.this.resistance += 3;
                        }
                    }
                    if (capturing_team != game.getUnitsTeam(u)){
                        if (capture_progress > 0){
                            capture_progress -= 20;
                            CaptureTheCity.this.resistance += 1;
                        }
                        if (capture_progress == 0){
                            capturing_team = game.getUnitsTeam(u);
                            capture_progress+=20;
                            CaptureTheCity.this.resistance += 3;
                        }
                    }
                }
            }

            if (capture_progress == 100){
                turns_held_full_capture+=1;
            }

            if (turns_held_full_capture == 10){
                CaptureTheCity.this.victorious_team = capturing_team;
            }
        }
    }

    public class CaptureTheCityEventListener implements EventListener{
        public int dcount = 0;

        @OnEvent(type = OnEvent.EventType.ON_UNIT_DEATH)
        public void onUnitDeath(Unit d){
            CaptureTheCity.this.resistance += 5;
            dcount++;
            if (dcount == 3){
                CaptureTheCity.this.rebel_count += 1;
                dcount = 0;
            }
        }

        @OnEvent(type = OnEvent.EventType.ON_UNIT_ATTACK)
        public void onUnitAttack(Unit attacker, Unit attacked){
            CaptureTheCity.this.resistance += 1;

            if (attacker instanceof LightTank){
                CaptureTheCity.this.resistance += 2;
            }
            if (attacker instanceof HeavyTank){
                CaptureTheCity.this.resistance += 4;
            }
        }

    }

    private boolean isInited = false;

    int resistance = 0;
    int rebel_count = 3;
    boolean rebellion_fired_up = false;

    Game.Team victorious_team = null;

    public record RebelDeploymentPoint(int x, int y){}

    final ArrayList<RebelDeploymentPoint> dep_points = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private void init(IGameControllerGamemode g){
        isInited = true;

        HashMap<String, Object> params = g.getAparams();
        if (params.containsKey("deployment_points")){
            for (HashMap<String, Integer> deployment_point: (ArrayList<HashMap<String, Integer>>)params.get("deployment_points")){
                this.dep_points.add(new RebelDeploymentPoint(deployment_point.get("x"), deployment_point.get("y")));
            }
        }

        if (params.containsKey("flag_coords")){
            HashMap<String, Integer> coords = (HashMap<String, Integer>) params.get("flag_coords");
            FlagEntity flagEntity = new FlagEntity(coords.get("x"), coords.get("y"), g);
            g.addEntity(flagEntity);
        }

        g.getEvReg().addEventListener(new CaptureTheCityEventListener());
    }

    @Override
    public void tick(IGameControllerGamemode g) {
        if (!isInited){
            init(g);
        }

        if (!rebellion_fired_up && resistance >= 100){
            int team_to_td = 0;
            for (RebelDeploymentPoint rebelDeploymentPoint: dep_points){
                if (rebel_count == 0){
                    break;
                }
                if (team_to_td > g.getTeams().size()-1){
                    team_to_td = 0;
                }
                RebelUnit rebel = new RebelUnit(rebelDeploymentPoint.x, rebelDeploymentPoint.y, g);
                g.getTeams().get(team_to_td).addUnit(rebel);
                rebel_count--;
                team_to_td++;
            }
            rebellion_fired_up = true;
        }
    }

    @Override
    public boolean hasGameEnded(IGameControllerGamemode g) {
        return false;
    }

    @Override
    public Game.Team getVictoriousTeam(IGameControllerGamemode g) {
        return null;
    }

    @Override
    public String gmName() {
        return "CTC";
    }
}
