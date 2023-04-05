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

package net.stalemate.server.core.pathfinding;

import net.stalemate.server.core.Entity;
import net.stalemate.server.core.controller.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class Pathfinding {

    public static class Node implements Cloneable {
        public int x;
        public int y;
        public Node parent;

        public int f = 0;
        public int g = 0;
        public int h = 0;

        public Node(int x, int y){
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return x == node.x &
                    y == node.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, f, g, h);
        }

        @Override
        protected Node clone() throws CloneNotSupportedException {
            Node cloned = new Node(x, y);
            cloned.f = f;
            cloned.g = g;
            cloned.h = h;
            cloned.parent = parent;
            return cloned;
        }
    }

    private static boolean[][] buildPassabilityArray(Game g){
        boolean[][] npassable = new boolean[g.getMapHeight()][g.getMapWidth()];

        for (int y = 0; y < g.getMapHeight(); y++){
            for (int x = 0; x < g.getMapWidth(); x++){
                if (!g.getMapObject(x, y).isPassable()){
                    npassable[y][x] = true;
                    continue;
                }

                for (Entity entity : g.getEntities(x, y)) {
                    if (!entity.isPassable()) {
                        npassable[y][x] = true;
                        break;
                    }
                }
            }
        }

        return npassable;
    }

    public static boolean isCoordPassable2(int x, int y, boolean[][] npassable){
        if (x < 0 || x >= npassable[0].length || y < 0 || y >= npassable.length)
            return false;
        return !npassable[y][x];
    }

    public static boolean isCoordPassable(int x, int y, Game g){
        if (x < 0 || x >= g.getMapWidth() || y < 0 || y >= g.getMapHeight())
            return false;
        return isCoordPassable2(x, y, buildPassabilityArray(g));
    }

    private static ArrayList<Node> buildPath(Node goal, Node successor, Node start){
        ArrayList<Node> final_p = new ArrayList<>();
        final_p.add(goal);
        while (successor.parent != null){
            if (successor.x == start.x && successor.y == start.y){
                break;
            }
            final_p.add(successor.parent);
            successor = successor.parent;
        }

        final_p.remove(final_p.size()-1);

        Collections.reverse(final_p);
        return final_p;
    }

    // optimize passability code
    public static ArrayList<Node> a_star(int gt_x, int gt_y, int x, int y, Game g, boolean return_closest_node_if_not_found){
        ArrayList<Node> open = new ArrayList<>();
        ArrayList<Node> closed = new ArrayList<>();
        Node start = new Node(x, y);

        Node goal = new Node(gt_x, gt_y);

        boolean[][] npassable = buildPassabilityArray(g);

        if (gt_x < 0 | gt_y < 0 |
                x < 0 | y < 0){
            return null;
        }

        open.add(start);

        while (open.size() > 0){
            Node q = open.stream().min(Comparator.comparingInt(a -> a.f)).get();
            open.remove(q);

            ArrayList<Node> successors = new ArrayList<>();

            successors.add(new Node(q.x + 1, q.y));
            successors.add(new Node(q.x - 1, q.y));
            successors.add(new Node(q.x, q.y + 1));
            successors.add(new Node(q.x, q.y - 1));
            successors.add(new Node(q.x + 1, q.y + 1));
            successors.add(new Node(q.x + 1, q.y - 1));
            successors.add(new Node(q.x - 1, q.y + 1));
            successors.add(new Node(q.x - 1, q.y - 1));

            for (Node successor: successors){
                if (successor.equals(goal)){
                    closed.add(q);

                    successor.parent = q;
                    return buildPath(goal, successor, start);
                }

                successor.g = q.g + 1;
                successor.h = (Math.abs(successor.x - goal.x)*Math.abs(successor.x - goal.x)
                        + Math.abs(successor.y - goal.y)*Math.abs(successor.y - goal.y));
                successor.f = successor.g+successor.h;
                successor.parent = q;

                if (open.contains(successor)||closed.contains(successor)){continue;}

                if (successor.x < 0 | successor.y < 0){
                    continue;
                }

                if (isCoordPassable2(successor.x, successor.y, npassable)){
                    open.add(successor);
                }
            }

            closed.add(q);

        }

        if (return_closest_node_if_not_found){
            ArrayList<Integer> h_s = new ArrayList<>();
            for (Node node: closed){
                h_s.add(node.h);
            }

            int idx = h_s.indexOf(Collections.min(h_s));

            return buildPath(goal, closed.get(idx), start);
        }
        return null;
    }


}

