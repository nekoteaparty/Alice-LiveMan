/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package site.alice.liveman.event;

import java.util.EventObject;
import java.util.Map;

public class DemandEvent extends EventObject {

    private String               currentDemandItem;
    private Map<String, Integer> demandCountMap;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public DemandEvent(Object source) {
        super(source);
    }

    public String getCurrentDemandItem() {
        return currentDemandItem;
    }

    public void setCurrentDemandItem(String currentDemandItem) {
        this.currentDemandItem = currentDemandItem;
    }

    public Map<String, Integer> getDemandCountMap() {
        return demandCountMap;
    }

    public void setDemandCountMap(Map<String, Integer> demandCountMap) {
        this.demandCountMap = demandCountMap;
    }
}
