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

import net.stalemate.core.animation.AnimationController;
import net.stalemate.core.properties.Properties;
import net.stalemate.core.util.IGameController;
import org.jetbrains.annotations.Nullable;

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

    protected int entrenchment = 0;
    /***
     * Entrenchment gain
     */
    protected int et_gain = 0;
    /***
     * Entrenchment max
     */
    protected int et_max = 0;

    public void setEtStats(int et_gain, int et_max){
        this.et_gain = et_gain;
        this.et_max = et_max;
    }

    protected int has_not_moved = 0;

    /***
     * Tracks MoveButton usage
     */
    protected boolean hasMoved = false;

    public void move(){
        hasMoved = true;
    }

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

    public void setEntrenchment(int i) {
        entrenchment = i;
    }

    public int getEntrenchment() {
        return entrenchment;
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

        default boolean canEnemyTeamUseOnOtherEnemyTeamUnit() {return false;}
    }

    /***
     * Button which doesn't need x and y coordinates to work
     * @see ISelectorButton
     */
    public interface IStandardButton extends IButton{
        void action(Unit unit, IGameController gameController);
    }

    /***
     * Base Unit statistics
     */
    public record UnitStats(int hp, int max_hp, int attack_range, int movement_range, int atk, int df, int supply,
                            int max_supply, int armor) {

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

    public interface Buff {
        UnitStats modStats(UnitStats ustats);

        /***
         * This method should also reduce turnTime
         * @param u Unit which has this Buff
         */
        void turnAction(Unit u);

        /***
         * Amount of ticks during which the buff is active
         */
        int getTurnTime();
    }

    /***
     * Buffs which Unit currently has
     */
    private final ArrayList<Buff> buffs = new ArrayList<>();

    public ArrayList<Buff> getBuffs(){
        return buffs;
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
        hasMoved = false;

        has_not_moved += 1;

        if (et_max != 0 && et_gain != 0)
        if (has_not_moved == 3){
            if (entrenchment < et_max)
                entrenchment+=et_gain;
            has_not_moved = 0;
        }

        for (Buff buff: buffs){
            buff.turnAction(this);
        }
    }

    private volatile boolean isAnimationUnsafe = false;

    @Override
    public void update() {
        isAnimationUnsafe = true;
        anim.tick();
        isAnimationUnsafe = false;

        if (hp <= 0){
            game.rmEntity(this);
            game.getEvReg().triggerUnitDeathEvent(this);
            onDeath();
        }

        if (max_supply != -1){
            if (supply <= 0){
                game.rmEntity(this);
                game.getEvReg().triggerUnitDeathEvent(this);
                onDeath();
            }
        }

        if (hasMoved){
            has_not_moved = 0;
            entrenchment = 0;
        }
    }

    public void onDeath(){

    }

    @Override
    public String getTextureFileName() {
        while (isAnimationUnsafe){
            Thread.onSpinWait();
        }
        return anim.getCurrentFrame();
    }

    public void resetTurn(){hasTurnEnded = false;}

    /***
     * @return 3x3 grid with Button classes. If you want to leave a space empty place a null there. (It is one dimensional ArrayList)
     */
    @Nullable
    public abstract ArrayList<IButton> getButtons();

    /***
     * @return 3x3 grid with Button classes. If you want to leave a space empty place a null there. These buttons can be used by enemy team. Can be null. (It is one dimensional ArrayList)
     */
    @Nullable
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
    @Nullable
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

    public int getAtk() {
        return atk + (int)(0.5*entrenchment);
    }

    public int getDf() {
        return df + (int)(0.5*entrenchment);
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

    public Properties getProperties(){
        Properties properties = new Properties();
        properties.put("hp", "" + this.unitStats().getHp() + "/" + this.unitStats().getMaxHp());
        if (this.unitStats().max_supply != -1)
        properties.put("su", "" + this.unitStats().getSupply() + "/" + this.unitStats().getMaxSupply());
        if (this.unitStats().getAtk() > 0)
        properties.put("atk", "" + this.unitStats().getAtk() + (entrenchment>1?"(+"+((int)(0.5*entrenchment))+")":""));
        if (this.getDf() > 0)
        properties.put("df", "" + this.unitStats().getDf() + (entrenchment>1?"(+"+ (int)(0.5*entrenchment) +")":""));
        if (this.unitStats().armor > 0)
        properties.put("ar", "" + this.unitStats().getArmor());
        if (this.entrenchment > 0)
        properties.put("et", "" + this.entrenchment);
        properties.put("name", this.getName());

        properties.put("atk_range", "" + this.unitStats().getAttackRange());
        properties.put("mov_range", "" + this.unitStats().getMovementRange());
        properties.put("ended_turn", this.hasTurnEnded ? "Yes": "No");
        return properties;
    }
}
