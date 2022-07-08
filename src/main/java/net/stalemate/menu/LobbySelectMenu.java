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

package net.stalemate.menu;

import net.libutils.error.ErrorResult;
import net.libutils.error.Expect;
import net.stalemate.networking.client.AssetLoader;
import net.stalemate.swing.ButtonHover;
import net.stalemate.swing.LobbyJList;
import net.stalemate.swing.StalemateStyle;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class LobbySelectMenu extends JPanel {
    private final JFrame frame;
    private final LobbyJList lbJList;
    private final BufferedImage background_img;
    /***
     * 1 -> connect
     * 2 -> refresh
     * 3 -> disconnect
     */
    private volatile int status = 0;

    private final JLabel label;
    private final BufferedImage title;

    private final ReentrantLock lock = new ReentrantLock();

    public LobbySelectMenu(JFrame frame, String srv_description){

        this.frame = frame;
        this.setSize(frame.getWidth(), frame.getHeight());
        this.setLayout(null);

        Font basis33 = AssetLoader.getBasis33();
        title = AssetLoader.load("assets/stalemate.png");
        background_img = AssetLoader.load("assets/background.png");

        assert basis33 != null;
        assert title!=null;
        assert background_img!=null;

        lbJList = new LobbyJList(basis33);
        lbJList.setLocation((this.getWidth()-lbJList.getWidth()-50)/2, (this.getHeight()-lbJList.getHeight())/2);
        lbJList.setVisible(true);
        add(lbJList);

        ButtonHover connect = new ButtonHover("Connect");
        StalemateStyle.makeButton(connect);
        connect.addActionListener(e -> status = 1);
        connect.setLocation(new Point(lbJList.getX()+375+25, lbJList.getY()));
        connect.setVisible(true);
        connect.setFont(basis33.deriveFont(16f));
        connect.setBorder(new BevelBorder(BevelBorder.LOWERED));

        ButtonHover refresh = new ButtonHover("Refresh");
        refresh.addActionListener(e -> status = 2);
        StalemateStyle.makeButton(refresh);
        refresh.setLocation(new Point(lbJList.getX()+375+25, lbJList.getY()+25));
        refresh.setVisible(true);
        refresh.setFont(basis33.deriveFont(16f));
        refresh.setBorder(new BevelBorder(BevelBorder.LOWERED));

        ButtonHover disconnect = new ButtonHover("Disconnect");
        disconnect.addActionListener(e -> status = 3);
        StalemateStyle.makeButton(disconnect);
        disconnect.setLocation(new Point(lbJList.getX()+375+25, lbJList.getY()+50));
        disconnect.setVisible(true);
        disconnect.setFont(basis33.deriveFont(16f));
        disconnect.setBorder(new BevelBorder(BevelBorder.LOWERED));

        JTextArea serverDesc = new JTextArea();
        serverDesc.setEditable(false);
        StalemateStyle.makeComponent(serverDesc);
        serverDesc.setText(srv_description);
        serverDesc.setLocation(new Point(lbJList.getX()+375+25, lbJList.getY()+75));
        serverDesc.setBorder(new BevelBorder(BevelBorder.LOWERED));
        serverDesc.setFont(basis33.deriveFont(15f));
        serverDesc.setSize(150, 225);

        this.add(connect);
        this.add(refresh);
        this.add(disconnect);
        this.add(serverDesc);
        // this.add(list);
        this.setVisible(true);
        frame.add(this);

        label = new JLabel();
        StalemateStyle.makeComponent(label);
        label.setForeground(Color.RED);
        label.setLocation(lbJList.getX(), lbJList.getY()+300);
        label.setVisible(true);
        label.setSize(400, 40);
        label.setFont(basis33.deriveFont(14f));
        this.add(label);

        // this.setBackground(new Color(51, 39, 31));
    }

    @Override
    public void paintComponent(Graphics g){
        lock.lock();
        super.paintComponent(g);
        lock.unlock();
    }

    public int getIndex(){
        return lbJList.getSelectedIndex();
    }

    public Expect<?, ErrorResult> setLobbies(ArrayList<String> a){
        lock.lock();
        try {
            return lbJList.setLobbies(a);
        }
        finally {
            lock.unlock();
        }
    }

    public int getStatus(){
        return status;
    }

    public void clFrame(){
        frame.remove(this);
    }

    public void setStatus(int i){
        status = i;
    }

    public void setText(String s){
        label.setText(s);
    }

    @Override
    public void paint(Graphics g){
        lock.lock();
        g.drawImage(background_img, 0, 0, null);
        paintComponents(g);
        if (title!=null)
            g.drawImage(title.getScaledInstance(364, 64, Image.SCALE_FAST), 234, 65, null);
        lock.unlock();
    }

}
