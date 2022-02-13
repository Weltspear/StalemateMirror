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

package net.stalemate.networking.server.lobby_management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import net.stalemate.core.Entity;
import net.stalemate.core.MapObject;
import net.stalemate.core.Unit;
import net.stalemate.core.communication.chat.Chat;
import net.stalemate.core.communication.chat.Message;
import net.stalemate.core.controller.Game;
import net.stalemate.core.map_system.MapLoader;
import net.stalemate.core.units.util.IBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Lobby implements Runnable{
    Game game;
    ArrayList<Player> players = new ArrayList<>();
    private int max_player_count = 2;
    Chat chat;

    final ArrayList<String> next_maps;
    int current_next_map = 0;

    // protected volatile boolean is_terminated = true;

    public synchronized int getMaxPlayerCount() {
        return max_player_count;
    }
    public synchronized int currentPlayerCount(){
        return players.size();
    }

    String map_path;
    String map_name = "default";
    final String lobby_name = "Lobby";

    public void resetLobby(){
        chat = new Chat();
        game = MapLoader.load(map_path);
        max_player_count = MapLoader.getMapPlayerCount(map_path);
        players = new ArrayList<>();
        map_name = MapLoader.getMapName(map_path);
        map_path = next_maps.get(current_next_map);
        current_next_map++;

        if (current_next_map == next_maps.size()){
            current_next_map = 0;
        }
        lbstart();
    }

    public void lbstart(){
        System.out.println("Lobby started!");
        printStatus();

        // Waiting for players
        int i = 0;
        while (players.size() != max_player_count){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (i > 10000){
                printStatus();
                i = 0;
            }
            i++;
        }
        System.out.print("Game Started!");

        for (Player player: players){
            Game.Team t = game.getUnassignedTeam();
            player.setTeam(t);

            for (Unit u: t.getTeamUnits()){
                if (u instanceof IBase){
                    player.setCamPos(u.getX() - 6, u.getY() - 2);
                    break;
                }
            }

            player.setGame(game);
            player.setMapPath(map_path);
            player.setChat(chat);
        }

        while (!game.hasGameEnded() /* Game isn't ended, true is a placeholder */){ // Hardcoded tick speed: 15
            long timeCurrent1 = System.currentTimeMillis();
            game.update();

            boolean terminate_lobby = false;
            for (Player player: players){
                if (player.isConnectionTerminated){
                    terminate_lobby = true;
                    break;
                }
            }
            if (terminate_lobby) {
                for (Player player: players){
                    player.terminateConnection();
                }
                resetLobby();
            }

            long timeCurrent2 = System.currentTimeMillis();
            long t = timeCurrent2 - timeCurrent1;
            long t2 = 70 - t;
            if (t2 > 0){
                try {
                    Thread.sleep(t2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        for (Player player: players){
            if (player.getTeam() == game.getVictoriousTeam()){
                player.setEndOfGameMessage("You won!");
            }
            else{
                String nick = null;
                for (Player p: players){
                    if (p.getTeam() == game.getVictoriousTeam()){
                        nick = p.nickname;
                    }
                }
                player.setEndOfGameMessage("You lost! Player " + nick + " won!");
            }
        }
        resetLobby();
    }

    @Override
    public void run() {
        resetLobby();
    }

    public void printStatus(){
        System.out.println("[Lobby] Current lobby status: Name: " + lobby_name + " Current Map: " + map_name + "Players: " + currentPlayerCount() + "/" + getMaxPlayerCount());
    }

    public static class Player{

        private String map_path = null;
        private int cam_x = 0;
        private int cam_y = 0;

        private int selector_x = 0;
        private int selector_y = 0;

        private int camSelMode = 0;

        private Unit selected_unit = null;

        private Game.Team team = null;
        private Game game = null;

        private String iselectorbuttonid = null;
        private Chat chat;

        String nickname;

        public Player(){
        }

        public void setChat(Chat c){
            chat = c;
        }

        public void setMapPath(String map_path){
            this.map_path = map_path;
        }

        private volatile boolean isConnectionTerminated = false;
        public synchronized boolean isConnectionTerminated(){return isConnectionTerminated;}
        public synchronized void terminateConnection(){isConnectionTerminated = true;}

        public synchronized void setCamPos(int x, int y){
            this.cam_x = x;
            this.cam_y = y;
        }

        public synchronized void setTeam(Game.Team t){
            team = t;
        }
        public synchronized void setGame(Game g){game = g;}

        public synchronized Game.Team getTeam(){
            return team;
        }

        public synchronized boolean hasGameStarted(){return (team != null && game != null);}

        public synchronized void set_nickname(String nickname){
            this.nickname = nickname;
        }

        @SuppressWarnings("unchecked")
        public synchronized void push_command(String json){
            /* Client sent packet example
            * {
            *   "type" : "ActionPacket",
            *
            *   "cam_x" : 0,
            *   "cam_y" : 0,
            *
            *   "sel_x" : 0,
            *   "sel_y" : 0,
            *
            *   "actions" : [
            *       {
            *           "action" : "SelectUnit",
            *       },
            *       {
            *           "action" : "Surrender"
            *       },
            *       {
            *           "action" : "IStandardButtonPress"
            *           "params" : {"id" : "id_here"}
            *       },
            *       {
            *           "action" : "ISelectorButtonPress"
            *           "params" : {"id" : "id_here"}
            *       },
            *       {
            *           "action" : "DeselectUnit"
            *       },
            *       {
            *           "action" : "ISBSelect"
            *       },
            *       {
            *           "action" : "ISBCancel"
            *       },
            *       {
            *           "action" : "EndTurn"
            *       },
            *       {
            *           "action" : "ChangeCamSelMode"
            *       }
            *       {
            *           "action" : "TeleportCamToBase1"
            *       },
            *       {
            *           "action" : "TypeChat",
            *           "msg" : msg
            *       }
            *   ]
            * }
            * */

            while(game.isTeamUpdateUnsafe() && game.isEntityUpdateUnsafe() && !game.isAllowedToUpdate())
            {
                Thread.onSpinWait();
            }

            try {
                if (camSelMode == 0){
                    selector_x = cam_x + 6;
                    selector_y = cam_y + 2;
                }

                PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                        .build();
                ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                Map<String, Object> data_map = (objectMapper).readValue(json, Map.class);
                ArrayList<Map<String, Object>> actions = (ArrayList<Map<String, Object>>) data_map.get("actions");

                boolean ignore_cam_set = false;

                for (Map<String, Object> action : actions) {
                    if (action.get("action").equals("TypeChat")){
                        String msg = (String) action.get("msg");
                        chat.pushMsg(new Message(nickname, msg));
                    }

                    if (action.get("action").equals("ChangeCamSelMode")){
                        switch (camSelMode) {
                            case 0 -> camSelMode = 1;
                            case 1 -> camSelMode = 0;
                        }
                    }
                    else if (action.get("action").equals("TeleportCamToBase1")){
                        iselectorbuttonid = null;
                        selected_unit = null;
                        camSelMode = 0;

                        for (Unit u: team.getTeamUnits()){
                            if (u instanceof IBase){
                                cam_x = u.getX() - 6;
                                cam_y = u.getY() - 2;

                                selector_x = u.getX();
                                selector_y = u.getY();
                                break;
                            }
                        }
                        ignore_cam_set = true;
                    }

                    else if (action.get("action").equals("SelectUnit")) {
                        iselectorbuttonid = null;
                        selected_unit = null;

                        ArrayList<Entity> entities = game.getEntities(selector_x, selector_y);

                        for (Entity entity : entities) {
                            if (entity instanceof Unit) {
                                selected_unit = (Unit) entity;
                            }
                        }

                    }
                    else if (action.get("action").equals("IStandardButtonPress")) {
                        if (game.getTeamDoingTurn() == team) {
                            Map<String, Object> params = (Map<String, Object>) action.get("params");

                            if (selected_unit != null) {
                                if (team.getTeamUnits().contains(selected_unit) && selected_unit.getButtons() != null) {
                                    for (Unit.IButton ibutton : selected_unit.getButtons()) {
                                        if (ibutton != null)
                                            if (ibutton.identifier().equals(params.get("id"))) {
                                                if (ibutton instanceof Unit.IStandardButton) {
                                                    while (game.isEntityUpdateUnsafe() && game.isTeamUpdateUnsafe()){
                                                        Thread.onSpinWait();
                                                    }
                                                    ((Unit.IStandardButton) ibutton).action(selected_unit, game);
                                                    iselectorbuttonid = null;
                                                }
                                            }
                                    }
                                } else {
                                    if (selected_unit.getButtonsEnemy() != null) {
                                        for (Unit.IButton ibutton : selected_unit.getButtonsEnemy()) {
                                            if (ibutton != null)
                                                if (ibutton.identifier().equals(params.get("id"))) {
                                                    if (ibutton instanceof Unit.IStandardButton) {
                                                        while (game.isEntityUpdateUnsafe() && game.isTeamUpdateUnsafe()){
                                                            Thread.onSpinWait();
                                                        }
                                                        ((Unit.IStandardButton) ibutton).action(selected_unit, game);
                                                        iselectorbuttonid = null;
                                                    }
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else if (action.get("action").equals("ISelectorButtonPress")) {
                        if (game.getTeamDoingTurn() == team) {
                            Map<String, Object> params = (Map<String, Object>) action.get("params");

                            if (selected_unit != null & iselectorbuttonid == null) {
                                if (team.getTeamUnits().contains(selected_unit) && selected_unit.getButtons() != null) {
                                    for (Unit.IButton button : selected_unit.getButtons()) {
                                        if (button != null)
                                            if (button.identifier().equals(params.get("id"))) {
                                                if (button instanceof Unit.ISelectorButton || button instanceof Unit.ISelectorButtonUnit) {
                                                    iselectorbuttonid = (String) params.get("id");
                                                    cam_x = selected_unit.getX() - 6;
                                                    cam_y = selected_unit.getY() - 2;

                                                    selector_x = selected_unit.getX();
                                                    selector_y = selected_unit.getY();
                                                }
                                            }
                                    }
                                } else {
                                    if (selected_unit.getButtonsEnemy() != null) {
                                        for (Unit.IButton button : selected_unit.getButtonsEnemy()) {
                                            if (button != null)
                                                if (button.identifier().equals(params.get("id"))) {
                                                    if (button instanceof Unit.ISelectorButton || button instanceof Unit.ISelectorButtonUnit) {
                                                        iselectorbuttonid = (String) params.get("id");
                                                        cam_x = selected_unit.getX() - 6;
                                                        cam_y = selected_unit.getY() - 2;

                                                        selector_x = selected_unit.getX();
                                                        selector_y = selected_unit.getY();
                                                    }
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else if (action.get("action").equals("ISBSelect")) {
                        if (game.getTeamDoingTurn() == team) {
                            if (selected_unit != null && iselectorbuttonid != null) {
                                if ((team.getTeamUnits().contains(selected_unit) && selected_unit.getButtons() != null) || (!team.getTeamUnits().contains(selected_unit) && selected_unit.getButtonsEnemy() != null)) {
                                    boolean isSelectedUnitEnemyTeam = !(selected_unit.getButtons() != null
                                            && team.getTeamUnits().contains(selected_unit));
                                    for (Unit.IButton button : isSelectedUnitEnemyTeam && selected_unit.getButtonsEnemy() != null ?
                                        selected_unit.getButtonsEnemy() : isSelectedUnitEnemyTeam
                                            ? new ArrayList<Unit.IButton>() : selected_unit.getButtons() != null
                                            ? selected_unit.getButtons() : new ArrayList<Unit.IButton>()) {
                                        if (button != null)
                                            if (button.identifier().equals(iselectorbuttonid)) {
                                                if (button instanceof Unit.ISelectorButton) {
                                                    if (selector_x >= 0 && selector_y >= 0) {
                                                        while (game.isEntityUpdateUnsafe() && game.isTeamUpdateUnsafe()){
                                                            Thread.onSpinWait();
                                                        }
                                                        ((Unit.ISelectorButton) button).action(selector_x, selector_y, selected_unit, game);
                                                    }
                                                    iselectorbuttonid = null;
                                                } else if (button instanceof Unit.ISelectorButtonUnit) {
                                                    ArrayList<Entity> entities = game.getEntities(selector_x, selector_y);

                                                    for (Entity entity : entities) {
                                                        if (entity instanceof Unit) {
                                                            if ((((Unit.ISelectorButtonUnit) button).isUsedOnAlliedUnit()
                                                                    && game.getUnitsTeam(selected_unit).getTeamUnits().contains(entity))) {
                                                                ((Unit.ISelectorButtonUnit) button).action(((Unit) entity), selected_unit, game);
                                                                iselectorbuttonid = null;
                                                            } else if ((((Unit.ISelectorButtonUnit) button).isUsedOnEnemy()
                                                                    && !game.getUnitsTeam(selected_unit).getTeamUnits().contains(entity))) {
                                                                while (game.isEntityUpdateUnsafe() && game.isTeamUpdateUnsafe()){
                                                                    Thread.onSpinWait();
                                                                }
                                                                if (isSelectedUnitEnemyTeam && !team.getTeamUnits().contains(entity) && ((Unit.ISelectorButtonUnit) button).canEnemyTeamUseOnOtherEnemyTeamUnit())
                                                                    ((Unit.ISelectorButtonUnit) button).action(((Unit) entity), selected_unit, game);
                                                                else if (isSelectedUnitEnemyTeam && team.getTeamUnits().contains(entity))
                                                                    ((Unit.ISelectorButtonUnit) button).action(((Unit) entity), selected_unit, game);
                                                                else if (!isSelectedUnitEnemyTeam)
                                                                    ((Unit.ISelectorButtonUnit) button).action(((Unit) entity), selected_unit, game);
                                                                iselectorbuttonid = null;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    }
                                }
                            }
                        }
                    }

                    else if (action.get("action").equals("ISBCancel")) {
                        iselectorbuttonid = null;
                    }

                    else if (action.get("action").equals("DeselectUnit")) {
                        selected_unit = null;
                        iselectorbuttonid = null;
                    }

                    else if (action.get("action").equals("EndTurn")) {
                        if (game.getTeamDoingTurn() == team) {
                            if (!team.endedTurn()) {
                                team.endTurn();
                                iselectorbuttonid = null;
                            }
                        }
                    }
                }


                int cam_y_tmp = camSelMode == 0 ? (int) data_map.get("cam_y") : (int) data_map.get("sel_y");
                int cam_x_tmp = camSelMode == 0 ? (int) data_map.get("cam_x") : (int) data_map.get("sel_x");

                if (iselectorbuttonid != null & selected_unit != null) {
                    int selector_x_tmp;
                    int selector_y_tmp;
                    if (camSelMode == 0) {
                        selector_x_tmp = cam_x_tmp + 6;
                        selector_y_tmp = cam_y_tmp + 2;
                    } else{
                        selector_x_tmp = (int) data_map.get("sel_x");
                        selector_y_tmp = (int) data_map.get("sel_y");
                    }

                    int range = -1;

                    // Get selector button
                    if ((team.getTeamUnits().contains(selected_unit) && selected_unit.getButtons() != null) || (selected_unit.getButtonsEnemy() != null && !team.getTeamUnits().contains(selected_unit)))
                        for (Unit.IButton button : Objects.requireNonNull((selected_unit.getButtons() != null
                                && team.getTeamUnits().contains(selected_unit)) ? selected_unit.getButtons() : selected_unit.getButtonsEnemy())) {
                            if (button != null)
                                if (button.identifier().equals(iselectorbuttonid)) {
                                    if (button instanceof Unit.ISelectorButton || button instanceof Unit.ISelectorButtonUnit) {
                                        if (button instanceof Unit.ISelectorButton) {
                                            range = ((Unit.ISelectorButton) button).selector_range();
                                        } else {
                                            range = ((Unit.ISelectorButtonUnit) button).selector_range();
                                        }
                                    }
                                }
                        }

                    if (((selected_unit.getX() - range) <= selector_x_tmp) && ((selected_unit.getX() + range) >= selector_x_tmp)) {
                        if (((selected_unit.getY() - range) <= selector_y_tmp) && ((selected_unit.getY() + range) >= selector_y_tmp)) {
                            if (camSelMode == 0) {
                                if ((cam_x_tmp + 6 >= 0 && cam_y_tmp + 2 >= 0) && (cam_y_tmp + 2 < game.getSizeY())) {
                                    if (cam_x_tmp + 6 < game.getSizeX(cam_y_tmp + 2)) {
                                        cam_x = cam_x_tmp;
                                        cam_y = cam_y_tmp;
                                    }
                                }
                            }
                            else {
                                if ((cam_x_tmp >= 0 && cam_y_tmp >= 0) && (cam_y_tmp < game.getSizeY())) {
                                    if (cam_x_tmp < game.getSizeX(cam_y_tmp)) {
                                        if (cam_x <= cam_x_tmp && cam_x_tmp <= cam_x+12 && cam_y <= cam_y_tmp && cam_y_tmp <= cam_y+4) {
                                            selector_x = cam_x_tmp;
                                            selector_y = cam_y_tmp;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (!ignore_cam_set){
                    if (camSelMode == 0) {
                        if ((cam_x_tmp + 6 >= 0 && cam_y_tmp + 2 >= 0) && (cam_y_tmp + 2 < game.getSizeY())) {
                            if (cam_x_tmp + 6 < game.getSizeX(cam_y_tmp + 2)) {
                                cam_x = cam_x_tmp;
                                cam_y = cam_y_tmp;
                            }
                        }
                    }
                    else {
                        if ((cam_x_tmp >= 0 && cam_y_tmp >= 0) && (cam_y_tmp < game.getSizeY())) {
                            if (cam_x_tmp < game.getSizeX(cam_y_tmp)) {
                                if (cam_x <= cam_x_tmp && cam_x_tmp <= cam_x+12 && cam_y <= cam_y_tmp && cam_y_tmp <= cam_y+4) {
                                    selector_x = cam_x_tmp;
                                    selector_y = cam_y_tmp;
                                }
                            }
                        }
                    }
                }

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        }

        public synchronized String create_json_packet(){
            if (game != null && team != null) {
                // Change selector coordinates to cam if camSelMode is 0
                if (camSelMode == 0){
                    selector_x = cam_x + 6;
                    selector_y = cam_y + 2;
                }

                // Create entity render
                ArrayList<ArrayList<Entity>> entity_render = new ArrayList<>();
                int y2 = 0;
                for (int y = 0; y < 5; y++){
                    entity_render.add(new ArrayList<>());
                    for (int x = 0; x < 13; x++){
                        boolean has_space_been_filled = false;
                        for (Entity entity: game.getAllEntitiesCopy()){
                            if (entity.getX() == cam_x+x & entity.getY() == cam_y+y){
                                has_space_been_filled = true;
                                entity_render.get(y2).add(entity);
                                break;
                            }
                        }
                        if (!has_space_been_filled){
                            entity_render.get(y2).add(null);
                        }
                    }
                    y2++;
                }

                // Create entity fog-of-war render

                ArrayList<Unit> ally_units = new ArrayList<>();
                for (Entity entity: game.getAllEntitiesCopy()){
                    if (entity instanceof Unit){
                        if (team.getTeamUnits().contains(entity)){
                            ally_units.add((Unit) entity);
                        }
                    }
                }

                // Remove out of range in-fog entities

                ArrayList<Entity> to_remove_entities = new ArrayList<>();

                for (int y = 0; y < 5; y++){
                    for (int x = 0; x < 13; x++){
                        Entity entity = entity_render.get(y).get(x);

                        boolean is_in_fog_of_war_range = false;

                        if (entity != null) {
                            for (Unit unit : ally_units) {
                                if (unit.isEntityInFogOfWarRange(entity)) {
                                    is_in_fog_of_war_range = true;
                                    break;
                                }
                            }
                            if (!is_in_fog_of_war_range || entity.isInvisible())
                            to_remove_entities.add(entity);

                            if (ally_units.isEmpty()){
                                to_remove_entities.add(entity);
                            }
                        }
                    }
                }

                for (Entity entity: to_remove_entities){
                    entity_render.get(entity.getY()-cam_y).set(entity.getX()-cam_x, null);
                }

                // Final Entity render

                ArrayList<ArrayList<String>> entity_render_final = new ArrayList<>();

                y2 = 0;
                for (int y = 0; y < 5; y++){
                    entity_render_final.add(new ArrayList<>());
                    for (int x = 0; x < 13; x++){
                        String entity = entity_render.get(y).get(x) != null ? entity_render.get(y).get(x).getTextureFileName() : null;
                        entity_render_final.get(y).add(entity);
                    }
                    y2++;
                }

                // Fog of war creation

                ArrayList<ArrayList<Integer>> fog_of_war = new ArrayList<>();

                y2 = 0;
                for (int y = 0; y < 5; y++){
                    fog_of_war.add(new ArrayList<>());
                    for (int x = 0; x < 13; x++){
                        fog_of_war.get(y2).add(1);
                    }
                    y2++;
                }

                for (Unit ally_unit: ally_units){
                    for (int y = -ally_unit.getFogOfWarRange(); y < ally_unit.getFogOfWarRange()+1; y++){
                        for (int x = -ally_unit.getFogOfWarRange(); x < ally_unit.getFogOfWarRange()+1; x++){
                            int x1 = (ally_unit.getX()-cam_x)+x;
                            int y1 = (ally_unit.getY()-cam_y)+y;

                            if ((x1 >= 0 && y1 >= 0)){
                                if (y1 < fog_of_war.size()){
                                    if (x1 < fog_of_war.get(y1).size()){
                                        fog_of_war.get(y1).set(x1, 0);
                                    }
                                }

                            }
                        }
                    }
                }

                // Making it a json string
                HashMap<String, Object> toBeJsoned = new HashMap<>();
                toBeJsoned.put("x", cam_x);
                toBeJsoned.put("y", cam_y);

                toBeJsoned.put("map_path", map_path);

                toBeJsoned.put("sel_x", selector_x);
                toBeJsoned.put("sel_y", selector_y);

                toBeJsoned.put("entity_render", entity_render_final);
                toBeJsoned.put("fog_of_war", fog_of_war);
                toBeJsoned.put("is_it_your_turn", team == game.getTeamDoingTurn());

                // tiles
                ArrayList<ArrayList<Boolean>> map_tiles = new ArrayList<>();
                int y = 0;
                for (ArrayList<MapObject> mapObjects: game.getMap()){
                    map_tiles.add(new ArrayList<>());
                    for (MapObject tile: mapObjects){
                        map_tiles.get(y).add(tile.isPassable);
                    }
                    y++;
                }

                // Create unit_data_ar

                ArrayList<ArrayList<HashMap<String, Object>>> unit_data_ar = new ArrayList<>();
                y2 = 0;
                for (ArrayList<Entity> ent_row : entity_render){
                    unit_data_ar.add(new ArrayList<>());
                    for (Entity entity : ent_row){
                        if (entity != null) {
                            if (entity instanceof Unit unit) {
                                HashMap<String, Object> unit_data = new HashMap<>();
                                for (Game.Team team : game.getTeams()) {
                                    if (team.getTeamUnits().contains(unit)) {
                                        ArrayList<Integer> rgb = new ArrayList<>();
                                        rgb.add(team.getTeamColor().getRed());
                                        rgb.add(team.getTeamColor().getGreen());
                                        rgb.add(team.getTeamColor().getBlue());
                                        unit_data.put("rgb", rgb);
                                    }
                                }
                                ArrayList<Integer> stats = new ArrayList<>();
                                stats.add(unit.unitStats().getHp());
                                stats.add(unit.unitStats().getMaxHp());
                                stats.add(unit.unitStats().getSupply());
                                stats.add(unit.unitStats().getMaxSupply());
                                unit_data.put("flip", unit.isFlipped());
                                unit_data.put("stats", stats);
                                unit_data_ar.get(y2).add(unit_data);
                            }
                            else {
                                unit_data_ar.get(y2).add(null);
                            }
                        } else {
                            unit_data_ar.get(y2).add(null);
                        }
                    }
                    y2++;
                }

                toBeJsoned.put("unit_data_ar", unit_data_ar);
                toBeJsoned.put("mp", team.getMilitaryPoints());

                // Deselect a unit if it is in fog of war and if it is removed or dead
                if (selected_unit != null){
                    if (selected_unit.getY()-cam_y >= 0 && selected_unit.getX()-cam_x >= 0)
                        if (selected_unit.getY()-cam_y < 5 && selected_unit.getX()-cam_x < 13)
                            if (fog_of_war.get(selected_unit.getY()-cam_y).get(selected_unit.getX()-cam_x) == 1){
                                selected_unit = null;
                                iselectorbuttonid = null;
                            }

                    else if (selected_unit.unitStats().getHp() <= 0){
                        selected_unit = null;
                        iselectorbuttonid = null;
                    }
                    else if (!game.getAllEntities().contains(selected_unit)){
                        selected_unit = null;
                    }
                }

                if (selected_unit != null) {
                    HashMap<String, Object> selected_unit_data = new HashMap<>();
                    selected_unit_data.put("properties", selected_unit.getProperties().getProperties());
                    selected_unit_data.put("texture", selected_unit.getTextureFileName());

                    ArrayList<Object> buttons = new ArrayList<>();
                    Unit.IButton iselectorbutton_used = null;

                    if (team.getTeamUnits().contains(selected_unit) && selected_unit.getButtons() != null){
                        for (Unit.IButton button : selected_unit.getButtons()){
                            if (button != null){
                                HashMap<String, Object> button_ = new HashMap<>();
                                button_.put("id", button.identifier());
                                button_.put("bind", button.bind());
                                button_.put("texture", button.texture());
                                button_.put("mode", button instanceof Unit.ISelectorButton || button instanceof Unit.ISelectorButtonUnit ? 2 : 1);
                                buttons.add(button_);

                                if (iselectorbuttonid != null)
                                    if (button instanceof Unit.ISelectorButton || button instanceof Unit.ISelectorButtonUnit)
                                        if (button.identifier().equals(iselectorbuttonid))
                                            iselectorbutton_used = button;
                            }
                            else{
                                buttons.add(0);
                            }
                        }
                    } else if (!team.getTeamUnits().contains(selected_unit)) {
                        if (selected_unit.getButtonsEnemy() != null)
                            for (Unit.IButton button : selected_unit.getButtonsEnemy()){
                                if (button != null){
                                    HashMap<String, Object> button_ = new HashMap<>();
                                    button_.put("id", button.identifier());
                                    button_.put("bind", button.bind());
                                    button_.put("texture", button.texture());
                                    button_.put("mode", button instanceof Unit.ISelectorButton || button instanceof Unit.ISelectorButtonUnit ? 2 : 1);
                                    buttons.add(button_);

                                    if (iselectorbuttonid != null)
                                        if (button instanceof Unit.ISelectorButton || button instanceof Unit.ISelectorButtonUnit)
                                            if (button.identifier().equals(iselectorbuttonid))
                                                iselectorbutton_used = button;
                                }
                                else{
                                    buttons.add(0);
                                }
                            }
                    }

                    selected_unit_data.put("buttons", buttons);

                    ArrayList<Object> queue = new ArrayList<>();

                    boolean is_queue_valid = false;

                    if (team.getTeamUnits().contains(selected_unit) && selected_unit != null){
                        if (selected_unit.getUnitQueue() != null){
                            if (!selected_unit.getUnitQueue().getQueueMembers().isEmpty()) {
                                ArrayList<Unit.UnitQueue.QueueMember> members = selected_unit.getUnitQueue().getQueueMembers();
                                for (Unit.UnitQueue.QueueMember queueMember : members) {
                                    HashMap<String, Object> member = new HashMap<>();
                                    member.put("texture", queueMember.texture());
                                    member.put("turn_time", queueMember.turn_time());
                                    queue.add(member);
                                }
                            }
                            is_queue_valid = true;
                        }

                    }

                    if (!is_queue_valid){
                        selected_unit_data.put("queue", 0);
                    }
                    else {
                        selected_unit_data.put("queue", queue);
                    }

                    if (iselectorbuttonid != null){
                        selected_unit_data.put("iselectorbutton_press", true);
                        if (iselectorbutton_used instanceof Unit.ISelectorButton)
                        selected_unit_data.put("iselectorbutton_data_selector_texture", ((Unit.ISelectorButton) iselectorbutton_used).selector_texture());
                        if (iselectorbutton_used instanceof Unit.ISelectorButtonUnit)
                            selected_unit_data.put("iselectorbutton_data_selector_texture", ((Unit.ISelectorButtonUnit) iselectorbutton_used).selector_texture());
                    }
                    else {
                        selected_unit_data.put("iselectorbutton_press", false);
                    }

                    toBeJsoned.put("selected_unit_data", selected_unit_data);
                }
                else {
                    toBeJsoned.put("selected_unit_data", 0);
                }

                // Chat
                ArrayList<String> chat = this.chat.read();
                toBeJsoned.put("chat", chat);

                try {
                    return (new ObjectMapper()).writeValueAsString(toBeJsoned);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected String endOfAGameMessage = null;

        public void setEndOfGameMessage(String endOfAGameMessage){
            this.endOfAGameMessage = endOfAGameMessage;
        }

        public String getEndOfAGameMessage() {
            return endOfAGameMessage;
        }
    }

    public Lobby(String map_path, ArrayList<String> next_maps){
        this.map_path = map_path;
        this.next_maps = next_maps;
    }

    public synchronized Player connect_to_lobby(){
        if (players.size() == max_player_count){
            return null;
        }
        else{
            Player player = new Player();
            players.add(player);
            return player;
        }
    }

    public enum LobbyState {
        WAITING_FOR_PLAYERS,
        STARTED
    }
    public synchronized LobbyState current_lobby_state(){
        return players.size() != max_player_count ? LobbyState.WAITING_FOR_PLAYERS : LobbyState.STARTED;
    }

    public synchronized String asString(){
        return "" + lobby_name + ": Map: " + map_name + "[" + currentPlayerCount() + "/" + getMaxPlayerCount() + "]";
    }


}
