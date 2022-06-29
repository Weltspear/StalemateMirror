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

import net.stalemate.menu.ui.Menu;
import net.stalemate.menu.ui.STButton;
import net.stalemate.networking.client.AssetLoader;
import net.stalemate.networking.client.Client;
import net.stalemate.networking.server.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class MainMenu extends JPanel implements Menu {

    private JFrame frame;
    private volatile Font basis33;
    private ReentrantLock lock = new ReentrantLock();
    private MouseAdapter mouseAdapter;

    private ArrayList<STButton> buttons = new ArrayList<>();

    public int status = 0;
    private int buttonToBeHighlighted = -1;

    private final static Color StalemateGreen = new Color(35, 115, 0);

    private BufferedImage background;
    private BufferedImage title;

    public class MainMenuMouse extends MouseAdapter{
        private final int width_f;
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
            buttonToBeHighlighted = commonMouseMovedProcedure(buttons, width_f, e, y);

            lock.unlock();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            lock.lock();
            int y = 276; // 276
            buttonToBeHighlighted = commonMouseClickProcedure(buttonToBeHighlighted, buttons, width_f, e, y);
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
            public void action() {
                status = 1;
            }
        });

        buttons.add(new STButton() {
            @Override
            public String text() {
                return "Start a server";
            }

            @Override
            public void action() {
                status = 2;
            }
        });

        buttons.add(new STButton() {
            @Override
            public String text() {
                return "Exit";
            }

            @Override
            public void action() {
                System.exit(0);
            }
        });

        try {
            basis33 = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getClassLoader().getResource("basis33/basis33.ttf")).openStream());

        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        mouseAdapter = new MainMenuMouse();

        frame.addMouseMotionListener(mouseAdapter);
        frame.addMouseListener(mouseAdapter);

        background = AssetLoader.load("assets/background.png");
        title = AssetLoader.load("assets/stalemate.png");

    }

    public void update(){
        lock.lock();
        this.repaint();

        if (status == 1){
            // frame.setVisible(false);
            this.frame.remove(this);
            this.frame.removeMouseListener(mouseAdapter);
            this.frame.removeMouseMotionListener(mouseAdapter);
            Client client = new Client(this.frame);
            client.start_client();
            this.frame.add(this);
            this.frame.addMouseListener(mouseAdapter);
            this.frame.addMouseMotionListener(mouseAdapter);
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
        g.drawImage(background, 0, 0, null);

        while (basis33 == null){
            Thread.onSpinWait();
        }
        g.setFont(basis33.deriveFont(16f));
        g.setColor(Color.WHITE);
        g.drawString("Version v0.3a-dev", 3, 570);
        g.drawString("Made by Weltspear and Dzolab", 587, 540);
        g.drawString("Licensed under terms of GNU AGPLv3", 587, 550);
        g.drawString("See NOTICE.md and LICENSE.md for m", 587, 560);
        g.drawString("ore information", 587, 570);

        // Render button text
        int y = 276;
        renderTitleAndButtons(g, y, basis33, buttons, buttonToBeHighlighted, title);
    }

    public static void renderTitleAndButtons(Graphics g, int y, Font basis33, ArrayList<STButton> buttons, int buttonToBeHighlighted, BufferedImage title){
        FontMetrics metrics = g.getFontMetrics(basis33.deriveFont((float)(25)).deriveFont(Font.BOLD));
        int width = metrics.stringWidth("A");

        int button_num = 0;
        g.setFont(basis33.deriveFont((float) (25)));
        g.setColor(Color.white);
        for (STButton b : buttons) {
            int half = ((832 + 14) - width * b.text().length()) / 2;
            if (button_num == buttonToBeHighlighted)
                g.setColor(Color.GRAY);
            g.drawString(b.text(), half, y);
            g.setColor(Color.WHITE);
            y += 20;
            button_num++;
        }

        // Render title
        g.setFont(basis33.deriveFont((float) (30)));
        g.setColor(Color.white);

        // title char size
        metrics = g.getFontMetrics(basis33.deriveFont((float) (30)).deriveFont(Font.BOLD));
        width = metrics.stringWidth("A");

        y = 230;
        int half = ((832 + 14) - width * "Stalemate".length()) / 2;
        // g.drawString("Stalemate", half, y);
        if (title!=null)
        g.drawImage(title.getScaledInstance(364, 64, Image.SCALE_FAST), 234, 230-60, null);
    }

    public static int commonMouseMovedProcedure(ArrayList<STButton> buttons, int width_f, MouseEvent e, int y){
        int buttonToBeHighlighted = -1;
        int button_num = 0;

        for (STButton b: buttons){
            int half = ((832+14) - width_f*b.text().length())/2;
            int half_end = half+(b.text().length()*width_f);

            if (e.getX() >= half && e.getX() <= half_end &&
                    e.getY() >= 20+y && e.getY() <= 20+y+10){
                buttonToBeHighlighted = button_num;
            }

            y += 20;
            button_num++;
        }

        return buttonToBeHighlighted;
    }

    public static int commonMouseClickProcedure(int buttonToBeHighlighted, ArrayList<STButton> buttons, int width_f, MouseEvent e, int y){
        int button_num = 0;
        for (STButton b: buttons){
            int half = ((832+14) - width_f*b.text().length())/2;
            int half_end = half+(b.text().length()*width_f);

            if (e.getX() >= half && e.getX() <= half_end &&
                    e.getY() >= 20+y && e.getY() <= 20+y+10){
                buttonToBeHighlighted = button_num;
                b.action();
            }

            y += 20;
            button_num++;
        }
        return buttonToBeHighlighted;
    }
}
