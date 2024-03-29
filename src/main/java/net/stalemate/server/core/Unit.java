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

package net.stalemate.server.core;

import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.controller.Game;
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
    protected int et_gain;
    /***
     * Entrenchment max
     */
    protected int et_max;

    protected int has_not_moved = 0;

    /***
     * Tracks MoveButton usage
     */
    protected boolean hasMoved = false;

    /***
     * Amount of moves remaining for the unit this turn
     * NOTE: Set this to -1 if you don't want your unit to have move amount
     */
    protected int move_amount = 0;
    protected int turn_move_amount = 0;

    /***
     * Sets the amount of moves remaining for the unit this turn
     */
    public void setMoveAmount(int m){
        this.move_amount = m;
    }

    /***
     * The amount of moves available each turn
     */
    public int getTurnMoveAmount(){
        return turn_move_amount;
    }

    public int getMoveAmount(){
        return move_amount;
    }

    /***
     * Tells the game that this unit has recently moved
     */
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

        default boolean canBeUsedWhenOtherTeamsTurn(){
            return false;
        }
    }

    public enum Layer{
        AIR,
        GROUND
    }

    /***
     * Button which needs a selection of x and y coordinates to work
     */
    public interface ISelectorButton extends IButton{
        void action(int x, int y, Unit unit, Game gameController);
        int selector_range();
        String selector_texture();

        default Layer getLayer(){
            return Layer.GROUND;
        }
    }

    /***
     * Button which needs a selection another unit to work
     */
    public interface ISelectorButtonUnit extends IButton{
        void action(Unit selected_unit, Unit unit, Game gameController);
        int selector_range();
        String selector_texture();

        boolean isUsedOnOurUnit();
        boolean isUsedOnEnemy();
        boolean isUsedOnAlliedUnit();

        default boolean canEnemyTeamUseOnOtherEnemyTeamUnit() {return false;}

        default Layer getLayer(){
            return Layer.GROUND;
        }
    }

    /***
     * Button which doesn't need x and y coordinates to work
     * @see ISelectorButton
     */
    public interface IStandardButton extends IButton{
        void action(Unit unit, Game gameController);
    }

    /***
     * If mouse cursor is over a button which implements this, the coord that was specified will be highlighted.
     */
    public interface IHighlightCoordButton {
        record HighlightCoord(int x, int y, int rgb){}

        HighlightCoord highlightCoord();
    }

    /***
     * Base Unit statistics
     */
    public record UnitStats(int hp, int max_hp, int attack_range, int movement_range, int atk, int df, int supply,
                            int max_supply, int armor, int et_gain, int et_max) {

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

        public int getEtMax() {
            return et_max;
        }

        public int getEtGain() {
            return et_gain;
        }
    }

    public abstract static class Buff {
        protected int turnTime;
        private boolean hasBeenApplied = false;
        public Buff(int turnTime){
            this.turnTime = turnTime;
        }
        public abstract UnitStats modifyStats(UnitStats ustats);

        private UnitStats revertModification(UnitStats ustats){

            UnitStats zero =
                    new UnitStats(0,0,0,0,0,0,0,0,0,0,0);
            UnitStats modified = modifyStats(zero);

            if (modified == null){
                return ustats;
            }

            return new UnitStats(
                    ustats.hp-modified.hp,
                    ustats.max_hp-modified.max_hp,
                    ustats.attack_range-modified.attack_range,
                    ustats.movement_range-modified.movement_range,
                    ustats.atk-modified.atk,
                    ustats.df-modified.df,
                    ustats.supply-modified.supply,
                    ustats.max_supply-modified.max_supply,
                    ustats.armor-modified.armor,
                    ustats.et_gain-modified.et_gain,
                    ustats.et_max-modified.et_max
            );
        }

        private void setApplied(){
            hasBeenApplied = true;
        }

        public boolean hasBeenApplied(){
            return hasBeenApplied;
        }

        /***
         * This method should also reduce turnTime
         * @param u Unit which has this Buff
         */
        public abstract void turnAction(Unit u);

        /***
         * Amount of ticks during which the buff is active
         */
        public int getTurnTime(){
            return turnTime;
        }
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
    public Unit(int x, int y, Game game, UnitStats unitStats, AnimationController anim, String name) {
        super(x, y, game);

        this.anim = anim;
        this.name = name;

        isPassable = false;

        applyUnitStats(unitStats);
    }

    public void applyUnitStats(UnitStats stats){
        atk = stats.getAtk();
        hp = stats.getHp();
        max_hp = stats.getMaxHp();
        df = stats.getDf();
        movement_range = stats.getMovementRange();
        attack_range = stats.getAttackRange();
        supply = stats.getSupply();
        max_supply = stats.getMaxSupply();
        armor = stats.getArmor();
        et_max = stats.getEtMax();
        et_gain = stats.getEtGain();
    }

    public String getName() {
        return name;
    }

    @Override
    public void turnUpdate() {
        hasTurnEnded = false;
        hasMoved = false;
        if (move_amount != -1){
            move_amount = turn_move_amount;
        }

        has_not_moved += 1;

        if (et_max != 0 && et_gain != 0)
            if (has_not_moved == 3) {
                if (entrenchment < et_max)
                    entrenchment += et_gain;
                has_not_moved = 0;
            }


        unitStatsBuff();

        ArrayList<Buff> to_be_removed_b = new ArrayList<>();

        if (buffs.size() > 0)
            for (Buff buff : buffs) {
                if (buff.getTurnTime() == 0) {
                    to_be_removed_b.add(buff);
                    UnitStats ustats = unitStats();
                    UnitStats reverted = buff.revertModification(ustats);
                    applyUnitStats(reverted);
                } else
                    buff.turnAction(this);
            }


        buffs.removeAll(to_be_removed_b);
    }

    @Override
    public void update() {
        anim.tick();

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

    /***
     * This method is called after all teams did their turn
     */
    public void allTeamTurnUpdate(){
        protector = null;
    }

    public void onDeath(){

    }

    @Override
    public String getTextureFileName() {
        return anim.getCurrentFrame();
    }

    /***
     * @return 3x3 grid with Button classes.
     */
    @Nullable
    public abstract IButton[] getButtons();

    /***
     * @return 3x3 grid with Button classes.
     */
    @Nullable
    public IButton[] getButtonsEnemy(){return null;}

    public UnitStats unitStats(){
        return new UnitStats(hp, max_hp, attack_range, movement_range, atk, df, supply, max_supply, armor, et_gain, et_max);
    }

    /***
     * Calculate UnitStats with all buffs taken into account
     */
    public void unitStatsBuff(){
        if (buffs.size() > 0) {
            UnitStats unitStats = unitStats();
            for (Buff b : buffs) {
                if (!b.hasBeenApplied()) {
                    unitStats = b.modifyStats(unitStats);
                    b.setApplied();
                }
            }
            applyUnitStats(unitStats);
        }
    }

    /***
     * Damages unit takes armor into account
     */
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

    /***
     * Protector unit receives damage when attacked
     */
    private Unit protector = null;

    /**
     * Sets protector to <code>other</code>
     * @param other new protector
     */
    public void protectUnitWith(Unit other){
        protector = other;
    }

    public Unit getProtector(){
        return protector;
    }

    public Properties getProperties(){
        Properties properties = new Properties();
        properties.put("hp", this.unitStats().getHp() + "/" + this.unitStats().getMaxHp());
        if (this.unitStats().max_supply != -1)
            properties.put("su", this.unitStats().getSupply() + "/" + this.unitStats().getMaxSupply());
        if (this.unitStats().getAtk() > 0)
            properties.put("atk", this.unitStats().getAtk() + (entrenchment>1?"(+"+((int)(0.5*entrenchment))+")":""));
        if (this.getDf() > 0)
            properties.put("df", this.unitStats().getDf() + (entrenchment>1?"(+"+ (int)(0.5*entrenchment) +")":""));
        if (this.unitStats().armor > 0)
            properties.put("ar", String.valueOf(this.unitStats().getArmor()));
        if (this.entrenchment > 0)
            properties.put("et", String.valueOf(this.entrenchment));
        properties.put("name", this.getName());

        properties.put("atk_range", String.valueOf(this.unitStats().getAttackRange()));
        properties.put("mov_range", String.valueOf(this.unitStats().getMovementRange()));

        if (move_amount != -1) {
            if (hasTurnEnded) {
                properties.put("move_amount", String.valueOf(0));
            } else {
                properties.put("move_amount", String.valueOf(move_amount));
            }
        }

        properties.put("ended_turn", this.hasTurnEnded ? "Yes": "No");
        return properties;
    }

    /***
     * If this unit is removed and is selected, the selection
     * will shift to unit which is returned by this method
     */
    public Unit shiftSelectionOnRemoval(){
        return null;
    }
}
