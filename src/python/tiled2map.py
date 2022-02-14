#  Stalemate Game
#  Copyright (C) 2022 Weltspear
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU Affero General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU Affero General Public License for more details.
#
#  You should have received a copy of the GNU Affero General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.

# Converts tiled map to stalemate map

import json
import sys
from PIL import ImageColor


def getPassability(dct):
    try:
        if dct["type"] == "passable":
            return True
        if dct["type"] == "unpassable":
            return False
    except KeyError:
        return True


def import_tileset(filename):
    loaded = json.load(open(filename, 'r'))
    tileset_processed = []

    for tile in loaded["tiles"]:
        tileset_processed.append(
            {"name": str(tile["id"] + 1), "asset_type": "internal", "isPassable": getPassability(tile)
                , "tileDir": tile["image"]})

    return tileset_processed


def is_tile_available(id: str, tileset: list):
    isAvailable = False
    for tile in tileset:
        if id == tile["name"]:
            isAvailable = True
            break

    if not isAvailable:
        raise Exception(
            f"Tile not found in tileset \"{id}\". NOTE: flipping textures in Tiled editor is not supported.")


def find_property(property: str, type_: str, properties: list):
    for p in properties:
        if p["type"] == type_:
            if p["name"] == property:
                return p["value"]

    raise Exception(f"Property \"{property}\" was not found")


def import_map(tileset_filename, map_filename):
    tileset_data = import_tileset(tileset_filename)

    map_unprocessed = json.load(open(map_filename, 'r'))

    layers = map_unprocessed["layers"]
    is_first = True

    map_ = []
    team_data = []
    entity_data = []

    for layer in layers:
        if layer["type"] == "tilelayer" and is_first and layer["visible"]:
            i = 0
            for id_ in layer["data"]:
                map_.append([])
                current_y = i / layer["height"]
                is_tile_available(str(id_), tileset_data)
                map_[int(current_y)].append(str(id_))

                i += 1
            is_first = False
        elif layer["type"] == "tilelayer" and layer["visible"]:
            i = 0
            for id_ in layer["data"]:
                map_.append([])
                current_y = i / layer["height"]
                current_x = i % layer["height"]
                if str(id_) != "0":
                    is_tile_available(str(id_), tileset_data)
                map_[int(current_y)][int(current_x)] = str(id_) if str(id_) != "0" else map_[int(current_y)][
                    int(current_x)]

                i += 1
        elif layer["type"] == "objectgroup" and layer["visible"]:
            for object_ in layer["objects"]:
                if 'point' in object_:
                    if object_["type"] == "TeamObj":
                        properties = object_["properties"]
                        try:
                            team = {"id": find_property("id", "string", properties),
                                    "rgb": ImageColor.getrgb(find_property("rgb", "color", properties)),
                                    # Color conversion doesn't work correctly
                                    "enable_dev": find_property("enable_dev", "bool", properties)}
                            color = list(team["rgb"])
                            del color[3]
                            team["rgb"] = color
                            team_data.append(team)
                        except Exception as e:
                            print("Failed to load team")
                            print(str(e))
                    if object_["type"] == "Entity":
                        properties = object_["properties"]
                        try:
                            clazz = None
                            try:
                                clazz = find_property("class", "string", properties)
                            except Exception:
                                clazz = "unit"

                            entity = {
                                "x": int(object_["x"] / 32),
                                "y": int(object_["y"] / 32),
                                "class": clazz,
                                "team": find_property("team", "string", properties),
                                "id": find_property("id", "string", properties)
                            }
                            entity_data.append(entity)
                        except Exception as e:
                            print("Failed to load entity")
                            print(str(e))
                else:
                    print("WARNING: Found non-point object. Non-point objects are not supported")
            # print(f"ObjectGroup was found, ObjectGroups currently unsupported. ObjectGroup name \"{layer['name']}\"")

    mode = find_property("mode", "string", map_unprocessed["properties"])
    name = find_property("name", "string", map_unprocessed["properties"])

    return {"map_data": {"tileset_data": tileset_data, "team_data": team_data, "name": name, "mode": mode},
            "mpobjects": map_,
            "entity_data": entity_data}


if __name__ == "__main__":
    if len(sys.argv) == 4:
        # tiled2map tileset_filename map_filename map_out
        json.dump(import_map(sys.argv[1], sys.argv[2]), open(sys.argv[3], 'w'))
    else:
        print("Incorrect argument count")
