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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.matthewloller.partybase.defence.DefenceTracker.SpecHistoryEntry;
import com.matthewloller.partybase.defence.DefenceTracker.SpellDrain;
import com.matthewloller.partybase.party.PartyLink;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/**
 * Regression coverage for the three review points on the generalised encounter handling: independently clocked
 * ordering, the no-op merge predicate, and target capture.
 */
public class OrderingAndTargetCaptureRegressionTest
{
	private static SpecHistoryEntry event(String id, String anchor, long tick,
		SpecialWeapon weapon, int hit, long observedAtMillis)
	{
		return new SpecHistoryEntry("Alice", weapon, hit, id, anchor, tick)
			.withObservedAtMillis(observedAtMillis);
	}

	/** DWH is order-sensitive against BGS: DWH first drains more in total. */
	private static EncounterTimeline.Replay<Integer> defenceReplay()
	{
		return new EncounterTimeline.Replay<Integer>()
		{
			@Override
			public Integer apply(Integer defence, SpecHistoryEntry entry)
			{
				if (entry.getWeapon() == SpecialWeapon.DRAGON_WARHAMMER)
				{
					return defence - defence * 30 / 100;
				}
				return Math.max(0, defence - entry.getHit());
			}

			@Override
			public Integer conservative(Integer a, Integer b)
			{
				return Math.max(a, b);
			}
		};
	}

	private static int replay(SpecHistoryEntry... events)
	{
		return EncounterTimeline.replay(Arrays.asList(events), 310, defenceReplay());
	}

	// 1. Independent clocks: order by the event's own millisecond stamp, with no minimum gap.
	@Test
	public void independentClocksOrderByMillisecondsWithoutAMinimumGap()
	{
		SpecHistoryEntry warhammer = event("dwh", "open:a", 10,
				SpecialWeapon.DRAGON_WARHAMMER, 1, 1_000L);
		SpecHistoryEntry godsword = event("bgs", "open:b", 20,
				SpecialWeapon.BANDOS_GODSWORD, 30, 1_100L);

		// Ordered: 310 -> 217 -> 187. Unordered would take the larger of both orders, 196.
		assertEquals(187, replay(warhammer, godsword));
		assertEquals(187, replay(godsword, warhammer));
	}

	@Test
	public void independentClocksOneMillisecondApartAreStillOrdered()
	{
		SpecHistoryEntry warhammer = event("dwh", "open:a", 10,
				SpecialWeapon.DRAGON_WARHAMMER, 1, 1_000L);
		SpecHistoryEntry godsword = event("bgs", "open:b", 20,
				SpecialWeapon.BANDOS_GODSWORD, 30, 1_001L);

		assertEquals(187, replay(warhammer, godsword));
	}

	@Test
	public void equalMillisecondStampsStayUnordered()
	{
		SpecHistoryEntry warhammer = event("dwh", "open:a", 10,
				SpecialWeapon.DRAGON_WARHAMMER, 1, 1_000L);
		SpecHistoryEntry godsword = event("bgs", "open:b", 20,
				SpecialWeapon.BANDOS_GODSWORD, 30, 1_000L);

		// No evidence of which came first, so both orders are legal: conservative maximum.
		assertEquals(196, replay(warhammer, godsword));
	}

	// 2. Shared raid ticks keep tick ordering, and the stamps cannot override it.
	@Test
	public void sharedAnchorKeepsTickOrderingEvenWhenStampsDisagree()
	{
		SpecHistoryEntry warhammer = event("dwh", "raid-time:v1:toa:i1:u2", 10,
				SpecialWeapon.DRAGON_WARHAMMER, 1, 9_000L);
		SpecHistoryEntry godsword = event("bgs", "raid-time:v1:toa:i1:u2", 20,
				SpecialWeapon.BANDOS_GODSWORD, 30, 1_000L);

		assertEquals(187, replay(warhammer, godsword));
		assertEquals(187, replay(godsword, warhammer));
	}

	@Test
	public void sameTickSameAnchorStaysUnordered()
	{
		SpecHistoryEntry first = event("dwh1", "raid-time:v1:toa:i1:u2", 10,
				SpecialWeapon.DRAGON_WARHAMMER, 1, 1_000L);
		SpecHistoryEntry second = event("dwh2", "raid-time:v1:toa:i1:u2", 10,
				SpecialWeapon.DRAGON_WARHAMMER, 1, 1_050L);

		// Same shared tick: the two stamps are sampling noise, so the reduction stays exact.
		assertEquals(310 - 310 * 30 / 100 - (310 - 310 * 30 / 100) * 30 / 100,
			replay(first, second));
	}

