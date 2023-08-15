package com.twistedmcstudios.survival.commands.tpa.managers;

import java.util.UUID;

public class TeleportRequest {
    public UUID requestee;
    public boolean isReversed;

    public TeleportRequest (UUID sender, boolean direction) {
        requestee = sender;
        isReversed = direction;
    }
}
