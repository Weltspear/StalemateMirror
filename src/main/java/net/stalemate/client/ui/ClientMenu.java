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

package net.stalemate.client.ui;

import net.stalemate.client.AssetLoader;
import net.stalemate.client.ui.swing.ButtonHover;
import net.stalemate.client.ui.swing.HintEntry;
import net.stalemate.client.ui.swing.StalemateStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.locks.ReentrantLock;

public class ClientMenu extends JPanel {

    private final JFrame frame;

    private final BufferedImage background;
    private final BufferedImage title;

    private final JTextField entry;
    private final ButtonHover connect;

    private boolean alr_added_ok_button = false;

    private final ActionListener oldconnect;

    public volatile int status = 0;

    private final static Color StalemateGreen = new Color(35, 115, 0);

    private String error = "";
    public void setError(String error){
        this.error = error;
    }

    public String getTxt(){
        if (entry.getText().equals("Enter IP")){
            return "";
        }
        return entry.getText();
    }

    public ClientMenu(JFrame f){
        this.frame = f;
        frame.add(this);
        frame.pack();
        this.setLayout(null);

        entry = new HintEntry("Enter IP");
        StalemateStyle.makeJTextField(entry);
        Font monogram = AssetLoader.getMonogram();
        entry.setFont(monogram.deriveFont(18f));
        entry.setBounds(new Rectangle(240, 40));
        entry.setLocation((832-14-entry.getWidth())/2, 276);
        this.add(entry);

        connect = new ButtonHover("Connect");
        StalemateStyle.makeButton(connect);
        connect.setLocation((832-14-connect.getWidth())/2, 320);
        connect.setFont(monogram.deriveFont(16f));

        oldconnect = a -> status = 1;
        connect.addActionListener(oldconnect);
        this.add(connect);

        background = AssetLoader.load("assets/background.png");
        title = AssetLoader.load("assets/stalemate.png");

    }

    public void update(){
        try {
            SwingUtilities.invokeAndWait(this::repaint);
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }

        if (status != 0 && !alr_added_ok_button){
            try {
                SwingUtilities.invokeAndWait(() -> {
                    this.remove(connect);
                    this.remove(entry);
                });
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        if (status == 4 && !alr_added_ok_button){
            // Repurposing of connect button
            try {
                SwingUtilities.invokeAndWait(() -> {
                    this.add(connect);
                    connect.setSelected(false);
                    connect.setText("OK");
                    connect.addActionListener(a -> status = -1
                    );
                    connect.removeActionListener(oldconnect);

                    alr_added_ok_button = true;
                });
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
            // add display of ok button here {status = -1;}
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        this.setBackground(StalemateGreen);
        g.drawImage(background, 0, 0, null);

        // title char size
        FontMetrics metrics = g.getFontMetrics(AssetLoader.getMonogram().deriveFont((float) (25)).deriveFont(Font.BOLD));
        int width = metrics.stringWidth("A");
        int y;

        if (title!=null)
            g.drawImage(title.getScaledInstance(364, 64, Image.SCALE_FAST), 234, 230-60, null);

        if (status == 3) {
            // Render connecting
            g.setFont(AssetLoader.getMonogram().deriveFont(25f));
            g.setColor(Color.WHITE);
            int half = ((832 + 14) - width * "Connecting...".length()) / 2;
            g.drawString("Connecting...", half, 296);
        }
        if (status == 4) {
            // Render error
            g.setFont(AssetLoader.getMonogram().deriveFont(25f));
            g.setColor(Color.WHITE);
            int half = ((832 + 14) - width * error.length()) / 2;
            g.drawString(error, half, 296);
        }

        this.paintComponents(g);
    }

    public void setStatus(int status){
        this.status = status;
    }

    public void clFrame(){
        try {
            SwingUtilities.invokeAndWait(() -> frame.remove(this));
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
