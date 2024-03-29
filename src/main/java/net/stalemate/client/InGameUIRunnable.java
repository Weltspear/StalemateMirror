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

package net.stalemate.client;

import net.stalemate.client.ui.InGameUI;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class InGameUIRunnable implements Runnable {
    private boolean isTerminated = false;
    private final InGameUI g;
    private final ClientGame cgame;
    public ReentrantLock lock = new ReentrantLock();

    public InGameUIRunnable(InGameUI g, ClientGame cgame){
        this.g = g;
        this.cgame = cgame;
    }

    public void terminate(){
        isTerminated = true;
    }

    @Override
    public void run() {
        while (!isTerminated){
            lock.lock();
            long t1 = System.currentTimeMillis();
            try {
		        g.inGameUIUpdate();

                g.unsafeLock.lock();
                Object[] ef = cgame.buildView(g.cam_x, g.cam_y, g.scale, (g.tr_width-832)/64, (g.tr_height-576)/64);

                Image minimap = cgame.drawMinimap(g.cam_x, g.cam_y);

                g.getClDataManager().updateData(cgame.getChat(),
                        (ClientGame.ClientEntity[][])ef[0], (boolean[][])ef[1], (boolean[][])ef[2], cgame.getSelectedUnit(), cgame.getMp(),
                        cgame.isIsItYourTurn(), cgame.getClMapLoader(), minimap, cgame.getTeamDoingTurnColor(), cgame.getTeamDoingTurnNick(),
                        cgame.getGamemodeProperties());

                g.unsafeLock.unlock();


                try {
                    SwingUtilities.invokeAndWait(g::repaint);
                } catch (InterruptedException | InvocationTargetException e) {
                    e.printStackTrace();
                }


                long t2 = System.currentTimeMillis() - t1;
                if (8 - t2 > 0) {
                    try {
                        Thread.sleep(8 - t2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            finally {
                lock.unlock();
            }
        }
    }
}
