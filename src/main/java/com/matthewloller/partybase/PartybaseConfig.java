/*
 * Copyright (c) 2020, TheStonedTurtle <https://github.com/TheStonedTurtle>
 * Copyright (c) 2026, tjeerocka <https://github.com/tjeerocka>
 * Copyright (c) 2026, Heisty <https://github.com/Falsary>
 * Copyright (c) 2020, vincent0955 <https://github.com/vincent0955>
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
import com.matthewloller.partybase.metronome.FontTypes;
import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
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

	String SERVER_URL_KEY = "serverUrl";
	String SERVER_KEY_KEY = "serverKey";

	@ConfigSection(name = "Party server", description = "Use your own Partybase relay server instead of RuneLite's party"
			+ " server", position = 1, closedByDefault = true)
	String partyServerSection = "partyServer";

	@ConfigItem(
		keyName = SERVER_URL_KEY,
		name = "Server URL",
		description = "<html>A Partybase relay server (wss://...). Empty: use RuneLite's party server.<br>"
			+ "When set, all Partybase party data goes to this server instead, so everyone in the party must use"
			+ " the same server.</html>",
		position = 0,
		section = partyServerSection
	)
	default String serverUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = SERVER_KEY_KEY,
		name = "Server key",
		description = "Your personal key for the server above. Leave empty if the server needs no key.",
		position = 1,
		secret = true,
		section = partyServerSection
	)
	default String serverKey()
	{
		return "";
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

	// ---- Metronome (from Visual Metronome) ----

	@ConfigSection(name = "Metronome", description = "A visual cue every game tick, synced to the party leader",
		position = 40, closedByDefault = true)
	String metronomeSection = "metronome";

	@ConfigItem(
		keyName = "metronomeEnabled",
		name = "Show metronome",
		description = "Show the tick metronome overlays",
		position = 0,
		section = metronomeSection
	)
	default boolean metronomeEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "metronomeSyncLeader",
		name = "Sync with party leader",
		description = "<html>Follow the party leader's metronome: their tick position, tick counts and number of"
			+ " colours.<br>The leader's own metronome is not changed.</html>",
		position = 1,
		section = metronomeSection
	)
	default boolean metronomeSyncLeader()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		section = metronomeSection,
		keyName = "enableMetronome",
		name = "Visual Metronome",
		description = "Enable visual metronome"
	)
	default boolean enableMetronome()
	{
		return true;
	}

	@Range(
		min = 16
	)
	@ConfigItem(
		position = 2,
		keyName = "boxWidth",
		name = "Default Box Size (Alt + Right Click Box)",
		description = "Configure the default length and width of the box. Use alt + right click on the box to reset to the size specified",
		hidden = true
	)
	default int boxWidth()
	{
		return 25;
	}

	@Range(
		min = 1
	)
	@ConfigItem(
		position = 3,
		section = metronomeSection,
		keyName = "tickCount",
		name = "Tick Count",
		description = "The tick on which the color changes"
	)
	default int tickCount()
	{
		return 1;
	}

	@ConfigSection(
		name = "Metronome: Tick Number Settings",
		description = "Change Tick Number settings",
		position = 46,
		closedByDefault = true
	)
	String TickNumberSettings = "Tick Number Settings";

	@ConfigItem(
		position = 1,
		keyName = "showTick",
		name = "Show Metronome Tick Number",
		description = "Shows current tick number on the metronome",
		section = TickNumberSettings
	)
	default boolean showTick()
	{
		return false;
	}

	@ConfigItem(
		position = 2,
		keyName = "showPlayerTick",
		name = "Show Tick Number Above Player",
		description = "Shows current tick number above the player",
		section = TickNumberSettings
	)
	default boolean showPlayerTick()
	{
		return false;
	}

	@ConfigItem(
		position = 3,
		keyName = "disableFontScaling",
		name = "Disable Font Size Scaling (Metronome Tick Only)",
		description = "Disables font size scaling for metronome tick number",
		section = TickNumberSettings
	)
	default boolean disableFontScaling()
	{
		return false;
	}

	@ConfigItem(
		position = 4,
		keyName = "overheadUseCurrentColor",
		name = "Use Metronome Color for Overhead",
		description = "Uses the metronome color for the overhead tick color instead of Tick Number Color",
		section = TickNumberSettings
	)
	default boolean overheadUseCurrentColor()
	{
		return false;
	}

	@Range(
		min = 8,
		max = 50
	)
	@ConfigItem(
		position = 5,
		keyName = "fontSize",
		name = "Font Size (Overhead Tick Only)",
		description = "Change the font size of the overhead Tick Number",
		section = TickNumberSettings
	)
	default int fontSize()
	{
		return 15;
	}

	@ConfigItem(
		position = 6,
		keyName = "countColor",
		name = "Tick Number Color",
		description = "Configures the color of tick number",
		section = TickNumberSettings
	)
	default Color NumberColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		position = 7,
		keyName = "fontType",
		name = "Font Type",
		description = "Change the font of the Tick Number",
		section = TickNumberSettings
	)
	default FontTypes fontType()
	{
		return FontTypes.REGULAR;
	}

	@ConfigSection(
		name = "Metronome: True Tile Overlay Settings",
		description = "Settings only applied to True Tile Overlay",
		position = 47,
		closedByDefault = true
	)
	String TileSettings = "True Tile Overlay Settings";

	@ConfigItem(
		position = 1,
		keyName = "highlightCurrentTile",
		name = "Enable True Tile Overlay",
		description = "Highlights true player tile using the metronome colors (replacement for tile indicator plugin setting)",
		section = TileSettings
	)
	default boolean highlightCurrentTile()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		position = 2,
		keyName = "currentTileFillColor",
		name = "True Tile Fill Color",
		description = "Fill color of the true tile overlay",
		section = TileSettings
	)
	default Color currentTileFillColor()
	{
		return new Color(0, 0, 0, 50);
	}

	@ConfigItem(
		position = 3,
		keyName = "currentTileBorderWidth",
		name = "True Tile Border Width",
		description = "Border size of the true tile overlay",
		section = TileSettings
	)
	default double currentTileBorderWidth()
	{
		return 2;
	}

	@ConfigItem(
		position = 4,
		keyName = "changeFillColor",
		name = "Enable Tile Fill Color Metronome",
		description = "Makes the tile fill color change with the metronome",
		section = TileSettings
	)
	default boolean changeFillColor()
	{
		return false;
	}

	@Range(
		min = 0,
		max = 255
	)
	@ConfigItem(
		position = 5,
		keyName = "changeFillColorOpacity",
		name = "Fill Color Metronome Opacity",
		description = "Opacity of the tile fill metronome color if the option above is enabled. Otherwise, the opacity is determined by the True Tile Fill Color setting",
		section = TileSettings
	)
	default int changeFillColorOpacity()
	{
		return 50;
	}

	@ConfigItem(
		position = 6,
		keyName = "currentTileCornersOnly",
		name = "Corners only",
		description = "Draw only the corners of the current tile.",
		section = TileSettings
	)
	default boolean currentTileCornersOnly()
	{
		return false;
	}

	@ConfigItem(
		position = 7,
		keyName = "trueTileFadeout",
		name = "Fadeout",
		description = "Fade out the true tile once the player stops moving.",
		section = TileSettings
	)
	default boolean trueTileFadeout()
	{
		return false;
	}

	@ConfigItem(
		position = 8,
		keyName = "trueTileFadeoutTime",
		name = "Fadeout time",
		description = "Milliseconds that the true tile is drawn for after moving. Minimum is 600 (1 game tick).",
		section = TileSettings
	)
	@Range(min = 600)
	@Units(Units.MILLISECONDS)
	default int trueTileFadeoutTime()
	{
		return 800;
	}


	@ConfigSection(
		name = "Metronome: Color Settings",
		description = "Change the colors and number of colors to cycle through",
		position = 45,
		closedByDefault = true
	)
	String ColorSettings = "Color Settings";


	@Range(
		min = 2,
		max = 10
	)
	@ConfigItem(
		position = 1,
		keyName = "colorCycle",
		name = "Number of Colors",
		description = "The number of colors it cycles through",
		section = ColorSettings
	)
	default int colorCycle()
	{
		return 2;
	}

	@Alpha
	@ConfigItem(
		position = 2,
		keyName = "tickColor",
		name = "Tick Color",
		description = "Configures the color of tick",
		section = ColorSettings
	)
	default Color getTickColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		position = 3,
		keyName = "tockColor",
		name = "Tock Color",
		description = "Configures the color of tock",
		section = ColorSettings
	)
	default Color getTockColor()
	{
		return Color.GRAY;
	}

	@Alpha
	@ConfigItem(
		position = 4,
		keyName = "tick3Color",
		name = "3rd Tick Color",
		description = "Configures the color of 3rd tick if enabled",
		section = ColorSettings
	)
	default Color getTick3Color()
	{
		return Color.DARK_GRAY;
	}
	@Alpha
	@ConfigItem(
		position = 5,
		keyName = "tick4Color",
		name = "4th Tick Color",
		description = "Configures the color of the 4th tick if enabled",
		section = ColorSettings
	)
	default Color getTick4Color()
	{
		return Color.BLACK;
	}
	@Alpha
	@ConfigItem(
		position = 6,
		keyName = "tick5Color",
		name = "5th Tick Color",
		description = "Configures the color of the 5th tick if enabled",
		section = ColorSettings
	)
	default Color getTick5Color()
	{
		return new Color(112, 131, 255);
	}
	@Alpha
	@ConfigItem(
		position = 7,
		keyName = "tick6Color",
		name = "6th Tick Color",
		description = "Configures the color of the 6th tick if enabled",
		section = ColorSettings
	)
	default Color getTick6Color()
	{
		return new Color(0, 23, 171);
	}
	@Alpha
	@ConfigItem(
		position = 8,
		keyName = "tick7Color",
		name = "7th Tick Color",
		description = "Configures the color of the 7th tick if enabled",
		section = ColorSettings
	)
	default Color getTick7Color()
	{
		return new Color(107, 255, 124);
	}
	@Alpha
	@ConfigItem(
		position = 9,
		keyName = "tick8Color",
		name = "8th Tick Color",
		description = "Configures the color of the 8th tick if enabled",
		section = ColorSettings
	)
	default Color getTick8Color()
	{
		return new Color(0, 191, 22);
	}
	@Alpha
	@ConfigItem(
		position = 10,
		keyName = "tick9Color",
		name = "9th Tick Color",
		description = "Configures the color of the 9th tick if enabled",
		section = ColorSettings
	)
	default Color getTick9Color()
	{
		return new Color(255, 105, 94);
	}
	@Alpha
	@ConfigItem(
		position = 11,
		keyName = "tick10Color",
		name = "10th Tick Color",
		description = "Configures the color of the 10th tick if enabled",
		section = ColorSettings
	)
	default Color getTick10Color()
	{
		return new Color(255, 17, 0);
	}

	@ConfigSection(
		name = "Metronome: Hotkey Settings",
		description = "Settings that use hotkeys",
		position = 49,
		closedByDefault = true
	)
	String HotkeySettings = "Hotkey Settings";

	@ConfigItem(
		position = 1,
		keyName = "tickResetHotkey",
		name = "Tick Cycle Reset Hotkey",
		description = "Hotkey to reset the tick cycle back to 0",
		section = HotkeySettings
	)
	default Keybind tickResetHotkey()
	{
		return Keybind.NOT_SET;
	}

	@Range(
		min = 0
	)
	@ConfigItem(
		position = 2,
		keyName = "tickResetStartTick",
		name = "Reset to Tick",
		description = "Choose which tick the hotkey resets the timer to",
		section = HotkeySettings
	)
	default int tickResetStartTick()
	{
		return 0;
	}

	@ConfigItem(
		position = 3,
		keyName = "showHideHotkey",
		name = "Show/Hide Metronome Hotkey",
		description = "Hotkey to toggle whether the metronome is shown or hidden",
		section = HotkeySettings
	)
	default Keybind showHideHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		position = 4,
		keyName = "increaseTickCountHotkey",
		name = "Increase Tick Count Hotkey",
		description = "Hotkey to increase the Tick Count setting by 1",
		section = HotkeySettings
	)
	default Keybind increaseTickCountHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		position = 5,
		keyName = "decreaseTickCountHotkey",
		name = "Decrease Tick Count Hotkey",
		description = "Hotkey to decrease the Tick Count setting by 1",
		section = HotkeySettings
	)
	default Keybind decreaseTickCountHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "metronomeHidden",
		name = "",
		description = "",
		hidden = true
	)
	default boolean metronomeHidden()
	{
		return false;
	}
	@ConfigSection(
		name = "Metronome: Mouse Following Settings",
		description = "Settings for mouse-following tick counter",
		position = 50,
		closedByDefault = true
	)
	String mouseFollowingSettings = "Mouse Following Settings";
	@ConfigItem(
		position = 0,
		keyName = "mouseFollowingTick",
		name = "Tick Counter Follows Mouse",
		description = "Makes the tick counter follow your mouse cursor. It uses the same settings as the Overhead Tick Number",
		section = mouseFollowingSettings
	)
	default boolean mouseFollowingTick()
	{
		return false;
	}

	@Range(
		min = -100,
		max = 100
	)
	@ConfigItem(
		position = 1,
		keyName = "mouseOffsetX",
		name = "Mouse X Offset",
		description = "X offset from mouse cursor for tick counter",
		section = mouseFollowingSettings
	)
	default int mouseOffsetX()
	{
		return 10;
	}

	@Range(
		min = -100,
		max = 100
	)
	@ConfigItem(
		position = 2,
		keyName = "mouseOffsetY",
		name = "Mouse Y Offset",
		description = "Y offset from mouse cursor for tick counter",
		section = mouseFollowingSettings
	)
	default int mouseOffsetY()
	{
		return -10;
	}

	@ConfigSection(
		name = "Metronome: Additional Cycle Settings",
		description = "Configure additional independent tick cycles and where they are displayed",
		position = 51,
		closedByDefault = true
	)
	String additionalOverheadSettings = "Additional Overhead Tick Settings";

	@ConfigItem(
		position = 1,
		keyName = "showSecondCycle",
		name = "Enable Cycle 2",
		description = "Activates the second tick counter",
		section = additionalOverheadSettings
	)
	default boolean enableCycle2()
	{
		return false;
	}

	@Range(min = 2)
	@ConfigItem(
		position = 2,
		keyName = "tickCount2",
		name = "Cycle 2 Length",
		description = "Length of the second cycle in ticks",
		section = additionalOverheadSettings
	)
	default int tickCount2()
	{
		return 2;
	}

	@Alpha
	@ConfigItem(
		position = 3,
		keyName = "cycle2Color",
		name = "Cycle 2 Color",
		description = "Color used for Cycle 2 in both overhead and overlay",
		section = additionalOverheadSettings
	)
	default Color cycle2Color()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		position = 4,
		keyName = "showCycle2Overhead",
		name = "Show Cycle 2 in Overhead",
		description = "Displays Cycle 2 tick number above the player",
		section = additionalOverheadSettings
	)
	default boolean showCycle2Overhead()
	{
		return false;
	}

	@ConfigItem(
		position = 5,
		keyName = "showCycle2Overlay",
		name = "Show Cycle 2 in Overlay",
		description = "Adds a Cycle 2 box to the metronome overlay",
		section = additionalOverheadSettings
	)
	default boolean showCycle2Overlay()
	{
		return false;
	}

	@ConfigItem(
		position = 6,
		keyName = "showThirdCycle",
		name = "Enable Cycle 3",
		description = "Activates the third tick counter",
		section = additionalOverheadSettings
	)
	default boolean enableCycle3()
	{
		return false;
	}

	@Range(min = 2)
	@ConfigItem(
		position = 7,
		keyName = "tickCount3",
		name = "Cycle 3 Length",
		description = "Length of the third cycle in ticks",
		section = additionalOverheadSettings
	)
	default int tickCount3()
	{
		return 2;
	}

	@Alpha
	@ConfigItem(
		position = 8,
		keyName = "cycle3Color",
		name = "Cycle 3 Color",
		description = "Color used for Cycle 3 in both overhead and overlay",
		section = additionalOverheadSettings
	)
	default Color cycle3Color()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		position = 9,
		keyName = "showCycle3Overhead",
		name = "Show Cycle 3 in Overhead",
		description = "Displays Cycle 3 tick number above the player",
		section = additionalOverheadSettings
	)
	default boolean showCycle3Overhead()
	{
		return false;
	}

	@ConfigItem(
		position = 10,
		keyName = "showCycle3Overlay",
		name = "Show Cycle 3 in Overlay",
		description = "Adds a Cycle 3 box to the metronome overlay",
		section = additionalOverheadSettings
	)
	default boolean showCycle3Overlay()
	{
		return false;
	}

	@ConfigItem(
		position = 11,
		keyName = "overheadCyclesGapDistance",
		name = "Overhead Cycle Gap Distance",
		description = "Configures the distance of the gap between the overhead ticks",
		section = additionalOverheadSettings
	)
	default int overheadCyclesGapDistance()
	{
		return 20;
	}

	@Range(
		min = -500,
		max = 500
	)
	@ConfigItem(
		position = 12,
		keyName = "overheadHeight",
		name = "Overhead Tick Height",
		description = "Configures the height of overhead ticks",
		section = additionalOverheadSettings
	)
	default int overheadHeight()
	{
		return 20;
	}

	@Range(
		min = -50,
		max = 50
	)
	@ConfigItem(
		position = 13,
		keyName = "overheadXCenterOffset",
		name = "Overhead Tick X Center Offset",
		description = "Configures the X offset of overhead ticks",
		section = additionalOverheadSettings
	)
	default int overheadXCenterOffset()
	{
		return 0;
	}
}
