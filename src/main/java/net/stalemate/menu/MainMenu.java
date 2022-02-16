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

import net.stalemate.menu.ui.STButton;
import net.stalemate.networking.client.Client;
import net.stalemate.networking.server.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class MainMenu extends JPanel {

    private JFrame frame;
    private volatile Font basis33;
    private ReentrantLock lock = new ReentrantLock();

    private ArrayList<STButton> buttons = new ArrayList<>();

    public int status = 0;
    private int buttonToBeHighlighted = -1;

    private final static Color StalemateGreen = new Color(35, 115, 0);

    public class MainMenuMouse extends MouseAdapter{
        private int width_f;
        private int height_f;

        public MainMenuMouse(){
            // get char size
            FontMetrics metrics = getGraphics().getFontMetrics(basis33.deriveFont((float)(25)).deriveFont(Font.BOLD));
            width_f = metrics.stringWidth("A");
            height_f = 20;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            lock.lock();

            // select button to be highlighted
            int y = 276; // 276
            int button_num = 0;
            boolean isHighlighting = false;
            for (STButton b: buttons){
                int half = ((832+14) - width_f*b.text().length())/2;
                int half_end = half+(b.text().length()*width_f);

                if (e.getX() >= half && e.getX() <= half_end &&
                    e.getY() >= 20+y && e.getY() <= 20+y+10){
                    buttonToBeHighlighted = button_num;
                    isHighlighting = true;
                }

                y += 20;
                button_num++;
            }

            if (!isHighlighting){
                buttonToBeHighlighted = -1;
            }

            lock.unlock();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            lock.lock();
            int y = 276; // 276
            int button_num = 0;
            for (STButton b: buttons){
                int half = ((832+14) - width_f*b.text().length())/2;
                int half_end = half+(b.text().length()*width_f);

                if (e.getX() >= half && e.getX() <= half_end &&
                        e.getY() >= 20+y && e.getY() <= 20+y+10){
                    buttonToBeHighlighted = button_num;
                    b.action(MainMenu.this);
                }

                y += 20;
                button_num++;
            }

            lock.unlock();
        }
    }

    public MainMenu(){
        frame = new JFrame("Stalemate");
        frame.setMinimumSize(new Dimension(832+14,576+32+6));
        frame.setSize(new Dimension(832+32,576+32+6));
        frame.setResizable(false);
        frame.add(this);
        frame.setVisible(true);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        buttons.add(new STButton() {
            @Override
            public String text() {
                return "Play";
            }

            @Override
            public void action(MainMenu m) {
                status = 1;
            }
        });

        buttons.add(new STButton() {
            @Override
            public String text() {
                return "Start a server";
            }

            @Override
            public void action(MainMenu m) {
                status = 2;
            }
        });

        buttons.add(new STButton() {
            @Override
            public String text() {
                return "Exit";
            }

            @Override
            public void action(MainMenu m) {
                System.exit(0);
            }
        });

        try {
            basis33 = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getClassLoader().getResource("basis33/basis33.ttf")).openStream());

        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        frame.addMouseMotionListener(new MainMenuMouse());
        frame.addMouseListener(new MainMenuMouse());
    }

    public void update(){
        lock.lock();
        this.repaint();

        if (status == 1){
            frame.setVisible(false);
            Client client = new Client();
            client.start_client();
            status = 0;
            frame.setVisible(true);
        }
        else if (status == 2){
            frame.setVisible(false);
            Server server = new Server();
            server.start_server();
            frame.setVisible(true);
        }
        lock.unlock();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        this.setBackground(StalemateGreen);

        while (basis33 == null){
            Thread.onSpinWait();
        }

        // get char size
        FontMetrics metrics = g.getFontMetrics(basis33.deriveFont((float)(25)).deriveFont(Font.BOLD));
        int width = metrics.stringWidth("A");

        // Render button text
        int y = 276;
        int button_num = 0;
        g.setFont(basis33.deriveFont((float)(25)));
        g.setColor(Color.white);
        for (STButton b: buttons){
            int half = ((832+14) - width*b.text().length())/2;
            if (button_num == buttonToBeHighlighted)
                g.setColor(Color.GRAY);
            g.drawString(b.text(), half, y);
            g.setColor(Color.WHITE);
            y += 20;
            button_num++;
        }

        // Render title
        g.setFont(basis33.deriveFont((float)(30)));
        g.setColor(Color.white);

        // title char size
        metrics = g.getFontMetrics(basis33.deriveFont((float)(30)).deriveFont(Font.BOLD));
        width = metrics.stringWidth("A");

        y = 230;
        int half = ((832+14) - width*"Stalemate".length())/2;
        g.drawString("Stalemate", half, y);
    }
}
