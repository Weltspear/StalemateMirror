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

package net.stalemate.server.lobby_management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.libutils.error.Expect;
import net.stalemate.server.core.AirUnit;
import net.stalemate.server.core.Entity;
import net.stalemate.server.core.MapObject;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.communication.chat.Chat;
import net.stalemate.server.core.communication.chat.Message;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.map_system.MapLoader;
import net.stalemate.server.core.units.util.IBase;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.stalemate.log.MakeLog.makeLog;

public class Lobby implements Runnable{ // todo add more locks if necessary
    Game game;
    ArrayList<Player> players = new ArrayList<>();
    private int max_player_count = 2;
    Chat chat;

    final ArrayList<String> next_maps;
    int current_next_map = 0;

    public int getMaxPlayerCount() {
        try {
            lobby_lock.lock();
            return max_player_count;
        } finally {
            lobby_lock.unlock();
        }
    }
    public int currentPlayerCount() {
        try {
            lobby_lock.lock();
            return players.size();
        } finally {
            lobby_lock.unlock();
        }
    }

    String map_path;
    String map_name = "default";
    String game_mode = "Versus";

    private static final Logger LOGGER = makeLog(Logger.getLogger(Lobby.class.getSimpleName()));

    private final ReentrantLock lobby_lock = new ReentrantLock();

    public void resetLobby(){
        lobby_lock.lock();

        chat = new Chat();
        game = MapLoader.load(map_path);
        max_player_count = MapLoader.getMapPlayerCount(map_path);
        players = new ArrayList<>();
        map_name = MapLoader.getMapName(map_path);
        map_path = next_maps.get(current_next_map);
        game_mode = game.getMode().gmName();
        current_next_map++;

        if (current_next_map == next_maps.size()){
            current_next_map = 0;
        }

        lobby_lock.unlock();
        lbstart();
    }

