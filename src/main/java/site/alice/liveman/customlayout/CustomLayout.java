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

package site.alice.liveman.customlayout;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import site.alice.liveman.customlayout.impl.*;
import site.alice.liveman.model.VideoInfo;

import javax.validation.constraints.NotNull;
import java.awt.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = DanmakuLayout.class, name = "DanmakuLayout"),
               @JsonSubTypes.Type(value = ImageLayout.class, name = "ImageLayout"),
               @JsonSubTypes.Type(value = ShapeLayout.class, name = "ShapeLayout"),
               @JsonSubTypes.Type(value = RectangleBlurLayout.class, name = "RectangleBlurLayout"),
               @JsonSubTypes.Type(value = ImageSegmentBlurLayout.class, name = "ImageSegmentBlurLayout")})
public abstract class CustomLayout implements Comparable<CustomLayout> {
    protected VideoInfo videoInfo;
    protected int       index;
    protected int       x;
    protected int       y;
    protected int       width;
    protected int       height;

    public abstract void paintLayout(Graphics2D g) throws Exception;

    public void setVideoInfo(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int compareTo(@NotNull CustomLayout o) {
        if (o.index == this.index) {
            return 1;
        }
        return this.index - o.index;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "index=" + index +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
