package net.lunix.svruptime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class SvrUptimeCommon {

    // Ordered smallest → largest. Ordinal position drives cascade logic.
    public enum CalendarUnit {
        DAYS("days"),
        MONTHS("months"),
        YEARS("years"),
        DECADES("decades"),
        CENTURIES("centuries"),
        MILLENNIA("millennia");

        public final String id;
        CalendarUnit(String id) { this.id = id; }
    }

    private static final long TICKS_PER_DAY      = 24000L;
    private static final long EVENING_TICK        = 12000L;
    private static final long DAYS_PER_MONTH      = 30L;
    private static final long DAYS_PER_YEAR       = 360L;
    private static final long DAYS_PER_DECADE     = 3600L;
    private static final long DAYS_PER_CENTURY    = 36000L;
    private static final long DAYS_PER_MILLENNIUM = 360000L;

    private static final long SECONDS_PER_DAY    = 86400L;
    private static final long SECONDS_PER_MONTH  = SECONDS_PER_DAY  * 30L;
    private static final long SECONDS_PER_YEAR   = SECONDS_PER_DAY  * 365L;
    private static final long SECONDS_PER_DECADE = SECONDS_PER_YEAR * 10L;

    private static boolean broadcastEnabled      = true;
    private static boolean morningMessageEnabled = true;
    private static boolean eveningMessageEnabled = true;
    private static String  morningMessage        = "§6☀ A new day has begun.";
    private static String  eveningMessage        = "§9☾ The night has come.";

    private static long lastBroadcastDay = -1L;
    private static long lastEveningDay   = -1L;
    private static long cachedTotalTicks = 0L;
    private static long cachedTotalDays  = 0L;

    // One cached MC line set per largest-unit view, plus shared real-time lines.
    private static final Map<CalendarUnit, List<Component>> cachedMcByUnit = new EnumMap<>(CalendarUnit.class);
    private static List<Component> cachedRealTimeLines  = new ArrayList<>();
    private static List<Component> cachedBroadcastLines = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void init() {
        loadConfig();
        saveConfig();
        TickEvent.SERVER_POST.register(SvrUptimeCommon::onServerTick);
        CommandRegistrationEvent.EVENT.register((dispatcher, buildContext, environment) ->
            registerCommands(dispatcher, buildContext, environment)
        );
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    private static void registerCommands(
            com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
            net.minecraft.commands.CommandBuildContext buildContext,
            Commands.CommandSelection environment) {

        var query = Commands.literal("query")
            .executes(ctx -> queryUptime(ctx.getSource(), CalendarUnit.MILLENNIA));
        for (CalendarUnit unit : CalendarUnit.values()) {
            query.then(Commands.literal(unit.id)
                .executes(ctx -> queryUptime(ctx.getSource(), unit)));
        }

        var admin = Commands.literal("admin")
            .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("broadcast")
                .then(Commands.literal("enable") .executes(ctx -> setBroadcast(ctx.getSource(), true)))
                .then(Commands.literal("disable").executes(ctx -> setBroadcast(ctx.getSource(), false))))
            .then(Commands.literal("morning")
                .then(Commands.literal("enable") .executes(ctx -> setMorning(ctx.getSource(), true)))
                .then(Commands.literal("disable").executes(ctx -> setMorning(ctx.getSource(), false))))
            .then(Commands.literal("evening")
                .then(Commands.literal("enable") .executes(ctx -> setEvening(ctx.getSource(), true)))
                .then(Commands.literal("disable").executes(ctx -> setEvening(ctx.getSource(), false))))
            .then(Commands.literal("reload").executes(ctx -> reloadConfig(ctx.getSource())))
            .then(Commands.literal("status").executes(ctx -> adminStatus(ctx.getSource())));

        var rootNode = dispatcher.register(Commands.literal("uptime")
            .then(query)
            .then(Commands.literal("help").executes(ctx -> showHelp(ctx.getSource())))
            .then(admin)
        );
        dispatcher.register(Commands.literal("ut").redirect(rootNode));
    }

    // -------------------------------------------------------------------------
    // Command handlers
    // -------------------------------------------------------------------------

    private static int queryUptime(CommandSourceStack source, CalendarUnit largestUnit) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6=== Server Uptime ==="));
        lines.addAll(cachedMcByUnit.getOrDefault(largestUnit, List.of()));
        lines.addAll(cachedRealTimeLines);
        lines.forEach(source::sendSystemMessage);
        return 1;
    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal("§6=== SvrUptime Help ==="));
        source.sendSystemMessage(Component.literal("§e/uptime query [unit] §7— Show uptime (optional: largest unit to display)"));
        source.sendSystemMessage(Component.literal("§7  Units: days, months, years, decades, centuries, millennia"));
        source.sendSystemMessage(Component.literal("§e/uptime help §7— Show this help message"));
        if (source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            source.sendSystemMessage(Component.literal("§c--- Admin ---"));
            source.sendSystemMessage(Component.literal("§e/uptime admin broadcast enable|disable §7— Toggle the daily uptime broadcast"));
            source.sendSystemMessage(Component.literal("§e/uptime admin morning enable|disable §7— Toggle the morning message"));
            source.sendSystemMessage(Component.literal("§e/uptime admin evening enable|disable §7— Toggle the evening message"));
            source.sendSystemMessage(Component.literal("§e/uptime admin status §7— Show current settings"));
        }
        return 1;
    }

    private static int adminStatus(CommandSourceStack source) {
        long totalSeconds = cachedTotalTicks / 20L;
        long realDays     = totalSeconds / SECONDS_PER_DAY;

        source.sendSystemMessage(Component.literal("§6=== SvrUptime Status ==="));
        source.sendSystemMessage(Component.literal("§eBroadcast: "        + (broadcastEnabled      ? "§aEnabled" : "§cDisabled")));
        source.sendSystemMessage(Component.literal("§eMorning Message: "  + (morningMessageEnabled  ? "§aEnabled" : "§cDisabled")));
        source.sendSystemMessage(Component.literal("§eEvening Message: "  + (eveningMessageEnabled  ? "§aEnabled" : "§cDisabled")));
        source.sendSystemMessage(Component.literal("§eTotal World Ticks: §f" + cachedTotalTicks));
        source.sendSystemMessage(Component.literal("§eReal Days Elapsed: §f" + realDays));
        return 1;
    }

    private static int setBroadcast(CommandSourceStack source, boolean enabling) {
        broadcastEnabled = enabling;
        saveConfig();
        source.sendSystemMessage(Component.literal(enabling ? "§aBroadcast enabled." : "§cBroadcast disabled."));
        return 1;
    }

    private static int setMorning(CommandSourceStack source, boolean enabling) {
        morningMessageEnabled = enabling;
        saveConfig();
        source.sendSystemMessage(Component.literal(enabling ? "§aMorning message enabled." : "§cMorning message disabled."));
        return 1;
    }

    private static int setEvening(CommandSourceStack source, boolean enabling) {
        eveningMessageEnabled = enabling;
        saveConfig();
        source.sendSystemMessage(Component.literal(enabling ? "§aEvening message enabled." : "§cEvening message disabled."));
        return 1;
    }

    private static int reloadConfig(CommandSourceStack source) {
        loadConfig();
        source.sendSystemMessage(Component.literal("§aSvrUptime config reloaded."));
        return 1;
    }

    // -------------------------------------------------------------------------
    // Tick / day & evening detection
    // -------------------------------------------------------------------------

    private static void onServerTick(MinecraftServer server) {
        var world = server.overworld();
        if (world == null) return;

        long dayTime    = world.getDayTime();
        long currentDay = dayTime / TICKS_PER_DAY;
        long timeOfDay  = dayTime % TICKS_PER_DAY;

        // Initialize on first tick — don't fire messages for already-passed events.
        if (lastBroadcastDay < 0) {
            lastBroadcastDay = currentDay;
            lastEveningDay   = (timeOfDay >= EVENING_TICK) ? currentDay : currentDay - 1;
            rebuildCache(world.getGameTime());
            return;
        }

        // New day — uptime broadcast + morning message.
        if (currentDay > lastBroadcastDay) {
            lastBroadcastDay = currentDay;
            rebuildCache(world.getGameTime());
            broadcastUptime(server);
        }

        // Evening — fires once per day when time crosses 12000.
        if (timeOfDay >= EVENING_TICK && lastEveningDay < currentDay) {
            lastEveningDay = currentDay;
            broadcastEvening(server);
        }
    }

    // -------------------------------------------------------------------------
    // Broadcasts
    // -------------------------------------------------------------------------

    private static void broadcastUptime(MinecraftServer server) {
        if (broadcastEnabled) {
            cachedBroadcastLines.forEach(line ->
                server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(line))
            );
        }
        if (morningMessageEnabled) {
            Component msg = Component.literal(morningMessage);
            server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(msg));
        }
    }

    private static void broadcastEvening(MinecraftServer server) {
        if (!eveningMessageEnabled) return;
        Component msg = Component.literal(eveningMessage);
        server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(msg));
    }

    // -------------------------------------------------------------------------
    // Cache
    // -------------------------------------------------------------------------

    private static void rebuildCache(long totalTicks) {
        cachedTotalTicks = totalTicks;
        cachedTotalDays  = totalTicks / TICKS_PER_DAY;

        for (CalendarUnit unit : CalendarUnit.values()) {
            cachedMcByUnit.put(unit, buildMcLines(unit));
        }
        cachedRealTimeLines = buildRealTimeLines();

        cachedBroadcastLines = new ArrayList<>();
        cachedBroadcastLines.add(Component.literal("§6=== Server Uptime ==="));
        cachedBroadcastLines.addAll(cachedMcByUnit.get(CalendarUnit.MILLENNIA));
        cachedBroadcastLines.addAll(cachedRealTimeLines);
    }

    private static List<Component> buildMcLines(CalendarUnit largestUnit) {
        long remaining = cachedTotalDays;

        long millennia = 0, centuries = 0, decades = 0, years = 0, months = 0, days;

        if (CalendarUnit.MILLENNIA.ordinal() <= largestUnit.ordinal()) {
            millennia = remaining / DAYS_PER_MILLENNIUM;
            remaining %= DAYS_PER_MILLENNIUM;
        }
        if (CalendarUnit.CENTURIES.ordinal() <= largestUnit.ordinal()) {
            centuries = remaining / DAYS_PER_CENTURY;
            remaining %= DAYS_PER_CENTURY;
        }
        if (CalendarUnit.DECADES.ordinal() <= largestUnit.ordinal()) {
            decades = remaining / DAYS_PER_DECADE;
            remaining %= DAYS_PER_DECADE;
        }
        if (CalendarUnit.YEARS.ordinal() <= largestUnit.ordinal()) {
            years = remaining / DAYS_PER_YEAR;
            remaining %= DAYS_PER_YEAR;
        }
        if (CalendarUnit.MONTHS.ordinal() <= largestUnit.ordinal()) {
            months = remaining / DAYS_PER_MONTH;
            remaining %= DAYS_PER_MONTH;
        }
        days = remaining + 1; // 1-indexed: first day of world = Day 1

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§a- Minecraft Time"));

        if (millennia > 0)
            lines.add(Component.literal("§e  " + millennia + " " + plural(millennia, "Millennium", "Millennia")));
        if (centuries > 0)
            lines.add(Component.literal("§e  " + centuries + " " + plural(centuries, "Century", "Centuries")));
        if (decades > 0)
            lines.add(Component.literal("§e  " + decades + " " + plural(decades, "Decade", "Decades")));
        if (years > 0)
            lines.add(Component.literal("§e  " + years + " " + plural(years, "Year", "Years")));
        if (months > 0)
            lines.add(Component.literal("§e  " + months + " " + plural(months, "Month", "Months")));
        lines.add(Component.literal("§e  " + days + " " + plural(days, "Day", "Days")));

        return lines;
    }

    private static List<Component> buildRealTimeLines() {
        long totalSeconds = cachedTotalTicks / 20L;

        long decades = totalSeconds / SECONDS_PER_DECADE; totalSeconds %= SECONDS_PER_DECADE;
        long years   = totalSeconds / SECONDS_PER_YEAR;   totalSeconds %= SECONDS_PER_YEAR;
        long months  = totalSeconds / SECONDS_PER_MONTH;  totalSeconds %= SECONDS_PER_MONTH;
        long days    = totalSeconds / SECONDS_PER_DAY;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§d- Real Time"));

        if (decades > 0)
            lines.add(Component.literal("§b  " + decades + " " + plural(decades, "Decade",  "Decades")));
        if (years > 0)
            lines.add(Component.literal("§b  " + years   + " " + plural(years,   "Year",    "Years")));
        if (months > 0)
            lines.add(Component.literal("§b  " + months  + " " + plural(months,  "Month",   "Months")));
        if (days > 0)
            lines.add(Component.literal("§b  " + days    + " " + plural(days,    "Day",     "Days")));

        if (lines.size() == 1)
            lines.add(Component.literal("§b  0 Days"));

        return lines;
    }

    // -------------------------------------------------------------------------
    // Config persistence
    // -------------------------------------------------------------------------

    private static Path configPath() {
        return Platform.getConfigFolder().resolve("svruptime.json");
    }

    private static void loadConfig() {
        Path path = configPath();
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("broadcastEnabled"))      broadcastEnabled      = obj.get("broadcastEnabled").getAsBoolean();
            if (obj.has("morningMessageEnabled"))  morningMessageEnabled = obj.get("morningMessageEnabled").getAsBoolean();
            if (obj.has("eveningMessageEnabled"))  eveningMessageEnabled = obj.get("eveningMessageEnabled").getAsBoolean();
            if (obj.has("morningMessage"))         morningMessage        = obj.get("morningMessage").getAsString();
            if (obj.has("eveningMessage"))         eveningMessage        = obj.get("eveningMessage").getAsString();
        } catch (IOException ignored) {}
    }

    private static void saveConfig() {
        JsonObject obj = new JsonObject();
        obj.addProperty("broadcastEnabled",      broadcastEnabled);
        obj.addProperty("morningMessageEnabled",  morningMessageEnabled);
        obj.addProperty("eveningMessageEnabled",  eveningMessageEnabled);
        obj.addProperty("morningMessage",         morningMessage);
        obj.addProperty("eveningMessage",         eveningMessage);
        try (Writer writer = Files.newBufferedWriter(configPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(obj, writer);
        } catch (IOException ignored) {}
    }

    private static String plural(long count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }
}
