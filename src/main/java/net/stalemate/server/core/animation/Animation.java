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

package net.stalemate.server.core.animation;

import java.util.ArrayList;

public class Animation {
    private final ArrayList<String> frames = new ArrayList<>();
    private int current_frame = 0;
    /***
     * Number of ticks to sleep before going to a next frame
     */
    protected final int tick_sleep;

    public Animation(int tick_sleep){
        this.tick_sleep = tick_sleep;
    }

    public void tick(){
        current_frame++;
    }

    public boolean hasEnded(){
        return ((current_frame == frames.size()));
    }

    public int getCurrentFrameIndex() {
        return current_frame;
    }

    public void setCurrentFrameIndex(int current_frame) {
        this.current_frame = current_frame;
    }

    public int getAnimationSize(){
        return frames.size();
    }

    public void addFrame(String frame){
        frames.add(frame);
    }

    public String getFrame(){
        return frames.get(current_frame);
    }

    public int getTickSleep() {
        return tick_sleep;
    }

    public Animation copy(){
        Animation anim = new Animation(tick_sleep);
        for (String frame: frames){
            anim.addFrame(frame);
        }
        return anim;
    }
}
