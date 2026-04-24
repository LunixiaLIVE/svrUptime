package net.lunix.svruptime;

import org.bukkit.plugin.java.JavaPlugin;

public class SvrUptimePaper extends JavaPlugin {

    private static SvrUptimePaper instance;

    @Override
    public void onEnable() {
        instance = this;
        UptimeManager.init(this);
        var cmd = getCommand("uptime");
        if (cmd != null) {
            var handler = new UptimeCommand();
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
    }

    @Override
    public void onDisable() {
        // Bukkit scheduler cancels all tasks automatically on disable
    }

    public static SvrUptimePaper getInstance() {
        return instance;
    }
}
