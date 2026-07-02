package net.lunix.svruptime.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.lunix.svruptime.SvrUptimeCommon;

public class SvrUptimeFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SvrUptimeCommon.init(FabricLoader.getInstance().getConfigDir());

        ServerTickEvents.END_SERVER_TICK.register(SvrUptimeCommon::onServerTick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            SvrUptimeCommon.registerCommands(dispatcher));
    }
}
