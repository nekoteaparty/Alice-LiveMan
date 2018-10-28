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
