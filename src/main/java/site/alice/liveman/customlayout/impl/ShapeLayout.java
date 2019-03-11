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

import site.alice.liveman.customlayout.CustomLayout;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ShapeLayout extends CustomLayout {

    private float  opacity;
    private float  radiusPercentW;
    private float  radiusPercentH;
    private String hexColor;
    private String rgba;

    public float getRadiusPercentW() {
        return radiusPercentW;
    }

    public void setRadiusPercentW(float radiusPercentW) {
        this.radiusPercentW = radiusPercentW;
    }

    public float getRadiusPercentH() {
        return radiusPercentH;
    }

    public void setRadiusPercentH(float radiusPercentH) {
        this.radiusPercentH = radiusPercentH;
    }

    public String getHexColor() {
        return hexColor;
    }

    public void setHexColor(String hexColor) {
        this.hexColor = hexColor;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public String getRgba() {
        return rgba;
    }

    public void setRgba(String rgba) {
        this.rgba = rgba;
    }

    @Override
    public void paintLayout(Graphics2D g) throws Exception {
        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(x, y, width, height, width * (radiusPercentW * 0.01f), height * (radiusPercentH * 0.01f));
        g.setColor(Color.decode(hexColor));
        Composite oldComp = g.getComposite();
        g.setPaintMode();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g.fill(roundedRectangle);
        g.setComposite(oldComp);
    }
}
