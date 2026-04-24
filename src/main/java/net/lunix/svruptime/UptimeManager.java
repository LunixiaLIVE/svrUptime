package net.lunix.svruptime;

import com.google.gson.*;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class UptimeManager {

    public enum CalendarUnit {
        DAYS("days"),
        MONTHS("months"),
        YEARS("years"),
        DECADES("decades"),
        CENTURIES("centuries"),
        MILLENNIA("millennia");

        public final String id;
        CalendarUnit(String id) { this.id = id; }

        public static CalendarUnit fromId(String id) {
            for (CalendarUnit u : values()) if (u.id.equals(id)) return u;
            return MILLENNIA;
        }
    }

    private static final long TICKS_PER_DAY       = 24000L;
    private static final long EVENING_TICK         = 12000L;
    private static final long DAYS_PER_MONTH       = 30L;
    private static final long DAYS_PER_YEAR        = 360L;
    private static final long DAYS_PER_DECADE      = 3600L;
    private static final long DAYS_PER_CENTURY     = 36000L;
    private static final long DAYS_PER_MILLENNIUM  = 360000L;
    private static final long SECONDS_PER_DAY      = 86400L;
    private static final long SECONDS_PER_MONTH    = SECONDS_PER_DAY * 30L;
    private static final long SECONDS_PER_YEAR     = SECONDS_PER_DAY * 365L;
    private static final long SECONDS_PER_DECADE   = SECONDS_PER_YEAR * 10L;

    static boolean broadcastEnabled      = true;
    static boolean morningMessageEnabled = true;
    static boolean eveningMessageEnabled = true;
    static String  morningMessage        = "§6☀ A new day has begun.";
    static String  eveningMessage        = "§9☾ The night has come.";

    private static long lastBroadcastDay = -1L;
    private static long lastEveningDay   = -1L;
    private static long cachedTotalTicks = 0L;
    private static long cachedTotalDays  = 0L;

    private static final Map<CalendarUnit, List<String>> cachedMcByUnit = new EnumMap<>(CalendarUnit.class);
    private static List<String> cachedRealTimeLines  = new ArrayList<>();
    private static List<String> cachedBroadcastLines = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static SvrUptimePaper plugin;

    static void init(SvrUptimePaper p) {
        plugin = p;
        loadConfig();
        saveConfig();
        new BukkitRunnable() {
            @Override public void run() { tick(); }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    static List<String> getUptimeLines(CalendarUnit largestUnit) {
        List<String> lines = new ArrayList<>();
        lines.add("§6=== Server Uptime ===");
        lines.addAll(cachedMcByUnit.getOrDefault(largestUnit, List.of()));
        lines.addAll(cachedRealTimeLines);
        return lines;
    }

    static long getCachedTotalTicks() { return cachedTotalTicks; }

    private static void tick() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) return;

        long fullTime  = world.getFullTime();
        long timeOfDay = world.getTime();
        long currentDay = fullTime / TICKS_PER_DAY;

        if (lastBroadcastDay < 0) {
            lastBroadcastDay = currentDay;
            lastEveningDay   = (timeOfDay >= EVENING_TICK) ? currentDay : currentDay - 1;
            rebuildCache(fullTime);
            return;
        }

        if (currentDay > lastBroadcastDay) {
            lastBroadcastDay = currentDay;
            rebuildCache(fullTime);
            if (broadcastEnabled)
                cachedBroadcastLines.forEach(l ->
                    Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(LEGACY.deserialize(l))));
            if (morningMessageEnabled)
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(LEGACY.deserialize(morningMessage)));
        }

        if (timeOfDay >= EVENING_TICK && lastEveningDay < currentDay) {
            lastEveningDay = currentDay;
            if (eveningMessageEnabled)
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(LEGACY.deserialize(eveningMessage)));
        }
    }

    private static void rebuildCache(long totalTicks) {
        cachedTotalTicks = totalTicks;
        cachedTotalDays  = totalTicks / TICKS_PER_DAY;

        for (CalendarUnit unit : CalendarUnit.values())
            cachedMcByUnit.put(unit, buildMcLines(unit));
        cachedRealTimeLines = buildRealTimeLines();

        cachedBroadcastLines = new ArrayList<>();
        cachedBroadcastLines.add("§6=== Server Uptime ===");
        cachedBroadcastLines.addAll(cachedMcByUnit.get(CalendarUnit.MILLENNIA));
        cachedBroadcastLines.addAll(cachedRealTimeLines);
    }

    private static List<String> buildMcLines(CalendarUnit largestUnit) {
        long remaining = cachedTotalDays;
        long millennia = 0, centuries = 0, decades = 0, years = 0, months = 0, days;

        if (CalendarUnit.MILLENNIA.ordinal() <= largestUnit.ordinal()) { millennia = remaining / DAYS_PER_MILLENNIUM; remaining %= DAYS_PER_MILLENNIUM; }
        if (CalendarUnit.CENTURIES.ordinal() <= largestUnit.ordinal()) { centuries = remaining / DAYS_PER_CENTURY;    remaining %= DAYS_PER_CENTURY;    }
        if (CalendarUnit.DECADES.ordinal()   <= largestUnit.ordinal()) { decades   = remaining / DAYS_PER_DECADE;     remaining %= DAYS_PER_DECADE;     }
        if (CalendarUnit.YEARS.ordinal()     <= largestUnit.ordinal()) { years     = remaining / DAYS_PER_YEAR;       remaining %= DAYS_PER_YEAR;       }
        if (CalendarUnit.MONTHS.ordinal()    <= largestUnit.ordinal()) { months    = remaining / DAYS_PER_MONTH;      remaining %= DAYS_PER_MONTH;      }
        days = remaining + 1;

        List<String> lines = new ArrayList<>();
        lines.add("§a- Minecraft Time");
        if (millennia > 0) lines.add("§e  " + millennia + " " + plural(millennia, "Millennium", "Millennia"));
        if (centuries > 0) lines.add("§e  " + centuries + " " + plural(centuries, "Century",    "Centuries"));
        if (decades   > 0) lines.add("§e  " + decades   + " " + plural(decades,   "Decade",     "Decades"));
        if (years     > 0) lines.add("§e  " + years     + " " + plural(years,     "Year",       "Years"));
        if (months    > 0) lines.add("§e  " + months    + " " + plural(months,    "Month",      "Months"));
        lines.add("§e  " + days + " " + plural(days, "Day", "Days"));
        return lines;
    }

    private static List<String> buildRealTimeLines() {
        long totalSeconds = cachedTotalTicks / 20L;
        long decades = totalSeconds / SECONDS_PER_DECADE; totalSeconds %= SECONDS_PER_DECADE;
        long years   = totalSeconds / SECONDS_PER_YEAR;   totalSeconds %= SECONDS_PER_YEAR;
        long months  = totalSeconds / SECONDS_PER_MONTH;  totalSeconds %= SECONDS_PER_MONTH;
        long days    = totalSeconds / SECONDS_PER_DAY;

        List<String> lines = new ArrayList<>();
        lines.add("§d- Real Time");
        if (decades > 0) lines.add("§b  " + decades + " " + plural(decades, "Decade",  "Decades"));
        if (years   > 0) lines.add("§b  " + years   + " " + plural(years,   "Year",    "Years"));
        if (months  > 0) lines.add("§b  " + months  + " " + plural(months,  "Month",   "Months"));
        if (days    > 0) lines.add("§b  " + days    + " " + plural(days,    "Day",     "Days"));
        if (lines.size() == 1) lines.add("§b  0 Days");
        return lines;
    }

    private static String plural(long n, String singular, String plural) {
        return n == 1 ? singular : plural;
    }

    static void loadConfig() {
        Path path = configPath();
        if (!Files.exists(path)) return;
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            if (obj.has("broadcastEnabled"))      broadcastEnabled      = obj.get("broadcastEnabled").getAsBoolean();
            if (obj.has("morningMessageEnabled"))  morningMessageEnabled = obj.get("morningMessageEnabled").getAsBoolean();
            if (obj.has("eveningMessageEnabled"))  eveningMessageEnabled = obj.get("eveningMessageEnabled").getAsBoolean();
            if (obj.has("morningMessage"))         morningMessage        = obj.get("morningMessage").getAsString();
            if (obj.has("eveningMessage"))         eveningMessage        = obj.get("eveningMessage").getAsString();
        } catch (IOException ignored) {}
    }

    static void saveConfig() {
        JsonObject obj = new JsonObject();
        obj.addProperty("broadcastEnabled",      broadcastEnabled);
        obj.addProperty("morningMessageEnabled",  morningMessageEnabled);
        obj.addProperty("eveningMessageEnabled",  eveningMessageEnabled);
        obj.addProperty("morningMessage",         morningMessage);
        obj.addProperty("eveningMessage",         eveningMessage);
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(obj, w);
            }
        } catch (IOException ignored) {}
    }

    private static Path configPath() {
        return plugin.getDataFolder().toPath().resolve("config.json");
    }
}
