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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.PartybaseConfig.DefenceThresholdUnit;
import com.matthewloller.partybase.defence.DefenceTracker.DefenceState;
import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.plugins.Plugin;
import org.junit.Test;

/** The Defence infobox shares the overhead palette and thresholds. */
public class DefenceInfoBoxColorTest
{
	private static final Color HIGH = new Color(0xeeeeee);
	private static final Color LOW = new Color(0xcca233);
	private static final Color CAPPED = new Color(0x579549);
	private static final Color DRAIN_AMOUNT = new Color(0xcc0066);

	private PartybaseConfig config()
	{
		PartybaseConfig config = mock(PartybaseConfig.class);
		when(config.defenceHighColor()).thenReturn(HIGH);
		when(config.defenceLowColor()).thenReturn(LOW);
		when(config.defenceCappedColor()).thenReturn(CAPPED);
		when(config.defenceDrainColor()).thenReturn(DRAIN_AMOUNT);
		when(config.defenceLowThresholdUnit()).thenReturn(DefenceThresholdUnit.LEVELS);
		when(config.defenceLowThreshold()).thenReturn(10);
		return config;
	}

	private static DefenceState state(long current, long floor, long base)
	{
		return new DefenceState(24, "Test Boss", current, floor, base,
				25000, 25000, 100, 100, 290, 290, true, true);
	}

	private static void check(DefenceState state, PartybaseConfig config, Color expected)
	{
		DefenceTracker tracker = mock(DefenceTracker.class);
		when(tracker.state()).thenReturn(state);
		DefenceInfoBox box = new DefenceInfoBox(
			new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
			mock(Plugin.class), tracker, config);
		assertEquals("Defence infobox colour", expected, box.getTextColor());
		assertEquals("Defence overhead colour", expected, DefenceReadout.defenceColor(state, config));
		// Independently configured drain-amount colour must never drive this text.
		org.junit.Assert.assertNotEquals(DRAIN_AMOUNT, box.getTextColor());
	}

	@Test
	public void cappedBossUsesFullyDrainedColourEvenWithPositiveDefenceFloor()
	{
		check(state(156, 156, 260), config(), CAPPED); // Archive Guardian
		check(state(0, 0, 100), config(), CAPPED); // Dagon'hai monk
		check(state(150, 150, 250), config(), CAPPED); // Ocellus final phase
	}

	@Test
	public void lowDefenceUsesLowColourUntilTheFloorIsReached()
	{
		check(state(161, 156, 260), config(), LOW);
		check(state(166, 156, 260), config(), LOW);
	}

	@Test
	public void defenceAboveLowThresholdUsesNormalColour()
	{
		check(state(182, 156, 260), config(), HIGH);
	}

	@Test
	public void percentLowThresholdAlsoMatchesOverheadTransitions()
	{
		PartybaseConfig c = config();
		when(c.defenceLowThresholdUnit()).thenReturn(DefenceThresholdUnit.PERCENT);
		when(c.defenceLowThreshold()).thenReturn(10);
		// 10% of the 104 drainable Defence levels = 10 (integer arithmetic).
		check(state(167, 156, 260), c, HIGH);
		check(state(166, 156, 260), c, LOW);
		check(state(156, 156, 260), c, CAPPED);
	}

	@Test
	public void magicDefenceInfoboxColourIsUntouched()
	{
		PartybaseConfig c = config();
		Color magicColour = new Color(0x0088ee);
		when(c.magicDefenceColor()).thenReturn(magicColour);
		DefenceTracker tracker = mock(DefenceTracker.class);
		when(tracker.state()).thenReturn(state(156, 156, 260));
		DefenceInfoBox box = new DefenceInfoBox(
			new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
			mock(Plugin.class), tracker, c, DefenceInfoBox.Stat.MAGIC_DEFENCE);
		assertEquals(magicColour, box.getTextColor());
	}

	@Test
	public void noActiveTargetUsesWhiteFallback()
	{
		DefenceTracker tracker = mock(DefenceTracker.class);
		DefenceInfoBox box = new DefenceInfoBox(
			new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
			mock(Plugin.class), tracker, config());
		assertEquals(Color.WHITE, box.getTextColor());
	}
}
