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

package net.stalemate.networking.client;

import net.stalemate.core.Unit;
import net.stalemate.swing.StalemateInternalFrame;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.stalemate.log.MakeLog.makeLog;

public class AssetLoader {

    private static HashMap<String, BufferedImage> img_storage = new HashMap<>();
    private static final Logger LOGGER = makeLog(Logger.getLogger(Client.class.getSimpleName()));

    private static String[] resources = {"assets/background.png",
            "assets/default.png",
            "assets/menu/button.png",
            // "assets/menu/select.png",
            // "assets/menu/server_connection_screen.png",
            "assets/mp.png",
            "assets/panel.png",
            // "assets/panel_old.png",
            // "assets/panel_old_2.png",
            "assets/placeholder_ui.png",
            "assets/placeholder_ui_2.png",
            // "assets/placeholder_ui_2_old.png",
            "assets/shovel.png",
            "assets/skull.png",
            "assets/stalemate.png",
            // "assets/team_holder.png",
            "assets/ui/buttons/attack_button.png",
            "assets/ui/buttons/build_base.png",
            "assets/ui/buttons/build_factory.png",
            "assets/ui/buttons/build_fortification.png",
            "assets/ui/buttons/build_no.png",
            "assets/ui/buttons/build_radar.png",
            "assets/ui/buttons/build_supply_station.png",
            "assets/ui/buttons/build_yes.png",
            "assets/ui/buttons/demotorize.png",
            "assets/ui/buttons/load_fortification.png",
            "assets/ui/buttons/machine_gunner_defensive.png",
            "assets/ui/buttons/machine_gunner_offensive.png",
            "assets/ui/buttons/motorize.png",
            "assets/ui/buttons/move_button.png",
            "assets/ui/buttons/placeholder_button.png",
            "assets/ui/buttons/repair_button.png",
            "assets/ui/buttons/resupply_button.png",
            "assets/ui/buttons/sacrifice_button.png",
            "assets/ui/buttons/scrap_button.png",
            "assets/ui/buttons/scrap_unit_queue.png",
            "assets/ui/buttons/set_deployment_point.png",
            "assets/ui/buttons/template.png",
            "assets/ui/buttons/train_anti_tank.png",
            "assets/ui/buttons/train_artillery.png",
            "assets/ui/buttons/train_engineer_button.png",
            "assets/ui/buttons/train_heavy_tank.png",
            "assets/ui/buttons/train_infantry_button.png",
            "assets/ui/buttons/train_light_tank_button.png",
            "assets/ui/buttons/train_machine_gunner.png",
            "assets/ui/buttons/train_motorized.png",
            "assets/ui/buttons/train_tankette.png",
            "assets/ui/buttons/train_template.png",
            "assets/ui/selectors/ui_attack.png",
            "assets/ui/selectors/ui_move.png",
            "assets/ui/selectors/ui_repair.png",
            "assets/ui/selectors/ui_resupply.png",
            "assets/ui/selectors/ui_select.png",
            "assets/units/anti_tank_attack_1.png",
            "assets/units/anti_tank_attack_2.png",
            "assets/units/anti_tank_attack_3.png",
            "assets/units/anti_tank_attack_4.png",
            "assets/units/anti_tank_idle.png",
            "assets/units/artillery_attack_1.png",
            "assets/units/artillery_attack_2.png",
            "assets/units/artillery_attack_3.png",
            "assets/units/artillery_attack_4.png",
            "assets/units/artillery_idle.png",
            "assets/units/building_military_tent.png",
            "assets/units/building_radar.png",
            "assets/units/engineer_fire_1.png",
            "assets/units/engineer_fire_2.png",
            "assets/units/engineer_fire_3.png",
            "assets/units/engineer_fire_4.png",
            "assets/units/engineer_fire_5.png",
            "assets/units/engineer_idle_1.png",
            "assets/units/engineer_idle_2.png",
            "assets/units/fortification.png",
            "assets/units/fortification_attack_1.png",
            "assets/units/fortification_attack_2.png",
            "assets/units/fortification_attack_3.png",
            "assets/units/fortification_attack_4.png",
            "assets/units/fortification_attack_5.png",
            "assets/units/fortification_attack_6.png",
            "assets/units/fortification_attack_7.png",
            "assets/units/heavy_tank_attack_0.png",
            "assets/units/heavy_tank_attack_1.png",
            "assets/units/heavy_tank_attack_2.png",
            "assets/units/heavy_tank_attack_3.png",
            "assets/units/heavy_tank_idle.png",
            "assets/units/light_tank_attack_1.png",
            "assets/units/light_tank_attack_2.png",
            "assets/units/light_tank_attack_3.png",
            "assets/units/light_tank_idle_1.png",
            "assets/units/light_tank_idle_2.png",
            "assets/units/light_tank_idle_3.png",
            "assets/units/light_tank_idle_4.png",
            "assets/units/light_tank_idle_5.png",
            "assets/units/machine_gunner_fire_1.png",
            "assets/units/machine_gunner_fire_2.png",
            "assets/units/machine_gunner_fire_3.png",
            "assets/units/machine_gunner_idle_1.png",
            "assets/units/machine_gunner_idle_2.png",
            "assets/units/military_tent_build.png",
            "assets/units/motorized_unit_idle.png",
            "assets/units/rifleman_fire_1.png",
            "assets/units/rifleman_fire_2.png",
            "assets/units/rifleman_fire_3.png",
            "assets/units/rifleman_fire_4.png",
            "assets/units/rifleman_fire_5.png",
            "assets/units/rifleman_idle_1.png",
            "assets/units/rifleman_idle_2.png",
            "assets/units/supply_station.png",
            "assets/units/supply_station_build.png",
            "assets/units/tankette_attack_1.png",
            "assets/units/tankette_attack_2.png",
            "assets/units/tankette_attack_3.png",
            "assets/units/tankette_idle.png",
            "assets/units/tank_factory.png",
            "assets/units/tank_factory_build.png",
    };

