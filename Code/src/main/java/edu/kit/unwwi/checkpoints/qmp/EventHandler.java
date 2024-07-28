package edu.kit.unwwi.checkpoints.qmp;

import org.jetbrains.annotations.NotNull;

public interface EventHandler {

    void handleEvent(@NotNull Event event);

    @NotNull
    String eventName();
}
