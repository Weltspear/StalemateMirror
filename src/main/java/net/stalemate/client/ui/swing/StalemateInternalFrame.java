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

package net.stalemate.client.ui.swing;

import net.stalemate.client.AssetLoader;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import javax.swing.plaf.metal.MetalInternalFrameUI;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

public class StalemateInternalFrame extends JInternalFrame {
    public StalemateInternalFrame(String title){
        super(title, false, true, false, false);
        // evil things to customize titlebar
        // If this doesn't work for some reason put it into updateUI
        setUI(new MetalInternalFrameUI(this) {
            @Override protected JComponent createNorthPane(JInternalFrame w) {
                return new BasicInternalFrameTitlePane(w) {
                    @Override
                    protected void paintTitleBackground(Graphics g) {
                        g.setColor(new Color(131, 71, 37));
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }

                    @Override
                    protected void installDefaults() {

                        super.installDefaults();
                        Font monogram = AssetLoader.getMonogram().deriveFont(15f);
                        setFont(monogram);
                        selectedTextColor = new Color(198, 130, 77);
                        notSelectedTextColor = Color.BLACK;
                    }

                    @Override public void createButtons() {
                        super.createButtons();
                        Arrays.asList(closeButton, maxButton, iconButton).forEach(b -> {
                            b.setContentAreaFilled(false);
                            b.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                        });
                        closeButton.setIcon(new ImageIcon(Objects.requireNonNull(StalemateInternalFrame.class.getClassLoader().getResource("assets/internalframe/x.png"))));
                        closeButton.setToolTipText("<html><font face=\"monogramextended\" size=4>Close</font></html>");
                        windowMenu.setEnabled(false);
                    }
                };
            }
        });
        this.setFrameIcon(new ImageIcon(Objects.requireNonNull(StalemateInternalFrame.class.getClassLoader().getResource("assets/internalframe/title_img_2.png"))));
        this.setBorder(new LineBorder(new Color(31, 27, 21), 2));
        this.setBackground(new Color(60, 38, 22));
    }
}
