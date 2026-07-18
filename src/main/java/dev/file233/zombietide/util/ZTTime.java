package dev.file233.zombietide.util;

/** Time formatting shared by the HUD and the commands. */
public final class ZTTime {
    private ZTTime() {}

    /** "2d 03:04:05" (days omitted when zero, e.g. "00:07:42"), treating 20 ticks = 1 real second. */
    public static String formatRealtime(long ticks) {
        long totalSeconds = ticks / 20L;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        String hms = "%02d:%02d:%02d".formatted(hours, minutes, seconds);
        return days > 0 ? days + "d " + hms : hms;
    }

    /** Same shape, but expressed in Minecraft days/hours (1 MC day = 24000 ticks). */
    public static String formatIngame(long ticks) {
        long days = ticks / 24000L;
        long rem = ticks % 24000L;
        long hours = rem / 1000L;
        rem %= 1000L;
        long minutes = (rem * 60L) / 1000L;
        long seconds = 0L;
        String hms = "%02d:%02d:%02d".formatted(hours, minutes, seconds);
        return days > 0 ? days + "d " + hms : hms;
    }
}
