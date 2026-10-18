package net.runelite.client.plugins.microbot.viruzosbot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(ViruzFFABotConfig.configGroup)
public interface ViruzFFABotConfig extends Config {
    String configGroup = "viruz-ffa-bot";

    @ConfigSection(name = "Bot Toggles", description = "Enable/disable bot features", position = 0)
    String toggles = "toggles";

    @ConfigItem(keyName = "attack", name = "Attack", description = "Attack targets", position = 0, section = toggles)
    default boolean attack() { return true; }

    @ConfigItem(keyName = "walkToTarget", name = "Walk to target", description = "Walk to target", position = 1, section = toggles)
    default boolean walkToTarget() { return false; }

    @ConfigItem(keyName = "quickPrayer", name = "Quick prayer", description = "Quick prayer", position = 2, section = toggles)
    default boolean quickPrayer() { return false; }

    @ConfigItem(keyName = "prayerOff", name = "Turn prayer off if no target", description = "Turn prayer off if no target", position = 3, section = toggles)
    default boolean prayerOff() { return false; }

    @ConfigItem(keyName = "switchPrayers", name = "Switch prayers", description = "Switch prayers", position = 4, section = toggles)
    default boolean switchPrayers() { return false; }

    @ConfigItem(keyName = "freeRoam", name = "Free roam", description = "Free roam", position = 5, section = toggles)
    default boolean freeRoam() { return false; }

    @ConfigItem(keyName = "protectBluePhats", name = "Protect BluePhats", description = "Protect BluePhats", position = 6, section = toggles)
    default boolean protectBluePhats() { return false; }

    @ConfigItem(keyName = "report", name = "Report targets", description = "Report targets", position = 7, section = toggles)
    default boolean report() { return false; }

    @ConfigItem(keyName = "chatGrabber", name = "Chat grabber", description = "Chat grabber", position = 8, section = toggles)
    default boolean chatGrabber() { return false; }

    @ConfigItem(keyName = "lureMulti", name = "Lure Multi", description = "Lure Multi", position = 9, section = toggles)
    default boolean lureMulti() { return false; }

    @ConfigItem(keyName = "scanHills", name = "Scan Hills", description = "Scan Hills", position = 10, section = toggles)
    default boolean scanHills() { return false; }

    @ConfigItem(keyName = "noDrag", name = "No Drag", description = "No Drag", position = 11, section = toggles)
    default boolean noDrag() { return false; }

    @ConfigSection(name = "Player Lists", description = "Comma-separated player name lists", position = 1)
    String playerLists = "playerLists";

    @ConfigItem(keyName = "focusPlayers", name = "Focus Players", description = "Only target these players (comma-separated)", position = 0, section = playerLists)
    default String focusPlayers() { return ""; }

    @ConfigItem(keyName = "unlist", name = "Unlist", description = "Players to unlist on start (comma-separated)", position = 1, section = playerLists)
    default String unlist() { return ""; }

    @ConfigItem(keyName = "whitelist", name = "Whitelist", description = "Players to whitelist on start (comma-separated)", position = 2, section = playerLists)
    default String whitelist() { return ""; }
}
