package net.lunix.svruptime.fabric;

import net.fabricmc.api.ModInitializer;
import net.lunix.svruptime.SvrUptimeCommon;

public class SvrUptimeFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SvrUptimeCommon.init();
    }
}