    public void lbstart(){
        LOGGER.log(Level.INFO,"Lobby started!");
        printStatus();

        // Waiting for players
        int i = 0;
        while (players.size() != max_player_count){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.log(Level.INFO, "Game Started! [" + currentPlayerCount() + "/" + getMaxPlayerCount() + "]");

        lobby_lock.lock();

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

        lobby_lock.unlock();

        while (!game.hasGameEnded()){ // Hardcoded tick speed: 15
            long timeCurrent1 = System.currentTimeMillis();
            game.update();

            boolean terminate_lobby = false;
            lobby_lock.lock();
            for (Player player: players){
                if (player.isConnectionTerminated){
                    terminate_lobby = true;
                    break;
                }
            }
            lobby_lock.unlock();
            if (terminate_lobby) {
                lobby_lock.lock();
                for (Player player: players){
                    player.terminateConnection("Another player has disconnected");
                }
                lobby_lock.unlock();
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

        lobby_lock.lock();

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

        lobby_lock.unlock();

        resetLobby();
    }

    @Override
    public void run() {
        resetLobby();
    }

    public void printStatus(){
        LOGGER.log(Level.INFO, "Current lobby status: Name: " + game_mode + " Current Map: " + map_name + "Players: " + currentPlayerCount() + "/" + getMaxPlayerCount());
    }

    public static class Player{

        public enum ViewMode{
            GROUND,
            AIR
        }

        private String map_path = null;
        private int cam_x = 0;
        private int cam_y = 0;

        private int selector_x = 0;
        private int selector_y = 0;

        private Unit selected_unit = null;

        private Game.Team team = null;
        private Game game = null;

        private String iselectorbuttonid = null;
        private Chat chat;

        String nickname;

        private final ReentrantLock lock = new ReentrantLock();

        private ViewMode viewMode = ViewMode.GROUND;

        public Player(){
        }

        public void setChat(Chat c){
            lock.lock();
            chat = c;
            lock.unlock();
        }

        public void setMapPath(String map_path){
            lock.lock();
            this.map_path = map_path;
            lock.unlock();
        }

        private volatile boolean isConnectionTerminated = false;
        private String contermination_cause = null;

        public boolean isConnectionTerminated(){return isConnectionTerminated;}
        public void terminateConnection(){isConnectionTerminated = true;}
        public void terminateConnection(String cause){
            lock.lock();
            isConnectionTerminated = true;
            contermination_cause = cause;
            lock.unlock();
        }

        public String getConTerminationCause(){
            return contermination_cause;
        }

        public void setCamPos(int x, int y){
            lock.lock();
            this.cam_x = x;
            this.cam_y = y;
            lock.unlock();
        }

        public void setTeam(Game.Team t){
            lock.lock();
            team = t;
            lock.unlock();
        }
        public void setGame(Game g){
            lock.lock();
            game = g;
            lock.unlock();
        }

        public Game.Team getTeam(){
            lock.lock();
            try {
                return team;
            } finally {
                lock.unlock();
            }
        }

        public boolean hasGameStarted(){
            lock.lock();
            try {
                return (team != null && game != null);
            } finally {
                lock.unlock();
            }
        }

        public void set_nickname(String nickname){
            lock.lock();
            this.nickname = nickname;
            lock.unlock();
        }

        @SuppressWarnings("unchecked")
        public synchronized Expect<Integer, ?> push_command(String json){
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
            *           "action" : "TeleportCamToBase1"
            *       },
            *       {
            *           "action" : "ChangeViewMode"
            *       },
            *       {
            *           "action" : "TypeChat",
            *           "msg" : msg
            *       }
            *   ]
            * }
            * */

            game.lock.lock();
            lock.lock();

            try {
                PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                        .build();
                ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                Map<String, Object> data_map = (objectMapper).readValue(json, Map.class);
                ArrayList<Map<String, Object>> actions = (ArrayList<Map<String, Object>>) data_map.get("actions");

                boolean ignore_cam_set = false;

                Game.LockGame lgame = game.getLockedGame();

                for (Map<String, Object> action : actions) {
                    if (action.get("action").equals("TypeChat")){
                        String msg = (String) action.get("msg");
                        chat.pushMsg(new Message(nickname, msg));
                    }

                    else if (action.get("action").equals("TeleportCamToBase1")){
                        iselectorbuttonid = null;
                        selected_unit = null;

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

                        ArrayList<Entity> entities = lgame.getEntities(selector_x, selector_y);

                        for (Entity entity : entities) {
                            if (entity instanceof Unit) {
                                if ((entity instanceof AirUnit && viewMode == ViewMode.AIR) || (!(entity instanceof AirUnit) && viewMode == ViewMode.GROUND))
                                selected_unit = (Unit) entity;
                            }
                        }

                    }
                    else if (action.get("action").equals("IStandardButtonPress")) {
                        if (lgame.getTeamDoingTurn() == team) {
                            Map<String, Object> params = (Map<String, Object>) action.get("params");

                            if (selected_unit != null) {
                                if (team.getTeamUnits().contains(selected_unit) && selected_unit.getButtons() != null) {
                                    for (Unit.IButton ibutton : selected_unit.getButtons()) {
                                        doActionIStandardButtonIfCorrect(params, ibutton);
                                    }
                                } else {
                                    if (selected_unit.getButtonsEnemy() != null) {
                                        for (Unit.IButton ibutton : selected_unit.getButtonsEnemy()) {
                                            doActionIStandardButtonIfCorrect(params, ibutton);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else if (action.get("action").equals("ISelectorButtonPress")) {
                        if (lgame.getTeamDoingTurn() == team) {
                            Map<String, Object> params = (Map<String, Object>) action.get("params");

                            if (selected_unit != null & iselectorbuttonid == null) {
                                if (team.getTeamUnits().contains(selected_unit) && selected_unit.getButtons() != null) {
                                    for (Unit.IButton button : selected_unit.getButtons()) {
                                        prepareISelectorButton(params, button);
                                    }
                                } else {
                                    if (selected_unit.getButtonsEnemy() != null) {
                                        for (Unit.IButton button : selected_unit.getButtonsEnemy()) {
                                            prepareISelectorButton(params, button);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else if (action.get("action").equals("ISBSelect")) {
                        if (lgame.getTeamDoingTurn() == team) {
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
                                                    if (viewMode == getButtonViewMode(button))
                                                    if (selector_x >= 0 && selector_y >= 0) {
                                                        ((Unit.ISelectorButton) button).action(selector_x, selector_y, selected_unit, game);
                                                    }
                                                    iselectorbuttonid = null;
                                                } else if (button instanceof Unit.ISelectorButtonUnit) {
                                                    if (viewMode == getButtonViewMode(button)) {
                                                        ArrayList<Entity> entities = game.getEntities(selector_x, selector_y);

                                                        for (Entity entity : entities) {
                                                            if (entity instanceof Unit) {
                                                                if ((((Unit.ISelectorButtonUnit) button).isUsedOnAlliedUnit()
                                                                        && game.getUnitsTeam(selected_unit).getTeamUnits().contains(entity))) {
                                                                    ((Unit.ISelectorButtonUnit) button).action(((Unit) entity), selected_unit, game);
                                                                    iselectorbuttonid = null;
                                                                } else if ((((Unit.ISelectorButtonUnit) button).isUsedOnEnemy()
                                                                        && !game.getUnitsTeam(selected_unit).getTeamUnits().contains(entity))) {
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
                    }

                    else if (action.get("action").equals("ISBCancel")) {
                        iselectorbuttonid = null;
                        if (selected_unit instanceof AirUnit && viewMode != ViewMode.AIR){
                            viewMode = ViewMode.AIR;
                        }

                        else if (!(selected_unit instanceof AirUnit) && viewMode != ViewMode.GROUND){
                            viewMode = ViewMode.GROUND;
                        }
                    }

                    else if (action.get("action").equals("DeselectUnit")) {
                        selected_unit = null;
                        iselectorbuttonid = null;
                    }

                    else if (action.get("action").equals("EndTurn")) {
                        if (lgame.getTeamDoingTurn() == team) {
                            if (!team.endedTurn()) {
                                team.endTurn();
                                iselectorbuttonid = null;
                            }
                        }
                    }

                    else if (action.get("action").equals("ChangeViewMode")){
                        iselectorbuttonid = null;
                        selected_unit = null;
                        viewMode = viewMode == ViewMode.GROUND ? ViewMode.AIR : ViewMode.GROUND;
                    }
                }


                int cam_y_tmp = (int) data_map.get("cam_y");
                int cam_x_tmp = (int) data_map.get("cam_x");
                int sel_x_tmp = (int) data_map.get("sel_x");
                int sel_y_tmp = (int) data_map.get("sel_y");

                if (iselectorbuttonid != null & selected_unit != null) {
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

                    if (((selected_unit.getX() - range) <= sel_x_tmp) && ((selected_unit.getX() + range) >= sel_x_tmp)) {
                        if (((selected_unit.getY() - range) <= sel_y_tmp) && ((selected_unit.getY() + range) >= sel_y_tmp)) {
                            performSelectorMove(sel_y_tmp, sel_x_tmp);
                        }
                    }
                }
                else {
                    performSelectorMove(sel_y_tmp, sel_x_tmp);
                }

                if (!ignore_cam_set){
                    performCameraMove(cam_y_tmp, cam_x_tmp);
                }

            } catch (JsonProcessingException | ClassCastException | NullPointerException e) {
                e.printStackTrace();
                return new Expect<>(() -> "Failed to read packet");
            } finally {
                lock.unlock();
                game.lock.unlock();
            }
            return new Expect<>(1);

        }

        /***
         * Moves camera if several conditions to ensure correct camera position are met
         * @param cam_y_tmp Camera y which is already moved
         * @param cam_x_tmp Camera x which is already moved
         */
        private void performCameraMove(int cam_y_tmp, int cam_x_tmp) {
            if ((cam_x_tmp + 6 >= 0 && cam_y_tmp + 2 >= 0) && (cam_y_tmp + 2 < game.getSizeY())) {
                if (cam_x_tmp + 6 < game.getSizeX(cam_y_tmp + 2)) {
                    cam_x = cam_x_tmp;
                    cam_y = cam_y_tmp;
                }
            }
        }

        public void performSelectorMove(int sel_y_tmp, int sel_x_tmp){
            if ((sel_x_tmp >= 0 && sel_y_tmp >= 0) && (sel_y_tmp < game.getSizeY())) {
                if (sel_x_tmp < game.getSizeX(sel_y_tmp)) {
                    if (cam_x-1 <= sel_x_tmp && sel_x_tmp <= cam_x+13 && cam_y-1 <= sel_y_tmp && sel_y_tmp <= cam_y+5) {
                        selector_x = sel_x_tmp;
                        selector_y = sel_y_tmp;
                    }
                }
            }
        }

        /***
         * Prepares ISelectorButtons to be used, moves camera and selector into correct position
         * @param params action paramters
         * @param button button to be checked
         */
        private void prepareISelectorButton(Map<String, Object> params, Unit.IButton button) {
            if (button != null)
                if (button.identifier().equals(params.get("id"))) {
                    if (button instanceof Unit.ISelectorButton || button instanceof Unit.ISelectorButtonUnit) {
                        iselectorbuttonid = (String) params.get("id");
                        cam_x = selected_unit.getX() - 6;
                        cam_y = selected_unit.getY() - 2;

                        selector_x = selected_unit.getX();
                        selector_y = selected_unit.getY();

                        viewMode = getButtonViewMode(button);
                    }
                }
        }

        private ViewMode layer2viewmode(Unit.Layer l){
            return l == Unit.Layer.GROUND ? ViewMode.GROUND: ViewMode.AIR;
        }

        private ViewMode getButtonViewMode(Unit.IButton button){
            if (button instanceof Unit.ISelectorButton b){
                return layer2viewmode(b.getLayer());
            } else if (button instanceof Unit.ISelectorButtonUnit b){
                return layer2viewmode(b.getLayer());
            }
            return ViewMode.GROUND;
        }

        private boolean containsGroundUnit(ArrayList<Entity> entities){
            for (Entity entity: entities){
                if (entity instanceof Unit && !(entity instanceof AirUnit)){
                    return true;
                }
            }
            return false;
        }

        private boolean containsAirUnit(ArrayList<Entity> entities){
            for (Entity entity: entities){
                if (entity instanceof AirUnit){
                    return true;
                }
            }
            return false;
        }

        /***
         * Performs an action with IStandardButton if params button id equals ibutton id. Resets iselectorbuttonid
         * @param params action parameters
         * @param ibutton button to be checked
         */
        private void doActionIStandardButtonIfCorrect(Map<String, Object> params, Unit.IButton ibutton) {
            if (ibutton != null)
                if (ibutton.identifier().equals(params.get("id"))) {
                    if (ibutton instanceof Unit.IStandardButton) {
                        ((Unit.IStandardButton) ibutton).action(selected_unit, game);
                        iselectorbuttonid = null;
                    }
                }
        }

        public synchronized String create_json_packet(){
            lock.lock();
            if (game != null)
            game.lock.lock();
            try {
                if (game != null && team != null) {
                    Game.LockGame lgame = game.getLockedGame();

                    // Create entity render
                    ArrayList<ArrayList<Entity>> entity_render = new ArrayList<>();
                    int y2 = 0;
                    for (int y = 0; y < 7; y++) {
                        entity_render.add(new ArrayList<>());
                        for (int x = 0; x < 15; x++) {
                            boolean has_space_been_filled = false;
                            for (Entity entity : game.getEntities(cam_x + x-1, cam_y + y-1)) {
                                if (entity instanceof AirUnit){
                                    if (viewMode == ViewMode.AIR){
                                        entity_render.get(y2).add(entity);
                                        has_space_been_filled = true;
                                        break;
                                    }
                                    else if (viewMode == ViewMode.GROUND && !containsGroundUnit(game.getEntities(cam_x + x-1, cam_y + y-1))){
                                        entity_render.get(y2).add(entity);
                                        has_space_been_filled = true;
                                        break;
                                    }
                                } else if (entity instanceof Unit){
                                    if (viewMode == ViewMode.GROUND){
                                        entity_render.get(y2).add(entity);
                                        has_space_been_filled = true;
                                        break;
                                    }
                                    else if (viewMode == ViewMode.AIR && !containsAirUnit(game.getEntities(cam_x + x-1, cam_y + y-1))){
                                        entity_render.get(y2).add(entity);
                                        has_space_been_filled = true;
                                        break;
                                    }
                                }
                            }
                            if (!has_space_been_filled) {
                                entity_render.get(y2).add(null);
                            }
                        }
                        y2++;
                    }

                    // Create entity fog-of-war render

                    ArrayList<Unit> ally_units = new ArrayList<>();
                    for (Entity entity : game.getAllEntitiesCopy()) {
                        if (entity instanceof Unit) {
                            if (team.getTeamUnits().contains(entity)) {
                                ally_units.add((Unit) entity);
                            }
                        }
                    }


                    // Fog of war creation

                    ArrayList<ArrayList<Integer>> fog_of_war = new ArrayList<>();
                    boolean[][] vision = new boolean[lgame.getMapHeight()][lgame.getMapWidth()];

                    y2 = 0;
                    for (int y = 0; y < 7; y++) {
                        fog_of_war.add(new ArrayList<>());
                        for (int x = 0; x < 15; x++) {
                            fog_of_war.get(y2).add(1);
                        }
                        y2++;
                    }

                    for (Unit ally_unit : ally_units) {
                        for (int y = -ally_unit.getFogOfWarRange(); y < ally_unit.getFogOfWarRange() + 1; y++) {
                            for (int x = -ally_unit.getFogOfWarRange(); x < ally_unit.getFogOfWarRange() + 1; x++) {
                                int x1 = (ally_unit.getX() - cam_x) + x;
                                int y1 = (ally_unit.getY() - cam_y) + y;

                                if ((x1 >= 0 && y1 >= 0)) {
                                    if (y1 < fog_of_war.size()) {
                                        if (x1 < fog_of_war.get(y1).size()) {
                                            fog_of_war.get(y1).set(x1, 0);
                                        }
                                    }

                                }

                                if ((x+ally_unit.getX() >= 0 && y+ally_unit.getY() >= 0)){
                                    if (x+ally_unit.getX() < lgame.getMapWidth() && y+ally_unit.getY() < lgame.getMapHeight())
                                        vision[y+ally_unit.getY()][x+ally_unit.getX()] = true;
                                }
                            }
                        }
                    }

                    // Remove out of range in-fog entities

                    ArrayList<Entity> to_remove_entities = new ArrayList<>();

                    for (int y = 0; y < 7; y++) {
                        for (int x = 0; x < 15; x++) {
                            Entity entity = entity_render.get(y).get(x);

                            if (entity != null) {
                                if (!vision[entity.getY()][entity.getX()] || entity.isInvisible()){
                                    to_remove_entities.add(entity);
                                }
                            }
                        }
                    }

                    for (Entity entity : to_remove_entities) {
                        entity_render.get(entity.getY() - (cam_y-1)).set(entity.getX() - (cam_x-1), null);
                    }

                    // Final Entity render

                    ArrayList<ArrayList<String>> entity_render_final = new ArrayList<>();

                    y2 = 0;
                    for (int y = 0; y < 7; y++) {
                        entity_render_final.add(new ArrayList<>());
                        for (int x = 0; x < 15; x++) {
                            String entity = entity_render.get(y).get(x) != null ? entity_render.get(y).get(x).getTextureFileName() : null;
                            entity_render_final.get(y).add(entity);
                        }
                        y2++;
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
                    for (ArrayList<MapObject> mapObjects : lgame.getMap()) {
                        map_tiles.add(new ArrayList<>());
                        for (MapObject tile : mapObjects) {
                            map_tiles.get(y).add(tile.isPassable);
                        }
                        y++;
                    }

                    // Create unit_data_ar

                    ArrayList<ArrayList<HashMap<String, Object>>> unit_data_ar = new ArrayList<>();
                    y2 = 0;
                    for (ArrayList<Entity> ent_row : entity_render) {
                        unit_data_ar.add(new ArrayList<>());
                        for (Entity entity : ent_row) {
                            if (entity != null) {
                                if (entity instanceof Unit unit) {
                                    HashMap<String, Object> unit_data = new HashMap<>();
                                    for (Game.Team team : lgame.getTeams()) {
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
                                    stats.add(unit.getEntrenchment());
                                    unit_data.put("flip", unit.isFlipped());
                                    unit_data.put("stats", stats);
                                    unit_data.put("transparent", unit instanceof AirUnit && viewMode != ViewMode.AIR || !(unit instanceof AirUnit) && viewMode != ViewMode.GROUND);
                                    unit_data_ar.get(y2).add(unit_data);
                                } else {
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
                    if (selected_unit != null) {
                        if (!vision[selected_unit.getY()][selected_unit.getX()]) {
                            selected_unit = null;
                            iselectorbuttonid = null;
                        } else if (selected_unit.unitStats().getHp() <= 0) {
                            selected_unit = null;
                            iselectorbuttonid = null;
                        } else if (!game.getAllEntities().contains(selected_unit)) {
                            selected_unit = null;
                        }
                    }

                    if (selected_unit != null) {
                        HashMap<String, Object> selected_unit_data = new HashMap<>();
                        selected_unit_data.put("properties", selected_unit.getProperties().getProperties());
                        selected_unit_data.put("texture", selected_unit.getTextureFileName());

                        ArrayList<Object> buttons = new ArrayList<>();
                        Unit.IButton iselectorbutton_used = null;

                        if (team.getTeamUnits().contains(selected_unit) && selected_unit.getButtons() != null) {
                            for (Unit.IButton button : selected_unit.getButtons()) {
                                iselectorbutton_used = buildButtonAndGetISelectorButtonUsed(buttons, iselectorbutton_used, button);
                            }
                        } else if (!team.getTeamUnits().contains(selected_unit)) {
                            if (selected_unit.getButtonsEnemy() != null)
                                for (Unit.IButton button : selected_unit.getButtonsEnemy()) {
                                    iselectorbutton_used = buildButtonAndGetISelectorButtonUsed(buttons, iselectorbutton_used, button);
                                }
                        }

                        selected_unit_data.put("buttons", buttons);

                        ArrayList<Object> queue = new ArrayList<>();

                        boolean is_queue_valid = false;

                        if (team.getTeamUnits().contains(selected_unit) && selected_unit != null) {
                            if (selected_unit.getUnitQueue() != null) {
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

                        if (!is_queue_valid) {
                            selected_unit_data.put("queue", 0);
                        } else {
                            selected_unit_data.put("queue", queue);
                        }

                        if (iselectorbuttonid != null) {
                            selected_unit_data.put("iselectorbutton_press", true);
                            if (iselectorbutton_used instanceof Unit.ISelectorButton)
                                selected_unit_data.put("iselectorbutton_data_selector_texture", ((Unit.ISelectorButton) iselectorbutton_used).selector_texture());
                            if (iselectorbutton_used instanceof Unit.ISelectorButtonUnit)
                                selected_unit_data.put("iselectorbutton_data_selector_texture", ((Unit.ISelectorButtonUnit) iselectorbutton_used).selector_texture());
                        } else {
                            selected_unit_data.put("iselectorbutton_press", false);
                        }

                        toBeJsoned.put("selected_unit_data", selected_unit_data);
                    } else {
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
            } finally {
                if (game != null)
                game.lock.unlock();
                lock.unlock();
            }
            return null;
        }

        /***
         * This method creates HashMap from button and returns ISelectorButton used by player
         * @param buttons Button ArrayList
         * @param iselectorbutton_used Currently used selector button if null it will replace it if button checked is used
         * @param button Button to be converted
         * @return selector button used
         */
        private Unit.IButton buildButtonAndGetISelectorButtonUsed(ArrayList<Object> buttons, Unit.IButton iselectorbutton_used, Unit.IButton button) {
            if (button != null) {
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
            } else {
                buttons.add(0);
            }
            return iselectorbutton_used;
        }

        protected String endOfAGameMessage = null;

        public void setEndOfGameMessage(String endOfAGameMessage){
            lock.lock();
            this.endOfAGameMessage = endOfAGameMessage;
            lock.unlock();
        }

        public String getEndOfAGameMessage() {
            lock.lock();
            try {
                return endOfAGameMessage;
            } finally {
                lock.unlock();
            }
        }
    }

    public Lobby(String map_path, ArrayList<String> next_maps){
        this.map_path = map_path;
        this.next_maps = next_maps;
    }

    public Player connect_to_lobby(){
        lobby_lock.lock();
        try {
            if (players.size() == max_player_count) {
                return null;
            } else {
                Player player = new Player();
                players.add(player);
                return player;
            }
        } finally {
            lobby_lock.unlock();
        }
    }

    public void rmFromLobby(Player player){
        lobby_lock.lock();
        players.remove(player);
        lobby_lock.unlock();
    }

    public enum LobbyState {
        WAITING_FOR_PLAYERS,
        STARTED
    }
    public LobbyState current_lobby_state(){
        try {
            lobby_lock.lock();
            return players.size() != max_player_count ? LobbyState.WAITING_FOR_PLAYERS : LobbyState.STARTED;
        } finally {
            lobby_lock.unlock();
        }
    }

    public String asString(){
        // return "" + lobby_name + ": Map: " + map_name + "[" + currentPlayerCount() + "/" + getMaxPlayerCount() + "]";
        try {
            lobby_lock.lock();
            return game_mode + "," + map_name + "," + currentPlayerCount() + "/" + getMaxPlayerCount();
        } finally {
            lobby_lock.unlock();
        }
    }

    public ArrayList<String> getPlayerNicks(){
        lobby_lock.lock();
        ArrayList<String> nicks = new ArrayList<>();
        for (Player player: players){
            nicks.add(player.nickname);
        }
        try {
            return nicks;
        } finally {
            lobby_lock.unlock();
        }
    }

    public String playerNicksString(){
        lobby_lock.lock();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        rootNode.put("type", "NickList");

        ArrayNode node = rootNode.putArray("nicks");
        for (String nick: getPlayerNicks()){
            node.add(nick);
        }

        try {
            return mapper.writer().writeValueAsString(rootNode);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        } finally {
            lobby_lock.unlock();
        }
    }

}
