/*
 * Copyright (c) 2026, Heisty <https://github.com/Falsary>
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
package com.matthewloller.partybase.defence;

import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.defence.DefenceTracker.DefenceState;
import com.matthewloller.partybase.defence.DefenceTracker.SpecHistoryEntry;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import net.runelite.client.ui.overlay.infobox.InfoBox;

// Status-bar Defence readout with optional spec history.
public class DefenceInfoBox extends InfoBox
{
	public enum Stat
	{
		DEFENCE, MAGIC_DEFENCE
	}

	private static final String DEFAULT_TOOLTIP = "Partybase defence";

	private final DefenceTracker tracker;
	private final PartybaseConfig config;
	private final Stat stat;

	// Reuse the tracker's immutable history snapshot.
	private List<SpecHistoryEntry> tooltipHistory;

	public DefenceInfoBox(BufferedImage image, Plugin plugin, DefenceTracker tracker, PartybaseConfig config)
	{
		this(image, plugin, tracker, config, Stat.DEFENCE);
	}

	public DefenceInfoBox(
		BufferedImage image,
		Plugin plugin,
		DefenceTracker tracker,
		PartybaseConfig config,
		Stat stat)
	{
		super(image, plugin);
		this.tracker = tracker;
		this.config = config;
		this.stat = stat;
		setTooltip(DEFAULT_TOOLTIP);
	}

	@Override
	public String getText()
	{
		DefenceState state = tracker.state();
		if (state == null)
		{
			return "";
		}
		// Prevent a stale TFA Magic frame.
		if (stat == Stat.MAGIC_DEFENCE && !DefenceReadout.magicDefenceVisible(state))
		{
			return "";
		}
		updateTooltip(state);
		if (stat == Stat.MAGIC_DEFENCE)
		{
			return magicText(state);
		}

		// Share the overlay's defence-level mode.
		boolean full = config.defenceShowFullLevel();
		long current = DefenceReadout.shownDefence(state, full);
		long base = DefenceReadout.shownBaseDefence(state, full);
		return infoBoxValue(current, base);
	}

	private String magicText(DefenceState state)
	{
		long rollPercent = DefenceReadout.percentRemaining(state.getMagicRoll(), state.getMagicBaseRoll());
		switch (config.magicDefenceDisplay())
		{
			case LEVEL :
				return infoBoxValue(state.getMagicLevel(), state.getMagicBaseLevel());
			case PERCENT :
				return rollPercent + "%";
			case BOTH :
				return state.getMagicDef() + " " + rollPercent + "%";
			case BONUS :
			default :
				return infoBoxValue(state.getMagicDef(), state.getMagicBaseDef());
		}
	}

	private String infoBoxValue(long current, long base)
	{
		switch (config.defenceInfoBoxValue())
		{
			case PERCENT :
				return DefenceReadout.percentRemaining(current, base) + "%";
			case DRAINED :
				return Long.toString(Math.max(0, base - current));
			case CURRENT :
			default :
				return Long.toString(current);
		}
	}

	// Tooltip contains accepted spec history only.
	private void updateTooltip(DefenceState state)
	{
		if (!config.extraInfoInInfoBox())
		{
			if (tooltipHistory != null)
			{
				tooltipHistory = null;
				setTooltip(DEFAULT_TOOLTIP);
			}
			return;
		}

		List<SpecHistoryEntry> history = tracker.specHistoryForCurrentTarget(state.getNpcIndex());
		if (history == tooltipHistory)
		{
			return;
		}
		tooltipHistory = history;
		List<SpecHistoryEntry> relevant = relevantHistory(history);
		if (relevant.isEmpty())
		{
			setTooltip("No specs yet");
			return;
		}

		StringBuilder tip = new StringBuilder("Spec history:");
		for (SpecSummary summary : aggregateHistory(relevant))
		{
			tip.append("<br>");
			if (summary.miss)
			{
				tip.append("<col=ff0000>");
			}
			tip.append(summary.playerName)
			.append(": ")
			.append(shortWeaponName(summary.weapon));
			if (summary.miss)
			{
				tip.append(" miss: ")
				.append(summary.value)
				.append("</col>");
			}
			else
			{
				tip.append(' ').append(summary.value);
			}
		}
		setTooltip(tip.toString());
	}

	private List<SpecHistoryEntry> relevantHistory(List<SpecHistoryEntry> history)
	{
		List<SpecHistoryEntry> relevant = new ArrayList<>();
		for (SpecHistoryEntry entry : history)
		{
			if (entry == null)
			{
				continue;
			}
			boolean include = stat == Stat.MAGIC_DEFENCE
				? SpecialWeaponRules.drainsMagicDefence(entry.getWeapon())
				: SpecialWeaponRules.drainsDefence(entry.getWeapon());
			if (include)
			{
				relevant.add(entry);
			}
		}
		return relevant;
	}

	// Fixed-effect specs count uses; BGS counts damage.
	// Keep misses separate and preserve first-seen order.
	private static List<SpecSummary> aggregateHistory(List<SpecHistoryEntry> history)
	{
		List<SpecSummary> summaries = new ArrayList<>();
		for (SpecHistoryEntry entry : history)
		{
			if (entry == null)
			{
				continue;
			}
			SpecialWeapon weapon = entry.getWeapon();
			String player = entry.getPlayerName();
			if (player == null || player.trim().isEmpty())
			{
				player = "Unknown";
			}
			boolean miss = entry.getHit() <= 0;
			SpecSummary existing = null;
			for (SpecSummary summary : summaries)
			{
				if (summary.weapon == weapon && summary.miss == miss && summary.playerName.equals(player))
				{
					existing = summary;
					break;
				}
			}

			// Misses count once and never inflate hit totals.
			int amount = miss ? 1 : SpecialWeaponRules.reportedValue(weapon, entry.getHit());
			if (existing == null)
			{
				summaries.add(new SpecSummary(player, weapon, amount, miss));
			}
			else
			{
				existing.value += amount;
			}
		}
		return summaries;
	}

	private static final class SpecSummary
	{
		private final String playerName;
		private final SpecialWeapon weapon;
		private final boolean miss;
		private int value;

		private SpecSummary(String playerName, SpecialWeapon weapon, int value, boolean miss)
		{
			this.playerName = playerName;
			this.weapon = weapon;
			this.value = value;
			this.miss = miss;
		}
	}

	private static String shortWeaponName(SpecialWeapon weapon)
	{
		return SpecialWeaponRules.shortName(weapon);
	}

	@Override
	public Color getTextColor()
	{
		if (stat == Stat.MAGIC_DEFENCE)
		{
			return config.magicDefenceColor();
		}
		DefenceState state = tracker.state();
		return state == null ? Color.WHITE : DefenceReadout.defenceColor(state, config);
	}
}
