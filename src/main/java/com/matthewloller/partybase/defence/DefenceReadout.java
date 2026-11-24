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
import com.matthewloller.partybase.PartybaseConfig.DefenceDrainFormat;
import com.matthewloller.partybase.PartybaseConfig.DefenceThresholdUnit;
import com.matthewloller.partybase.PartybaseConfig.DefenceValueFormat;
import com.matthewloller.partybase.defence.DefenceTracker.DefenceState;
import java.awt.Color;

// Shared Defence text, colour, and threshold formatting.
final class DefenceReadout
{
	private static final int PERCENT = 100;

	private DefenceReadout()
	{
	}

	static String value(DefenceValueFormat format, long current, long base)
	{
		switch (format)
		{
			case CURRENT_OF_BASE :
				return current + "/" + base;
			case PERCENT :
				return percentRemaining(current, base) + "%";
			case CURRENT_AND_PERCENT :
				return current + " (" + percentRemaining(current, base) + "%)";
			case CURRENT :
			default :
				return Long.toString(current);
		}
	}

	// Return the drain suffix, or null when hidden or unchanged.
	static String drain(DefenceDrainFormat format, long current, long base)
	{
		long drained = base - current;
		if (drained <= 0 || format == DefenceDrainFormat.OFF)
		{
			return null;
		}
		return format == DefenceDrainFormat.PERCENT
			? percent(drained, base) + "%"
			: Long.toString(drained);
	}

	// Empty base values count as untouched.
	static long percentRemaining(long current, long base)
	{
		return base > 0 ? percent(Math.max(0, current), base) : PERCENT;
	}

	// Display full level or remaining defence above the floor.
	static long shownDefence(DefenceState state, boolean fullLevel)
	{
		return Math.max(0, fullLevel ? state.getCurrent() : state.getCurrent() - state.getMin());
	}

	static long shownBaseDefence(DefenceState state, boolean fullLevel)
	{
		return Math.max(0, fullLevel ? state.getBase() : state.getBase() - state.getMin());
	}

	// A target at its floor is fully drained.
	static boolean isFullyDrained(DefenceState state)
	{
		return state.getCurrent() <= state.getMin();
	}

	// Hide Magic Defence in TFA.
	static boolean magicDefenceVisible(DefenceState state)
	{
		return state != null && !state.isTfaBoss();
	}

	// Use the same low-defence threshold for overlay and info box.
	static boolean isLowDefence(DefenceState state, PartybaseConfig config)
	{
		long aboveFloor = Math.max(state.getCurrent() - state.getMin(), 0);
		long threshold = config.defenceLowThresholdUnit() == DefenceThresholdUnit.PERCENT
			? (state.getBase() - state.getMin()) * Math.min(PERCENT, config.defenceLowThreshold()) / PERCENT
			: config.defenceLowThreshold();
		return aboveFloor > 0 && aboveFloor <= threshold;
	}

	static Color defenceColor(DefenceState state, PartybaseConfig config)
	{
		if (isFullyDrained(state))
		{
			return config.defenceCappedColor();
		}
		return isLowDefence(state, config) ? config.defenceLowColor() : config.defenceHighColor();
	}

	private static long percent(long part, long whole)
	{
		return Math.round(part * 100.0 / whole);
	}
}
