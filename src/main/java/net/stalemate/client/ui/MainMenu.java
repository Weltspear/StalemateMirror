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

import net.libutils.error.Expect;
import net.stalemate.StVersion;
import net.stalemate.client.AssetLoader;
import net.stalemate.client.Client;
import net.stalemate.client.ui.swing.*;
import net.stalemate.server.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.locks.ReentrantLock;

public class MainMenu extends JPanel implements DesktopPaneFocusAssist.Disable {
    private final JFrame frame;
    private final ButtonHover play;
    private final ButtonHover start_srv;
    private final ButtonHover options;
    private final ButtonHover exit;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean areButtonsDisabled = false;

    public int status = 0;

    private final static Color StalemateGreen = new Color(35, 115, 0);

    private final BufferedImage background;
    private final BufferedImage title;

    private final DesktopPaneFocusAssist p;

    public MainMenu(){
        frame = new JFrame("Stalemate");
        frame.setMinimumSize(new Dimension(832+14,576+32+6));
        frame.setSize(new Dimension(832+32,576+32+6));
        frame.setResizable(false);
        frame.add(this);
        frame.setVisible(true);
        frame.setIconImage(AssetLoader.load("assets/ui/selectors/ui_attack.png"));
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLayout(null);

        // Disable F10 bind due to weird stuff happening when openning escape menu
        Action emptyAction = new AbstractAction(){public void actionPerformed(ActionEvent e) {}};
        KeyStroke f10 = KeyStroke.getKeyStroke("F10");
        frame.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(f10, "F10");
        frame.getRootPane().getActionMap().put("F10", emptyAction);

        Font monogram = AssetLoader.getMonogram();

        background = AssetLoader.load("assets/background.png");
        title = AssetLoader.load("assets/stalemate.png");

        play = new ButtonHover("Play");
        StalemateStyle.makeButton(play);
        start_srv = new ButtonHover("Start a server");
        StalemateStyle.makeButton(start_srv);
        options = new ButtonHover("Options");
        StalemateStyle.makeButton(options);
        exit = new ButtonHover("Exit");
        StalemateStyle.makeButton(exit);

        play.setLocation(new Point((832+14- play.getWidth())/2, 250));
        play.setFont(monogram.deriveFont(18f));
        play.addActionListener((e) -> status = 1);
        start_srv.setLocation(new Point((832+14- start_srv.getWidth())/2, 280));
        start_srv.setFont(monogram.deriveFont(18f));
        start_srv.addActionListener((e) -> status = 2);
        options.setLocation(new Point((832+14- options.getWidth())/2, 310));
        options.addActionListener((e) -> {
            try {
                Runtime.getRuntime().exec("notepad grass32");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        options.setFont(monogram.deriveFont(18f));
        exit.setLocation(new Point((832+14- exit.getWidth())/2, 340));
        exit.setFont(monogram.deriveFont(18f));
        exit.addActionListener((e) -> System.exit(0));

        p = new DesktopPaneFocusAssist(this);
        p.setSize(new Dimension(832+14,576));
        p.setBackground(new Color(0x00FFFFFF, true));
        p.setPreferredSize(new Dimension(832+14,576));
        p.setBounds(0,0, 832+14,576);

        this.add(play);
        this.add(start_srv);
        this.add(options);
        this.add(exit);
        this.add(p);
    }

    public void update(){
        lock.lock();
        try {
            SwingUtilities.invokeAndWait(this::repaint);
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }

        if (status == 1){
            // frame.setVisible(false);
            try {
                SwingUtilities.invokeAndWait(() -> this.frame.remove(this));
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }

            Client client = new Client(this.frame);
            Expect<?, ?> expect = client.start_client();
            if (expect.isNone()){
                StMessageBox messageBox = new StMessageBox("Error", makeNewLinesError(expect.getResult().message()));
                messageBox.setLocation(((p.getWidth())-messageBox.getWidth())/2, ((p.getHeight())-messageBox.getHeight())/2);
                messageBox.setVisible(true);
                p.add(messageBox);
            }


            try {
                SwingUtilities.invokeAndWait(() -> {
                    this.frame.add(this);
                    this.frame.revalidate();
                    this.requestFocus();
                });
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
            status = 0;
        }
        else if (status == 2){
            try {
                SwingUtilities.invokeAndWait(() -> frame.setVisible(false));
                Server server = new Server();
                server.start_server();
                SwingUtilities.invokeAndWait(() -> frame.setVisible(true));
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        p.updateAssist();
        lock.unlock();
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

        g.setFont(AssetLoader.getMonogram().deriveFont(18f));
        g.setColor(Color.WHITE);
        g.drawString("Version " + StVersion.version, 3, 570);
        g.drawString("Made by Weltspear and SP7", 587, 540);
        g.drawString("Licensed under terms of GNU AGPLv3", 587, 550);
        g.drawString("See NOTICE.md and LICENSE.md for m", 587, 560);
        g.drawString("ore information", 587, 570);

        // Render button text
        int y = 276;
        if (title!=null)
            g.drawImage(title.getScaledInstance(364, 64, Image.SCALE_FAST), 234, 230-60, null);

        // even more evil things to get jdesktoppane to work
        if (p != null)
            p.setVisible(false);
        // this is connected to UI glitch with StMessageBox
        if (play != null && start_srv != null && options != null && exit != null){
            if (areButtonsDisabled){
                play.setVisible(true);
                start_srv.setVisible(true);
                exit.setVisible(true);
                options.setVisible(true);
            }
        }
        paintComponents(g);
        if (play != null && start_srv != null && options != null && exit != null){
            if (areButtonsDisabled){
                play.setVisible(false);
                start_srv.setVisible(false);
                exit.setVisible(false);
                options.setVisible(false);
            }
        }
        if (p != null)
            p.setVisible(true);

        if (p != null){
            // Evil things to get JDesktopPanel to work
            BufferedImage clone = new BufferedImage(832+32,576+32+6, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = clone.createGraphics();
            p.printAll(graphics);
            graphics.dispose();
            g.drawImage(clone, 0, 0, null);
        }
    }

    @Override
    public void disableWhole() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                play.setEnabled(false);
                start_srv.setEnabled(false);
                exit.setEnabled(false);
                options.setEnabled(false);

                play.setVisible(false);
                start_srv.setVisible(false);
                exit.setVisible(false);
                options.setVisible(false);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
        areButtonsDisabled = true;
    }

    public String makeNewLinesError(String in){
        StringBuilder stringBuilder = new StringBuilder();
        int count = 0;
        for (char c: in.toCharArray()){
            stringBuilder.append(c);
            count++;
            if (count == 33){
                stringBuilder.append('\n');
                count = 0;
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public void enableWhole() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                play.setEnabled(true);
                start_srv.setEnabled(true);
                exit.setEnabled(true);
                options.setEnabled(true);

                play.setVisible(true);
                start_srv.setVisible(true);
                exit.setVisible(true);
                options.setVisible(true);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
        areButtonsDisabled = false;
    }
}
