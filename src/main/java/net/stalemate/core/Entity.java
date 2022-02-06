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

package net.stalemate.core;

import net.stalemate.core.util.IGameController;

/***
 * An object on the grid which can move
 */
public abstract class Entity {
    private final double id = Math.random();
    public double getId(){return id;}

    // To turn entity texture left
    /***
     * If true entity texture is flipped left
     */
    public boolean flip = false;

    /***
     * @see Entity#unflip()
     */
    public void flip(){
        flip = true;
    }

    /***
     * @see Entity#flip()
     */
    public void unflip(){
        flip = false;
    }

    public boolean isFlipped() {
        return flip;
    }

    /***
     * If an <code>Entity</code> implements this interface
     * each tick <code>update</code> method will be called
     */
    public interface ServerUpdateTick{
        /***
         * An action performed each server tick.
         */
        void update();
    }

    protected int x;
    protected int y;
    protected final IGameController game;
    protected boolean isInvisible = false;

    public boolean isInvisible() {
        return isInvisible;
    }

    protected boolean isPassable;

    /***
     * @return Name of a texture to render if returns null then it is treated as invisible entity
     */
    public abstract String getTextureFileName();

    public Entity(int x, int y, IGameController game){
        this.x = x;
        this.y = y;
        this.game = game;
    }

    /***
     * An action performed after user does his turn
     */
    public abstract void turnUpdate();

    public int getY() { return y; }
    public int getX() { return x; }

    public boolean isPassable() {
        return isPassable;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}
