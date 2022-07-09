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

import javax.swing.*;
import java.awt.*;

public class ButtonHover extends JButton {

    private Color hoverBackgroundColor;
    private Color pressedBackgroundColor;

    private Color defForegroundColor;
    private Color hoverForegroundColor;
    private Color pressedForegroundColor;

    public ButtonHover() {
        this(null);
    }

    public ButtonHover(String text) {
        super(text);
        super.setContentAreaFilled(false);
    }

    @Override
    public void setForeground(Color c){
        super.setForeground(c);
        defForegroundColor = c;
    }

    public void setHoverForegroundColor(Color c){
        hoverForegroundColor = c;
    }

    public void setPressedForegroundColor(Color c){
        pressedForegroundColor = c;
    }

    public Color getHoverForegroundColor() {
        return hoverForegroundColor;
    }

    public Color getPressedForegroundColor() {
        return pressedForegroundColor;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (getModel().isPressed()) {
            g.setColor(pressedBackgroundColor);
            super.setForeground(pressedForegroundColor);
        } else if (getModel().isRollover()) {
            g.setColor(hoverBackgroundColor);
            super.setForeground(hoverForegroundColor);
        } else {
            g.setColor(getBackground());
            super.setForeground(defForegroundColor);
        }
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    @Override
    public void setContentAreaFilled(boolean b) {
    }

    public Color getHoverBackgroundColor() {
        return hoverBackgroundColor;
    }

    public void setHoverBackgroundColor(Color hoverBackgroundColor) {
        this.hoverBackgroundColor = hoverBackgroundColor;
    }

    public Color getPressedBackgroundColor() {
        return pressedBackgroundColor;
    }

    public void setPressedBackgroundColor(Color pressedBackgroundColor) {
        this.pressedBackgroundColor = pressedBackgroundColor;
    }
}
