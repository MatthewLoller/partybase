/*
 * Copyright (c) 2020, TheStonedTurtle <https://github.com/TheStonedTurtle>
 * Copyright (c) 2026, tjeerocka <https://github.com/tjeerocka>
 * Copyright (c) 2026, Heisty <https://github.com/Falsary>
 * Copyright (c) 2026, Matthew Loller <matthewloller@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.matthewloller.partybase;

import com.matthewloller.partybase.dps.model.MeterMode;
import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Alpha;
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

	@ConfigSection(name = "Party info", description = "The Party info tab: member banners and party controls", position = 0)
	String partyInfoSection = "partyInfo";

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
		name = "Simple defence label",
		description = "Above a monster: a short \"Def 120 / 246\" line after special attacks that lower it. Off by"
			+ " default: the Defence tracker sections below show defence in more detail.",
		position = 8,
		section = overlaySection
	)
	default boolean showDefence()
	{
		return false;
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
		description = "<html>Controls whether the sidebar icon is always shown (checked) or only shown while inside a party (unchecked)</html>",
		section = partyInfoSection
	)
	default boolean alwaysShowIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPartyControls",
		name = "Show Party Controls",
		description = "<html>Controls whether we display the party control buttons like create and leave party</html>",
		position = 0,
		section = partyInfoSection
	)
	default boolean showPartyControls()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPartyPassphrase",
		name = "Show Party Passphrase",
		description = "<html>Controls whether the party passphrase is displayed within the UI<br/>If disabled and party controls are shown you can still copy</html>",
		position = 1,
		section = partyInfoSection
	)
	default boolean showPartyPassphrase()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoExpandMembers",
		name = "Expand members by default",
		description = "<html>Controls whether party member details are automatically expanded (checked) or collapsed into banners (unchecked)</html>",
		position = 2,
		section = partyInfoSection
	)
	default boolean autoExpandMembers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "displayVirtualLevels",
		name = "Display Virtual Levels",
		description = "<html>Controls whether we display a players virtual level as their base level</html>",
		position = 3,
		section = partyInfoSection
	)
	default boolean displayVirtualLevels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "displayPlayerWorlds",
		name = "Display Player Worlds",
		description = "<html>Controls whether we display the world a player is currently on</html>",
		position = 4,
		section = partyInfoSection
	)
	default boolean displayPlayerWorlds()
	{
		return true;
	}


	@ConfigItem(
		keyName = "displayPartyReminderOverlay",
		name = "Display Party Reminder",
		description = "Displays an overlay when you are not in a party",
		position = 5,
		section = partyInfoSection
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

	// ---- Defence tracker (from Better Party Defence) ----

	String KEY_DEFENCE_FONT = "defenceFont";
	String KEY_CUSTOM_FONT_PATH = "customFontPath";
	String KEY_INFO_BOX_THEME_ICON = "defenceInfoBoxUseThemeSkillIcon";

	@Getter
	@RequiredArgsConstructor
	enum DefenceValueFormat
	{
		CURRENT("Current"), CURRENT_OF_BASE("Current / base"), PERCENT("Percent"), CURRENT_AND_PERCENT(
			"Current (percent)");

		private final String label;

		@Override
		public String toString()
		{
			return label;
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum DefenceDrainFormat
	{
		AMOUNT("Amount"), PERCENT("Percent"), OFF("Off");

		private final String label;

		@Override
		public String toString()
		{
			return label;
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum DefenceInfoBoxValue
	{
		CURRENT("Current defence"), PERCENT("Percent remaining"), DRAINED("Amount drained");

		private final String label;

		@Override
		public String toString()
		{
			return label;
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum DefenceOverlayPosition
	{
		ABOVE_HP_BAR("Above HP bar", 1.0, 55, 0), RIGHT_OF_HP_BAR("Right of HP bar", 1.0, 55,
			38), CENTRE_OF_NPC("Centre of NPC", 0.5, 0, 0), AT_NPC_FEET("At NPC feet", 0.0, 0, 0);

		private final String label;
		private final double heightFactor;
		private final int heightOffset;
		private final int xNudge;

		@Override
		public String toString()
		{
			return label;
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum DefenceThresholdUnit
	{
		LEVELS("Levels"), PERCENT("Percent of drainable");

		private final String label;

		@Override
		public String toString()
		{
			return label;
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum MagicDefenceDisplay
	{
		BONUS("Magic defence bonus"), LEVEL("Magic level"), PERCENT("Percent of starting roll"), BOTH(
			"Bonus and percent");

		private final String label;

		@Override
		public String toString()
		{
			return label;
		}
	}

	enum TrackerFont
	{
		RUNESCAPE("RuneScape"), SANS_SERIF("Sans serif"), SERIF("Serif"), MONOSPACED("Monospaced"), CUSTOM(
			"Custom font"), ADD_CUSTOM("Add custom font...");

		private final String label;

		TrackerFont(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}
	// Other Sidebar Settings
	@ConfigSection(name = "Defence: general", description = "Core tracker behaviour and compatibility options.", position = 20)
	String GENERAL = "defenceGeneral";

	@ConfigSection(name = "Defence: position", description = "Placement and formatting of Defence readouts.", position = 21)
	String MAIN_TRACKER = "defenceMainTracker";

	@ConfigSection(name = "Defence: info boxes", description = "RuneLite status-bar info boxes for Defence, Magic defence, and BPD weapon counters.", position = 22, closedByDefault = true)
	String INFO_BOXES = "defenceInfoBoxes";

	@ConfigSection(name = "Defence: text and icons", description = "Font, skill icons, and text appearance for Defence readouts.", position = 23, closedByDefault = true)
	String TEXT = "defenceText";

	@ConfigSection(name = "Defence: colours", description = "Colours and low-Defence threshold for Defence readouts.", position = 24, closedByDefault = true)
	String COLOURS = "defenceColours";

	@ConfigSection(name = "Defence: magic", description = "Magic-defence display settings.", position = 25, closedByDefault = true)
	String MAGIC = "defenceMagic";

	@ConfigSection(name = "Defence: effect info boxes", description = "Infoboxes for landed boss effects.", position = 26, closedByDefault = true)
	String OTHER_INFOBOXES = "defenceOtherInfoboxes";

	@ConfigItem(keyName = "defenceHpBar", name = "Show defence tracker", description = "Enable the Better Party Defence tracker.", position = 1, section = GENERAL)
	default boolean defenceHpBar()
	{
		return true;
	}

	@ConfigItem(keyName = "defenceAlwaysShow", name = "Display before special att", description = "Show the target's at its starting Defence before any tracked defence-draining special attack lands.", position = 2, section = GENERAL)
	default boolean defenceAlwaysShow()
	{
		return false;
	}

	@ConfigItem(keyName = "defenceHpBarPosition", name = "Position on NPC", description = "Where the Defence readout appears on each NPC.", position = 2, section = MAIN_TRACKER)
	default DefenceOverlayPosition defenceHpBarPosition()
	{
		return DefenceOverlayPosition.ABOVE_HP_BAR;
	}

	@Range(min = -200, max = 200)
	@Units(Units.PIXELS)
	@ConfigItem(keyName = "defenceHpBarYOffset", name = "Vertical offset", description = "Move Defence readouts vertically. Positive values move them up; negative values move them down.", position = 3, section = MAIN_TRACKER)
	default int defenceHpBarYOffset()
	{
		return 0;
	}

	@ConfigItem(keyName = "defenceValueFormat", name = "Defence value format", description = "Choose a Defence format, such as 142, 142/200, 71%, or 142 (71%).", position = 4, section = MAIN_TRACKER)
	default DefenceValueFormat defenceValueFormat()
	{
		return DefenceValueFormat.CURRENT;
	}

	@ConfigItem(keyName = "defenceDrainFormat", name = "Drain suffix", description = "Choose what appears after the down arrow: levels drained, percent drained, or nothing.", position = 5, section = MAIN_TRACKER)
	default DefenceDrainFormat defenceDrainFormat()
	{
		return DefenceDrainFormat.AMOUNT;
	}

	@ConfigItem(keyName = "defenceShowFullLevel", name = "Show full Defence", description = "Off (default): show only drainable Defence, On: show the actual Defence level down to its floor.", position = 6, section = MAIN_TRACKER)
	default boolean defenceShowFullLevel()
	{
		return false;
	}

	@ConfigItem(keyName = "defenceInfoBox", name = "Defence info box", description = "Show tracked Defence in the RuneLite info-box bar. This follows the shared Show full Defence level setting above.", position = 1, section = INFO_BOXES)
	default boolean defenceInfoBox()
	{
		return true;
	}

	@ConfigItem(keyName = "defenceInfoBoxValue", name = "Defence info-box value", description = "Choose the single Defence number shown inside the info box.", position = 2, section = INFO_BOXES)
	default DefenceInfoBoxValue defenceInfoBoxValue()
	{
		return DefenceInfoBoxValue.CURRENT;
	}

	@ConfigItem(keyName = "extraInfoInInfoBox", name = "Spec history on hover", description = "When hovering the Defence info box, show who used each tracked defence-draining special attack.", position = 3, section = INFO_BOXES)
	default boolean extraInfoInInfoBox()
	{
		return true;
	}

	@ConfigItem(keyName = "specWeaponInfoBoxes", name = "Weapon spec info boxes", description = "Show Better Party Defence's own weapon-icon counters using accepted encounter spec history and party sync. Turn off to hide the extra counters.", position = 4, section = INFO_BOXES)
	default boolean specWeaponInfoBoxes()
	{
		return true;
	}

	@ConfigItem(keyName = "magicDefenceInfoBox", name = "Magic info box", description = "Show tracked Magic-defence changes in a RuneLite info box. Requires Show magic defence to be enabled.", position = 5, section = INFO_BOXES)
	default boolean magicDefenceInfoBox()
	{
		return true;
	}

	@ConfigItem(keyName = KEY_INFO_BOX_THEME_ICON, name = "Use themed info-box icons", description = "Use Defence and Magic skill icons from the active RuneLite theme in Better Party Defence info boxes when available.", position = 6, section = INFO_BOXES)
	default boolean defenceInfoBoxUseThemeSkillIcon()
	{
		return false;
	}

	@ConfigItem(keyName = "defenceShowIcons", name = "Show skill icons", description = "Draw Defence and Magic skill icons in front of Defence readouts.", position = 1, section = TEXT)
	default boolean defenceShowIcons()
	{
		return true;
	}

	@ConfigItem(keyName = "defenceUseThemeSkillIcons", name = "Use themed tracker icons", description = "Use skill icons from the active RuneLite theme when available; otherwise use the standard icons.", position = 2, section = TEXT)
	default boolean defenceUseThemeSkillIcons()
	{
		return true;
	}

	@ConfigItem(keyName = KEY_DEFENCE_FONT, name = "Font", description = "Font used for Defence readouts. Choose Add custom font... to select a local font file.", position = 3, section = TEXT)
	default TrackerFont defenceFont()
	{
		return TrackerFont.RUNESCAPE;
	}

	@Range(min = 8, max = 48)
	@ConfigItem(keyName = "defenceFontSizePx", name = "Font size", description = "Font size for Defence readouts, in pixels.", position = 4, section = TEXT)
	default int defenceFontSize()
	{
		return 15;
	}

	@ConfigItem(keyName = "defenceBoldText", name = "Bold text", description = "Use bold text for Defence readouts.", position = 5, section = TEXT)
	default boolean defenceBoldText()
	{
		return false;
	}

	@ConfigItem(keyName = KEY_CUSTOM_FONT_PATH, name = "Custom font file", description = "Local font selected by Add custom font.", position = 6, section = TEXT, hidden = true)
	default String customFontPath()
	{
		return "";
	}

	@ConfigItem(keyName = "defenceTextPlate", name = "Text background", description = "Draw a translucent background behind Defence readouts.", position = 6, section = TEXT)
	default boolean defenceTextPlate()
	{
		return false;
	}

	@Range(min = 0, max = 500)
	@ConfigItem(keyName = "defenceLowThreshold", name = "Low-Defence threshold", description = "At or below this amount of remaining drainable Defence, use the Low Defence colour.", position = 1, section = COLOURS)
	default int defenceLowThreshold()
	{
		return 10;
	}

	@ConfigItem(keyName = "defenceLowThresholdUnit", name = "Threshold type", description = "Treat the Low-Defence threshold as levels above the Defence floor or as a percent of total drainable Defence.", position = 2, section = COLOURS)
	default DefenceThresholdUnit defenceLowThresholdUnit()
	{
		return DefenceThresholdUnit.LEVELS;
	}

	@Alpha
	@ConfigItem(keyName = "defenceHighColor", name = "Normal Defence colour", description = "Colour used while Defence is above the Low-Defence threshold.", position = 3, section = COLOURS)
	default Color defenceHighColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(keyName = "defenceLowColor", name = "Low Defence colour", description = "Colour used when Defence reaches or falls below the Low-Defence threshold.", position = 4, section = COLOURS)
	default Color defenceLowColor()
	{
		return new Color(0xC0, 0xAB, 0x46);
	}

	@Alpha
	@ConfigItem(keyName = "defenceCappedColor", name = "Fully drained colour", description = "Colour used when the target reaches its minimum Defence floor and cannot be drained further.", position = 5, section = COLOURS)
	default Color defenceCappedColor()
	{
		return new Color(0x57, 0x95, 0x49);
	}

	@Alpha
	@ConfigItem(keyName = "defenceDrainColor", name = "Drain amount colour", description = "Colour used for the down arrow and drained amount or percentage.", position = 6, section = COLOURS)
	default Color defenceDrainColor()
	{
		return new Color(0xAD, 0x14, 0x59);
	}

	@ConfigItem(keyName = "magicDefence", name = "Show magic defence", description = "Track and display Magic-defence changes caused by supported special attacks.", position = 1, section = MAGIC)
	default boolean magicDefence()
	{
		return true;
	}

	@ConfigItem(keyName = "magicDefenceDisplay", name = "Magic value shown", description = "Choose whether the tracker shows Magic-defence bonus, Magic level, percent of the starting magic-defence roll, or bonus plus percent.", position = 2, section = MAGIC)
	default MagicDefenceDisplay magicDefenceDisplay()
	{
		return MagicDefenceDisplay.BONUS;
	}

	@ConfigItem(keyName = "magicDefenceSameRow", name = "Place beside Defence", description = "Show the Magic-defence readout beside Defence instead of on a second line.", position = 3, section = MAGIC)
	default boolean magicDefenceSameRow()
	{
		return true;
	}

	@Alpha
	@ConfigItem(keyName = "magicDefenceColor", name = "Magic defence colour", description = "Colour used for the Magic-defence readout.", position = 4, section = MAGIC)
	default Color magicDefenceColor()
	{
		return new Color(0x22, 0x5E, 0xA8);
	}

	@ConfigItem(keyName = "showRedKeris", name = "Show red keris", description = "Red keris timer infobox on spec", position = 1, section = OTHER_INFOBOXES)
	default boolean showRedKeris()
	{
		return true;
	}

	@ConfigItem(keyName = "showShadowBarrage", name = "Show shadow barrage", description = "Must have shadow sceptre equipped", position = 2, section = OTHER_INFOBOXES)
	default boolean showShadowBarrage()
	{
		return true;
	}

	@ConfigItem(keyName = "showVulnerability", name = "Show vulnerability", description = "Shows a vulnerability infobox on successful hit", position = 3, section = OTHER_INFOBOXES)
	default boolean showVulnerability()
	{
		return true;
	}
}