	// 3. The no-op merge predicate must follow each weapon's drain formula.
	@Test
	public void zeroHitWarhammerThatStillDrainsIsNotANoOp()
	{
		NPC npc = mock(NPC.class);
		SpecialWeapon weapon = mock(SpecialWeapon.class);
		// RuneLite reports Tekton's guaranteed-on-miss drain as a positive percentage.
		when(weapon.computeDrainPercent(0, npc)).thenReturn(5.0f);

		SpecHistoryEntry miss = new SpecHistoryEntry("Alice", weapon, 0, "e1", "open", 1)
		.withObservedAtMillis(1_000L);

		assertFalse(DefenceTracker.onlyNoOpEvents(Collections.singletonList(miss), npc));
	}

	@Test
	public void zeroHitWithNoDrainPercentageIsANoOp()
	{
		NPC npc = mock(NPC.class);
		SpecialWeapon weapon = mock(SpecialWeapon.class);
		when(weapon.computeDrainPercent(0, npc)).thenReturn(0.0f);

		SpecHistoryEntry miss = new SpecHistoryEntry("Alice", weapon, 0, "e1", "open", 1)
		.withObservedAtMillis(1_000L);

		assertTrue(DefenceTracker.onlyNoOpEvents(Collections.singletonList(miss), npc));
	}

	@Test
	public void undatedRowsAndSpellsAreNeverNoOps()
	{
		NPC npc = mock(NPC.class);
		SpecialWeapon weapon = mock(SpecialWeapon.class);
		when(weapon.computeDrainPercent(0, npc)).thenReturn(0.0f);

		SpecHistoryEntry undated = new SpecHistoryEntry("Alice", weapon, 0, "e1", "open", 1);
		assertFalse(DefenceTracker.onlyNoOpEvents(Collections.singletonList(undated), npc));

		SpecHistoryEntry spell = new SpecHistoryEntry("Alice", null, SpellDrain.VULNERABILITY,
			1, "e2", "open", 1).withObservedAtMillis(1_000L);
		assertFalse(DefenceTracker.onlyNoOpEvents(Collections.singletonList(spell), npc));
	}

	// 4. Target capture: a target change after the spec must not steal it.
	@Test
	public void capturedTargetWinsWhenTheInteractionChanged()
	{
		Client client = mock(Client.class);
		Player local = mock(Player.class);
		NPC captured = mock(NPC.class);
		NPC newTarget = mock(NPC.class);
		when(captured.getId()).thenReturn(1_234);
		when(captured.getIndex()).thenReturn(100);
		when(captured.getName()).thenReturn("Chaos Elemental");
		when(newTarget.getId()).thenReturn(1_234);
		when(newTarget.getIndex()).thenReturn(101);
		when(newTarget.getName()).thenReturn("Chaos Elemental");
		when(client.getLocalPlayer()).thenReturn(local);
		when(local.getInteracting()).thenReturn(newTarget);

		LocalDefenceSpecDetector detector = new LocalDefenceSpecDetector(
			client, mock(ClientThread.class), mock(PartyLink.class),
			mock(PluginManager.class), mock(DefenceTracker.class));

		assertSame("The spec belongs to the actor it was cast on", captured,
			detector.resolveTarget(captured));
	}

	@Test
	public void sameTargetIsPreferredOverTheCapturedInstance()
	{
		Client client = mock(Client.class);
		Player local = mock(Player.class);
		NPC captured = mock(NPC.class);
		when(local.getInteracting()).thenReturn(captured);
		when(client.getLocalPlayer()).thenReturn(local);

		LocalDefenceSpecDetector detector = new LocalDefenceSpecDetector(
			client, mock(ClientThread.class), mock(PartyLink.class),
			mock(PluginManager.class), mock(DefenceTracker.class));

		assertSame(captured, detector.resolveTarget(captured));
	}

	@Test
	public void clearedInteractionWithNothingCapturedStaysUnresolved()
	{
		Client client = mock(Client.class);
		Player local = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(local);

		LocalDefenceSpecDetector detector = new LocalDefenceSpecDetector(
			client, mock(ClientThread.class), mock(PartyLink.class),
			mock(PluginManager.class), mock(DefenceTracker.class));

		NPC captured = mock(NPC.class);
		assertNull(detector.resolveTarget(null));
		assertNull(detector.resolveTarget(captured));
	}
}
