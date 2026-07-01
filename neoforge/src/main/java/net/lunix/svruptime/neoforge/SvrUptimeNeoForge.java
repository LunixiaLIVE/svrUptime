package net.lunix.svruptime.neoforge;

import net.lunix.svruptime.SvrUptimeCommon;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("svruptime")
public class SvrUptimeNeoForge {

    public SvrUptimeNeoForge() {
        SvrUptimeCommon.init(FMLPaths.CONFIGDIR.get());

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e ->
            SvrUptimeCommon.registerCommands(e.getDispatcher()));

        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e ->
            SvrUptimeCommon.onServerTick(e.getServer()));
    }
}
