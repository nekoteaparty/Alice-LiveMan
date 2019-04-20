/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package site.alice.liveman.customlayout.impl;

import site.alice.liveman.customlayout.BlurLayout;

import java.awt.*;

public class RectangleBlurLayout extends BlurLayout {
    @Override
    public void paintLayout(Graphics2D g) throws Exception {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, width, height);
    }
}
