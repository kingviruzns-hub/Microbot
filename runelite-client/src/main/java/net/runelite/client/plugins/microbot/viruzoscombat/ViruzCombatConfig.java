package net.runelite.client.plugins.microbot.viruzoscombat;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(ViruzCombatConfig.configGroup)
public interface ViruzCombatConfig extends Config {
    String configGroup = "viruz-combat";

    @ConfigSection(
            name = "General",
            description = "General settings",
            position = 0
    )
    String generalSection = "general";

    @ConfigItem(
            keyName = "foodName",
            name = "Food Override",
            description = "Name of food to use. Leave blank for auto-detect.",
            position = 0,
            section = generalSection
    )
    default String foodName() {
        return "";
    }

    @ConfigItem(
            keyName = "rangedMode",
            name = "Ranged Mode",
            description = "Use ranged training",
            position = 1,
            section = generalSection
    )
    default boolean rangedMode() {
        return false;
    }
}