    private static String[] tiles = {"tiles/forest1.png",
            "tiles/forest2.png",
            "tiles/forest3.png",
            "tiles/grass1.png",
            "tiles/grass2.png",
            "tiles/grass3.png",
            "tiles/house1.png",
            "tiles/house2.png",
            "tiles/house3.png",
            "tiles/house4.png",
            "tiles/house4destroyed.png",
            "tiles/mountain.png",
            "tiles/mountain2.png",
            "tiles/mountain3.png",
            "tiles/mountain4.png",
            "tiles/road-1.png",
            "tiles/road1.png",
            "tiles/road2-1.png",
            "tiles/road2-2.png",
            "tiles/road2-3.png",
            "tiles/road2.png",
            "tiles/road3.png",
            "tiles/roadend.png",
            "tiles/roadend1.png",
            "tiles/roadend2.png",
            "tiles/roadend3.png",
    };

    private static Font basis33;

    public static Font getBasis33(){
        return basis33;
    }

    public static void loadAll(){
        LOGGER.log(Level.INFO, "Loading all assets...");
        for (String res: resources){
            load(res);
        }

        for (String tile: tiles){
            load(tile);
        }

        try {
            basis33 = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(StalemateInternalFrame.class.getClassLoader().getResource("basis33/basis33.ttf")).openStream());
        } catch (FontFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.log(Level.INFO, "Loaded basis33 font");
    }

    public static BufferedImage load(String img_path){
        if (!img_storage.containsKey(img_path)){
            try {
                LOGGER.log(Level.INFO,"Loading " + img_path);
                img_storage.put(img_path, ImageIO.read(Objects.requireNonNull(AssetLoader.class.getClassLoader().getResource(img_path))));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load " + img_path);
                return null;
            }
        }
        return img_storage.get(img_path);
    }

}
