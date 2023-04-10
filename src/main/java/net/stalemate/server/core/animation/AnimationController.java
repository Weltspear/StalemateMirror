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
import java.util.HashMap;
import java.util.Map;

public class AnimationController {
    private Animation current_animation = null;

    private final HashMap<String, Animation> animationMap = new HashMap<>();
    private final HashMap<Animation, String> animationMap2 = new HashMap<>();

    protected final HashMap<String, String> shifts = new HashMap<>();
    int anim_tick = 0;

    public AnimationController(){

    }

    public void addAnimation(String anim_name, Animation animation){
        animationMap.put(anim_name, animation);
        animationMap2.put(animation, anim_name);
    }

    /***
     * Adds a shift from animation <code>a1</code> to animation <code>a2</code>
     */
    public void addShift(String a1, String a2){
        shifts.put(a1, a2);
    }

    public void setCurrentAnimation(String a){
        current_animation = animationMap.get(a);
        current_animation.setCurrentFrameIndex(0);
        anim_tick = 0;
    }

    public String getCurrentFrame(){
        if (current_animation != null) {
            return current_animation.getFrame();
        }
        else return null;
    }

    public Animation getCurrentAnimation(){
        return current_animation;
    }

    public boolean containsAnimation(String a){
        return animationMap.containsKey(a);
    }

    public Animation getAnimation(String a){
        return animationMap.get(a);
    }

    public String getAnimationName(Animation a) {return animationMap2.get(a);}

    public void tick(){
        if (current_animation != null){
            anim_tick++;

            if (anim_tick == current_animation.getTickSleep()) {
                anim_tick = 0;
                current_animation.tick();
            }

            if (current_animation.hasEnded()) {
                boolean hasShift = false;

                if (shifts.containsKey(animationMap2.get(current_animation))){
                    current_animation = animationMap.get(shifts.get(animationMap2.get(current_animation)));
                    current_animation.setCurrentFrameIndex(0);
                    anim_tick = 0;
                    hasShift = true;
                }

                if (!hasShift){
                    current_animation.setCurrentFrameIndex(0);
                }
            }
        }
    }


}
