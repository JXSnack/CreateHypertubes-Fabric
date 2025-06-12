package com.pedrorok.hypertube;

import com.pedrorok.hypertube.events.ClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

// FABRIC ONLY
// For client initialization and co
public class HypertubeModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register((client) -> ClientEvents.onTickPre());
        ClientTickEvents.END_CLIENT_TICK.register((client) -> ClientEvents.onTickPre());

        WorldRenderEvents.END.register(ClientEvents::onRenderWorld);
    }
}
