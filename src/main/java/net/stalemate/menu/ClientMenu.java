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
import net.stalemate.menu.ui.STEntry;
import net.stalemate.networking.client.config.KeyboardBindMapper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import static net.stalemate.menu.MainMenu.*;

public class ClientMenu extends JPanel implements Menu {

    private JFrame frame;
    private volatile Font basis33;
    private ReentrantLock lock = new ReentrantLock();
    private MouseAdapter mouseAdapter;

    private BufferedImage background;

    private ArrayList<STButton> buttons = new ArrayList<>();

    private STEntry entry = new STEntry("Enter ip: ");

    public int status = 0;
    private int buttonToBeHighlighted = -1;

    private final static Color StalemateGreen = new Color(35, 115, 0);

    private final KeyListener keyListener;

    private String error = "";
    public void setError(String error){
        this.error = error;
    }

    public class EntryKeyboardListener implements KeyListener{

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            lock.lock();
            if (status == 0) {
                if (" qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM<>=-()[]{}\"';:.,1234567890@#$%^&*/\\?".contains(String.valueOf(e.getKeyChar()))) {
                    entry.write(String.valueOf(e.getKeyChar()));
                } else if (e.getKeyCode() == KeyboardBindMapper.escape) {
                    status = 2;
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    status = 1;
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    entry.backspace();
                }
            }
            lock.unlock();
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }

    public class ClientMenuMouse extends MouseAdapter{
        /***
         * Char width in basis33
         */
        private final int width_f;
        private int height_f;

        public ClientMenuMouse(){
            // get char size
            FontMetrics metrics = getGraphics().getFontMetrics(basis33.deriveFont((float)(25)).deriveFont(Font.BOLD));
            width_f = metrics.stringWidth("A");
            height_f = 20;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            lock.lock();

            if (status == 0 || status == 4) {
                // select button to be highlighted
                int y = 276 + 20;
                commonMouseMovedProcedure(buttons, width_f, e, y);
            }

            lock.unlock();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            lock.lock();
            int y = 276 + 20; // 276
            buttonToBeHighlighted = commonMouseClickProcedure(buttonToBeHighlighted, buttons, width_f, e, y);

            lock.unlock();
        }
    }

    public String getTxt(){
        lock.lock();
        try {
            return entry.getTxt();
        } finally {
            lock.unlock();
        }
    }

    public ClientMenu(JFrame f){
        this.frame = f;

        try {
            basis33 = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getClassLoader().getResource("basis33/basis33.ttf")).openStream());

        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        buttons.add(new STButton() {
            @Override
            public String text() {
                return "OK";
            }

            @Override
            public void action() {
                status = 1;
            }
        });

        frame.add(this);
        frame.pack();
        mouseAdapter = new ClientMenuMouse();
        keyListener = new EntryKeyboardListener();

        frame.addMouseMotionListener(mouseAdapter);
        frame.addMouseListener(mouseAdapter);
        frame.addKeyListener(keyListener);

        try {
            background = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource("assets/background.png")));
        }
        catch (IOException e) {
            background = null;
        }
    }

    public void update(){
        lock.lock();
        if (status == 4){
            buttons = new ArrayList<>();
            buttons.add(new STButton() {
                @Override
                public String text() {
                    return "OK";
                }

                @Override
                public void action() {
                    status = -1;
                }
            });
        }
        this.repaint();
        lock.unlock();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        this.setBackground(StalemateGreen);
        g.drawImage(background, 0, 0, null);

        lock.lock();
        if (status == 0) {
            while (basis33 == null) {
                Thread.onSpinWait();
            }

            // get char size
            FontMetrics metrics = g.getFontMetrics(basis33.deriveFont((float) (25)).deriveFont(Font.BOLD));
            int width = metrics.stringWidth("A");

            // Render entry
            g.setFont(basis33.deriveFont((float) (25)));
            g.setColor(Color.WHITE);
            int half_entry = ((832 + 14) - width * entry.getFullTxt().length()) / 2;
            g.drawString(entry.getFullTxt(), half_entry, 276);

            // Render button text
            int y = 276 + 20;
            renderTitleAndButtons(g, y, basis33, buttons, buttonToBeHighlighted);

            lock.unlock();
            return;
        }

        FontMetrics metrics;
        int width;
        int y;
        // Render title
        g.setFont(basis33.deriveFont((float) (30)));
        g.setColor(Color.white);

        // title char size
        metrics = g.getFontMetrics(basis33.deriveFont((float) (30)).deriveFont(Font.BOLD));
        width = metrics.stringWidth("A");

        y = 230;
        int half = ((832 + 14) - width * "Stalemate".length()) / 2;
        g.drawString("Stalemate", half, y);

        if (status == 3) {
            // Render connecting
            half = ((832 + 14) - width * "Connecting...".length()) / 2;
            g.drawString("Connecting...", half, 296);
        }
        if (status == 4) {
            // Render error
            half = ((832 + 14) - width * error.length()) / 2;
            g.drawString(error, half, 276);

            // Render button text
            y = 276 + 20;
            int button_num = 0;
            g.setFont(basis33.deriveFont((float) (25)));
            g.setColor(Color.white);
            for (STButton b : buttons) {
                half = ((832 + 14) - width * b.text().length()) / 2;
                if (button_num == buttonToBeHighlighted)
                    g.setColor(Color.GRAY);
                g.drawString(b.text(), half, y);
                g.setColor(Color.WHITE);
                y += 20;
                button_num++;
            }
        }
        lock.unlock();
    }

    public void setStatus(int status){
        lock.lock();
        this.status = status;
        lock.unlock();
    }

    public void clFrame(){
        lock.lock();
        frame.remove(this);
        frame.removeMouseListener(mouseAdapter);
        frame.removeMouseMotionListener(mouseAdapter);
        frame.removeKeyListener(keyListener);
        lock.unlock();
    }
}
