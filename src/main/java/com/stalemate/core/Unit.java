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

package com.stalemate.core;

import com.stalemate.core.animation.AnimationController;
import com.stalemate.core.event.EventListenerRegistry;
import com.stalemate.core.upgrade.Upgrade;
import com.stalemate.core.util.IGameController;

import java.util.ArrayList;

public abstract class Unit extends Entity implements Entity.ServerUpdateTick {
    protected int hp;
    protected int max_hp;

    protected int attack_range;
    protected int movement_range;

    protected int atk;
    protected int df;

    protected int supply;
    protected int max_supply;

    protected int armor;

    protected int fog_of_war_range = 2;
    public int getFogOfWarRange() {
        return fog_of_war_range;
    }

    protected final AnimationController anim;

    protected String name;

    protected boolean hasTurnEnded;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasTurnEnded() {
        return hasTurnEnded;
    }

    public void endTurn() {
        this.hasTurnEnded = true;
    }

    private final ArrayList<Upgrade<?>> applied_upgrades = new ArrayList<>();

    public void addUpgrade(Upgrade<?> u){
        applied_upgrades.add(u);
    }

    public boolean hasUpgradeBeenApplied(Upgrade<?> u){
        for (Upgrade<?> u2: applied_upgrades){
            if (u2.id() == u.id()){
                return true;
            }
        }
        return false;
    }

    public interface IButton{
        /***
         * @return Button keybind
         */
        String bind();

        /***
         * @return Button texture
         */
        String texture();

        /***
         * @return Button identifier
         */
        String identifier();
    }

    /***
     * Button which needs a selection of x and y coordinates to work
     */
    public interface ISelectorButton extends IButton{
        void action(int x, int y, Unit unit, IGameController gameController);
        int selector_range();
        String selector_texture();
    }

    /***
     * Button which needs a selection another unit to work
     */
    public interface ISelectorButtonUnit extends IButton{
        void action(Unit selected_unit, Unit unit, IGameController gameController);
        int selector_range();
        String selector_texture();

        boolean isUsedOnOurUnit();
        boolean isUsedOnEnemy();
        boolean isUsedOnAlliedUnit();
    }

    /***
     * Button which doesn't need x and y coordinates to work
     * @see ISelectorButton
     */
    public interface IStandardButton extends IButton{
        void action(Unit unit, IGameController gameController);
    }

    /***
     * Unit statistics
     */
    public static class UnitStats {
        protected final int hp;
        protected final int max_hp;

        protected final int attack_range;
        protected final int movement_range;

        protected final int atk;
        protected final int df;

        protected final int supply;
        protected final int max_supply;

        protected final int armor;

        public UnitStats(int hp, int max_hp, int attack_range, int movement_range, int atk, int df, int supply, int max_supply, int armor){
            this.hp = hp;
            this.max_hp = max_hp;
            this.attack_range = attack_range;
            this.movement_range = movement_range;
            this.atk = atk;
            this.df = df;
            this.supply = supply;
            this.max_supply = max_supply;
            this.armor = armor;
        }

        public int getAtk() {
            return atk;
        }

        public int getDf() {
            return df;
        }

        public int getHp() {
            return hp;
        }

        public int getMaxHp() {
            return max_hp;
        }

        public int getAttackRange() {
            return attack_range;
        }

        public int getMovementRange() {
            return movement_range;
        }

        public int getSupply() {
            return supply;
        }

        public int getMaxSupply() {
            return max_supply;
        }

        public int getArmor() {
            return armor;
        }
    }

    /***
     * NOTE: If you don't want unit to have supply set <code>UnitStats.supply</code> to -1
     */
    public Unit(int x, int y, IGameController game, UnitStats unitStats, AnimationController anim, String name) {
        super(x, y, game);

        this.anim = anim;
        this.name = name;

        isPassable = false;

        atk = unitStats.getAtk();
        hp = unitStats.getHp();
        max_hp = unitStats.getMaxHp();
        df = unitStats.getDf();
        movement_range = unitStats.getMovementRange();
        attack_range = unitStats.getAttackRange();
        supply = unitStats.getSupply();
        max_supply = unitStats.getMaxSupply();
        armor = unitStats.getArmor();
    }

