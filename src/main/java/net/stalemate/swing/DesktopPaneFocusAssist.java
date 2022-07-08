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

package net.stalemate.swing;

import javax.swing.*;

public class DesktopPaneFocusAssist extends JDesktopPane{
    private final JComponent componentToRegainFocus;
    private boolean hasDisabled = false;

    public interface Disable{
        void disableWhole();
        void enableWhole();
    }

    public DesktopPaneFocusAssist(JComponent componentToRegainFocus) {
        super();
        this.setOpaque(true);
        this.componentToRegainFocus = componentToRegainFocus;
    }

    public void updateAssist(){
        if (this.getAllFrames().length == 0) {
            componentToRegainFocus.requestFocus();
            ((Disable) componentToRegainFocus).enableWhole();
            hasDisabled = false;
        }
        else if (!hasDisabled) {
            requestFocus();
            ((Disable) componentToRegainFocus).disableWhole();
            hasDisabled = true;
        }
    }
}
