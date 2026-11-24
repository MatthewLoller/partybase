/*
 * Party settings come from Party Panel, Copyright (c) 2020, TheStonedTurtle.
 * DPS settings come from GoldenDPS, Copyright (c) 2026, tjeerocka.
 * Both BSD 2-Clause; see LICENSE.
 */
package com.matthewloller.partybase;

import com.matthewloller.partybase.dps.model.MeterMode;
import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(PartybaseConfig.GROUP)
public interface PartybaseConfig extends Config
{
	String GROUP = "partybase";
	/** Saved state that is no setting (panel sections, the Barrows run): changes need no config handling. */
	String STATE_GROUP = "partybasestate";
	String MODE_KEY = "overlayMode";
	String BOSS_ONLY_KEY = "bossOnly";
	String SCOPE_KEY = "overlayScope";

	enum Scope
	{
		FIGHT("Current / last fight"),
		SESSION("Session total");

		private final String label;

		Scope(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigSection(name = "DPS: party", description = "Sharing DPS with your RuneLite party", position = 10)
	String partySection = "dpsParty";

	@ConfigSection(name = "DPS: overlay", description = "The DPS meter on the game screen", position = 11)
	String overlaySection = "dpsOverlay";

	@ConfigSection(name = "DPS: fun", description = "Extras for group fights", position = 12)
	String funSection = "dpsFun";

	// Boss only, Show and Data are set in the overlay right-click menu and the panel; hidden here
	@ConfigItem(
		keyName = BOSS_ONLY_KEY,
		name = "Boss only",
		description = "Only count damage on bosses",
		hidden = true
	)
	default boolean bossOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareWithParty",
		name = "Share with party",
		description = "Send your numbers to party members who run Partybase",
		position = 0,
		section = partySection
	)
	default boolean shareWithParty()
	{
		return true;
	}

	@ConfigItem(
		keyName = "othersOnlyTogether",
		name = "Others only when together",
		description = "Count party members only when you are in the same instance or fight the same boss",
		position = 1,
		section = partySection
	)
	default boolean othersOnlyTogether()
	{
		return true;
	}

	@ConfigItem(
		keyName = "estimateOthers",
		name = "Show other players",
		description = "Also count players outside your party: their damage is guessed from their attacks (marked"
			+ " with ~). Off: only you and your party members show.",
		position = 2,
		section = partySection
	)
	default boolean estimateOthers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoRaidParty",
		name = "Raid party",
		description = "In CoX, ToB, ToA and the Fractured Archive, join a party with everyone in your raid who"
			+ " runs Partybase automatically, and leave it after the raid. A party you joined yourself is never changed.",
		position = 3,
		section = partySection
	)
	default boolean autoRaidParty()
	{
		return false;
	}

