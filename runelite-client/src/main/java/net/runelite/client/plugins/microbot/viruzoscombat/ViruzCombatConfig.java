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

    @ConfigItem(
            keyName = "attackNpc",
            name = "Attack NPC",
            description = "NPC name to attack (leave empty for auto)",
            position = 2,
            section = generalSection
    )
    default String attackNpc() {
        return "";
    }

    @ConfigItem(
            keyName = "usePrayer",
            name = "Use Prayer",
            description = "Auto flick/use protection prayers",
            position = 3,
            section = generalSection
    )
    default boolean usePrayer() {
        return false;
    }

    @ConfigItem(
            keyName = "lootItems",
            name = "Loot Items",
            description = "Loot valuable items",
            position = 4,
            section = generalSection
    )
    default boolean lootItems() {
        return true;
    }

    @ConfigItem(
            keyName = "buryBones",
            name = "Bury Bones",
            description = "Bury bones automatically",
            position = 5,
            section = generalSection
    )
    default boolean buryBones() {
        return false;
    }
}
