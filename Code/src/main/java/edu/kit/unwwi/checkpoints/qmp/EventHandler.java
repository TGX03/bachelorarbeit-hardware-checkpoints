package edu.kit.unwwi.checkpoints.qmp;

public interface EventHandler {

    void handleEvent(Event event);

    String eventName();
}