	@ConfigItem(
		keyName = "autoBossReport",
		name = "Auto boss report",
		description = "When a boss dies, the party leader sends the boss damage to the party chat by itself",
		position = 4,
		section = partySection
	)
	default boolean autoBossReport()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ownParty",
		name = "Own party",
		description = "When you start a boss fight (or a Barrows run) in no party, put you in a party of your own,"
			+ " so the automatic boss and Barrows reports also work solo. Leaving a party yourself, or losing it,"
			+ " turns it off until you log out.",
		position = 5,
		section = partySection
	)
	default boolean ownParty()
	{
		return false;
	}

	@ConfigItem(
		keyName = "leaveOnLogout",
		name = "Leave party on logout",
		description = "Leave the party 10 seconds after you log out, unless you are back in by then"
			+ " (world hops never leave)",
		position = 6,
		section = partySection
	)
	default boolean leaveOnLogout()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatColor",
		name = "Chat color",
		description = "The color of Partybase messages in your chatbox (leaving and kicks stay orange and red)",
		position = 7,
		section = partySection
	)
	default Color chatColor()
	{
		return new Color(0, 190, 210);
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Show the meter on the game screen",
		position = 0,
		section = overlaySection
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = MODE_KEY,
		name = "Show",
		description = "What the bars show",
		hidden = true
	)
	default MeterMode overlayMode()
	{
		return MeterMode.DAMAGE;
	}

	@ConfigItem(
		keyName = SCOPE_KEY,
		name = "Data",
		description = "The current fights or the total of this session",
		hidden = true
	)
	default Scope overlayScope()
	{
		return Scope.FIGHT;
	}

	@ConfigItem(
		keyName = "compactOverlay",
		name = "Compact",
		description = "A narrow meter: short names (\"Franz\" becomes \"Fr\") and only the damage (or the number"
			+ " you show)",
		position = 1,
		section = overlaySection
	)
	default boolean compactOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "maxRows",
		name = "Max bars",
		description = "How many bars to show",
		position = 2,
		section = overlaySection
	)
	@Range(min = 1, max = 20)
	default int maxRows()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "overlayWidth",
		name = "Width",
		description = "Overlay width",
		position = 3,
		section = overlaySection
	)
	@Range(min = 140, max = 400)
	@Units(Units.PIXELS)
	default int overlayWidth()
	{
		return 240;
	}

	@ConfigItem(
		keyName = "hideWhenEmpty",
		name = "Hide when empty",
		description = "Hide the overlay when there is nothing to show",
		position = 4,
		section = overlaySection
	)
	default boolean hideWhenEmpty()
	{
		return true;
	}

	@ConfigItem(
		keyName = "myColor",
		name = "My bar color",
		description = "Color of your own bar",
		position = 5,
		section = overlaySection
	)
	default Color myColor()
	{
		return new Color(255, 196, 0);
	}


	@ConfigItem(
		keyName = "showOverallMeter",
		name = "Overall meter",
		description = "The damage meter for the whole fight, on the game screen and in the DPS tab. Off: only the"
			+ " boss meter shows (bosses and marked targets).",
		position = 6,
		section = overlaySection
	)
	default boolean showOverallMeter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bossMeter",
		name = "Boss meter",
		description = "A second meter with only the damage on the boss, over the boss time",
		position = 7,
		section = overlaySection
	)
	default boolean bossMeter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDefence",
		name = "Defence",
		description = "Above a monster: its defence after special attacks that lower it (DWH, BGS, Elder maul, Arclight...)",
		position = 8,
		section = overlaySection
	)
	default boolean showDefence()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHitChance",
		name = "Hit chance",
		description = "Above the monster you attack: your chance to hit it with a normal attack, from your levels, "
			+ "prayers, gear and attack style",
		position = 9,
		section = overlaySection
	)
	default boolean showHitChance()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mvpCrown",
		name = "Crown on the MVP",
		description = "A crown over the player with the most damage in a group fight",
		position = 0,
		section = funSection
	)
	default boolean mvpCrown()
	{
		return true;
	}

	@ConfigItem(
		keyName = "healerSnack",
		name = "Snack on the top healer",
		description = "A burger or fries and hungry lines over the player who healed the most in a group fight",
		position = 1,
		section = funSection
	)
	default boolean healerSnack()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMaxHit",
		name = "Max hit on the meter",
		description = "A Max column with every player's biggest hit in the damage and DPS bars",
		position = 2,
		section = funSection
	)
	default boolean showMaxHit()
	{
		return true;
	}

	// ---- Party info (from Party Panel) ----

	@ConfigItem(
			keyName = "alwaysShowIcon",
			name = "Always show sidebar",
			description = "<html>Controls whether the sidebar icon is always shown (checked) or only shown while inside a party (unchecked)</html>"
	)
	default boolean alwaysShowIcon()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showPartyControls",
			name = "Show Party Controls",
			description = "<html>Controls whether we display the party control buttons like create and leave party</html>",
			position = 0
	)
	default boolean showPartyControls()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showPartyPassphrase",
			name = "Show Party Passphrase",
			description = "<html>Controls whether the party passphrase is displayed within the UI<br/>If disabled and party controls are shown you can still copy</html>",
			position = 1
	)
	default boolean showPartyPassphrase()
	{
		return true;
	}

	@ConfigItem(
			keyName = "autoExpandMembers",
			name = "Expand members by default",
			description = "<html>Controls whether party member details are automatically expanded (checked) or collapsed into banners (unchecked)</html>",
			position = 2
	)
	default boolean autoExpandMembers()
	{
		return false;
	}

	@ConfigItem(
			keyName = "displayVirtualLevels",
			name = "Display Virtual Levels",
			description = "<html>Controls whether we display a players virtual level as their base level</html>",
			position = 3
	)
	default boolean displayVirtualLevels()
	{
		return true;
	}

	@ConfigItem(
			keyName = "displayPlayerWorlds",
			name = "Display Player Worlds",
			description = "<html>Controls whether we display the world a player is currently on</html>",
			position = 4
	)
	default boolean displayPlayerWorlds()
	{
		return true;
	}


	@ConfigItem(
			keyName = "displayPartyReminderOverlay",
			name = "Display Party Reminder",
			description = "Displays an overlay when you are not in a party",
			position = 5
	)
	default boolean displayPartyReminderOverlay()
	{
		return false;
	}


	@ConfigItem(
			keyName = "previousPartyId",
			name = "",
			description = "",
			hidden = true
	)
	default String previousPartyId()
	{
		return "";
	}

	@ConfigItem(
			keyName = "previousPartyId",
			name = "",
			description = "",
			hidden = true
	)
	void setPreviousPartyId(String id);
}