    public String getName() {
        return name;
    }

    @Override
    public void turnUpdate() {
        hasTurnEnded = false;
    }

    @Override
    public void update() {
        anim.tick();

        if (hp <= 0){
            game.rmEntity(this);
            game.getEvReg().triggerUnitDeathEvent(this);
        }

        if (max_supply != -1){
            if (supply <= 0){
                game.rmEntity(this);
                game.getEvReg().triggerUnitDeathEvent(this);
            }
        }
    }

    @Override
    public String getTextureFileName() {
        return anim.getCurrentFrame();
    }

    public void resetTurn(){hasTurnEnded = false;}

    /***
     * @return 3x3 grid with Button classes. If you want to leave a space empty place a null there. (It is one dimensional ArrayList)
     */
    public abstract ArrayList<IButton> getButtons();

    /***
     * @return 3x3 grid with Button classes. If you want to leave a space empty place a null there. These buttons can be used by enemy team. Can be null. (It is one dimensional ArrayList)
     */
    public ArrayList<IButton> getButtonsEnemy(){return null;}

    public UnitStats unitStats(){
        return new UnitStats(hp, max_hp, attack_range, movement_range, atk, df, supply, max_supply, armor);
    }

    /***
     * Used for rendering purposes. Shouldn't be overridden by other units.
     */
    public boolean isEntityInFogOfWarRange(Entity entity){
        return ((this.x-fog_of_war_range <= entity.getX() && entity.getX() <= this.x+fog_of_war_range) & (this.y-fog_of_war_range <= entity.getY() && entity.getY() <= this.y+fog_of_war_range));
    }

    public void damage(int dmg){
        dmg -= armor;
        if (dmg <= 0){
            dmg = 1;
        }
        this.hp -= dmg;
    }

    @Deprecated
    public void modHP(int HP){
        hp += HP;
    }

    public AnimationController getAnimationController() {
        return anim;
    }

    public void consumeSupply(int su){
        this.supply -= su;
    }

    /***
     * This "queue" will be rendered on the panel on the left
     */
    public static class UnitQueue {
        public record QueueMember(String texture, int turn_time){}

        private final ArrayList<QueueMember> queueMembers = new ArrayList<>();

        public UnitQueue(){
        }

        public void addQueueMember(QueueMember q){
            if (queueMembers.size() != 9)
            queueMembers.add(q);
        }

        public int getSize(){
            return queueMembers.size();
        }

        @SuppressWarnings("unchecked")
        public ArrayList<QueueMember> getQueueMembers() {return (ArrayList<QueueMember>) queueMembers.clone();}
    }

    /***
     * This method returns a <code>UnitQueue</code> which is going to be rendered by client
     */
    public UnitQueue getUnitQueue(){return null;}

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return max_hp;
    }

    public void setMaxHp(int max_hp) {
        this.max_hp = max_hp;
    }

    public int getAttackRange() {
        return attack_range;
    }

    public void setAttackRange(int attack_range) {
        this.attack_range = attack_range;
    }

    public int getMovementRange() {
        return movement_range;
    }

    public void setMovementRange(int movement_range) {
        this.movement_range = movement_range;
    }

    public int getAtk() {
        return atk;
    }

    public void setAtk(int atk) {
        this.atk = atk;
    }

    public int getDf() {
        return df;
    }

    public void setDf(int df) {
        this.df = df;
    }

    public int getSupply() {
        return supply;
    }

    public void setSupply(int supply) {
        this.supply = supply;
    }

    public int getMaxSupply() {
        return max_supply;
    }

    public void setMaxSupply(int max_supply) {
        this.max_supply = max_supply;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }

    public void setFogOfWarRange(int fog_of_war_range) {
        this.fog_of_war_range = fog_of_war_range;
    }
}
