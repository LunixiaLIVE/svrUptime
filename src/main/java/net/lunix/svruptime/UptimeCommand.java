package net.lunix.svruptime;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.*;

import java.util.*;

public class UptimeCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("query")) {
            UptimeManager.CalendarUnit unit = UptimeManager.CalendarUnit.MILLENNIA;
            if (args.length >= 2) unit = UptimeManager.CalendarUnit.fromId(args[1].toLowerCase());
            UptimeManager.getUptimeLines(unit).forEach(l -> sender.sendMessage(LEGACY.deserialize(l)));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help"  -> showHelp(sender);
            case "admin" -> handleAdmin(sender, args);
            default      -> sender.sendMessage(LEGACY.deserialize("§cUnknown subcommand. Use /uptime help."));
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        send(sender,
            "§6=== SvrUptime Help ===",
            "§e/uptime query [unit] §7— Show uptime (optional: largest unit)",
            "§7  Units: days, months, years, decades, centuries, millennia",
            "§e/uptime help §7— Show this help"
        );
        if (sender.hasPermission("svruptime.admin")) {
            send(sender,
                "§c--- Admin ---",
                "§e/uptime admin broadcast enable|disable",
                "§e/uptime admin morning enable|disable",
                "§e/uptime admin evening enable|disable",
                "§e/uptime admin reload",
                "§e/uptime admin status"
            );
        }
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("svruptime.admin")) {
            sender.sendMessage(LEGACY.deserialize("§cYou do not have permission."));
            return;
        }
        if (args.length < 2) { showAdminHelp(sender); return; }

        switch (args[1].toLowerCase()) {
            case "broadcast" -> {
                if (args.length < 3) { send(sender, "§cUsage: /uptime admin broadcast enable|disable"); return; }
                boolean en = args[2].equalsIgnoreCase("enable");
                UptimeManager.broadcastEnabled = en;
                UptimeManager.saveConfig();
                send(sender, en ? "§aBroadcast enabled." : "§cBroadcast disabled.");
            }
            case "morning" -> {
                if (args.length < 3) { send(sender, "§cUsage: /uptime admin morning enable|disable"); return; }
                boolean en = args[2].equalsIgnoreCase("enable");
                UptimeManager.morningMessageEnabled = en;
                UptimeManager.saveConfig();
                send(sender, en ? "§aMorning message enabled." : "§cMorning message disabled.");
            }
            case "evening" -> {
                if (args.length < 3) { send(sender, "§cUsage: /uptime admin evening enable|disable"); return; }
                boolean en = args[2].equalsIgnoreCase("enable");
                UptimeManager.eveningMessageEnabled = en;
                UptimeManager.saveConfig();
                send(sender, en ? "§aEvening message enabled." : "§cEvening message disabled.");
            }
            case "reload" -> {
                UptimeManager.loadConfig();
                send(sender, "§aSvrUptime config reloaded.");
            }
            case "status" -> {
                long totalSeconds = UptimeManager.getCachedTotalTicks() / 20L;
                long realDays = totalSeconds / 86400L;
                send(sender,
                    "§6=== SvrUptime Status ===",
                    "§eBroadcast: "        + (UptimeManager.broadcastEnabled      ? "§aEnabled" : "§cDisabled"),
                    "§eMorning Message: "  + (UptimeManager.morningMessageEnabled  ? "§aEnabled" : "§cDisabled"),
                    "§eEvening Message: "  + (UptimeManager.eveningMessageEnabled  ? "§aEnabled" : "§cDisabled"),
                    "§eTotal World Ticks: §f" + UptimeManager.getCachedTotalTicks(),
                    "§eReal Days Elapsed: §f" + realDays
                );
            }
            default -> showAdminHelp(sender);
        }
    }

    private void showAdminHelp(CommandSender sender) {
        send(sender,
            "§6=== SvrUptime Admin ===",
            "§e/uptime admin broadcast enable|disable",
            "§e/uptime admin morning enable|disable",
            "§e/uptime admin evening enable|disable",
            "§e/uptime admin reload",
            "§e/uptime admin status"
        );
    }

    private void send(CommandSender sender, String... lines) {
        for (String l : lines) sender.sendMessage(LEGACY.deserialize(l));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1)
            return filter(List.of("query", "help", "admin"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("query"))
            return filter(List.of("days", "months", "years", "decades", "centuries", "millennia"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("svruptime.admin"))
            return filter(List.of("broadcast", "morning", "evening", "reload", "status"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")
                && List.of("broadcast", "morning", "evening").contains(args[1].toLowerCase()))
            return filter(List.of("enable", "disable"), args[2]);
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream().filter(s -> s.startsWith(prefix.toLowerCase())).toList();
    }
}
