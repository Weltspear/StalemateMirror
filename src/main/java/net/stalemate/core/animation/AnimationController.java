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

package net.stalemate.core.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AnimationController {
    protected Animation current_animation = null;

    protected final HashMap<String, Animation> animationMap = new HashMap<>();

    protected final ArrayList<ArrayList<String>> shifts = new ArrayList<>();
    int anim_tick = 0;

    public AnimationController(){

    }

    public void addAnimation(String anim_name, Animation animation){
        animation.stopLoop();
        animationMap.put(anim_name, animation);
    }

    public void addShift(String a1, String a2){
        ArrayList<String> shift = new ArrayList<>();
        shift.add(a1);
        shift.add(a2);
        shifts.add(shift);
    }

    public void setCurrentAnimation(String a){
        current_animation = animationMap.get(a);
        current_animation.current_frame = 0;
        anim_tick = 0;
    }

    public String getCurrentFrame(){
        if (current_animation.hasEnded()) {
            boolean hasShift = false;
            for (ArrayList<String> shift : shifts) {
                for (Map.Entry<String, Animation> entry : animationMap.entrySet()) {
                    if (entry.getValue() == current_animation) {
                        if (shift.get(0).equals(entry.getKey())) {
                            current_animation = animationMap.get(shift.get(1));
                            current_animation.current_frame = 0;
                            anim_tick = 0;
                            hasShift = true;
                            break;
                        }
                    }
                }
            }
            if (!hasShift){
                current_animation.current_frame = 0;
            }
        }
        if (current_animation != null) return current_animation.getFrame();
        else return null;
    }

    public boolean containsAnimation(String a){
        return animationMap.containsKey(a);
    }

    public void tick(){
        if (current_animation != null){
            anim_tick++;
            if (current_animation.hasEnded()) {
                boolean hasShift = false;
                for (ArrayList<String> shift : shifts) {
                    for (Map.Entry<String, Animation> entry : animationMap.entrySet()) {
                        if (entry.getValue() == current_animation) {
                            if (shift.get(0).equals(entry.getKey())) {
                                current_animation = animationMap.get(shift.get(1));
                                anim_tick = 0;
                                hasShift = true;
                                break;
                            }
                        }
                    }
                }
                if (!hasShift){
                    current_animation.current_frame = 0;
                }
            }

            if (anim_tick == current_animation.getTickSleep()) {
                anim_tick = 0;
                current_animation.tick();
            }
        }
    }


}
