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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class HintEntry extends JTextField implements FocusListener {
    private boolean hinted = false;

    public HintEntry(String hint){
        super();
        setText(hint);
        addFocusListener(this);
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (!hinted){
            setText("");
            hinted = true;
        }
    }

    @Override
    public void focusLost(FocusEvent e) {

    }
}
