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

package site.alice.liveman.service.external;

import java.awt.*;

public class TextLocation {
    private Rectangle rectangle;
    private String    text;
    private Double    score;
    private Long      lastHitTime;

    public Rectangle getRectangle() {
        return rectangle;
    }

    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Long getLastHitTime() {
        return lastHitTime;
    }

    public void setLastHitTime(Long lastHitTime) {
        this.lastHitTime = lastHitTime;
    }

    @Override
    public String toString() {
        return "TextLocation{" +
                "rectangle=" + rectangle +
                ", text='" + text + '\'' +
                ", score=" + score +
                '}';
    }
}
