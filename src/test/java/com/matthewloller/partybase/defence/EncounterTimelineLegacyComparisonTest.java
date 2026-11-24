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

import static com.matthewloller.partybase.defence.EncounterTimeline.HistoryRelation.*;
import static org.junit.Assert.*;
import com.matthewloller.partybase.defence.DefenceTracker.SpecHistoryEntry;
import com.matthewloller.partybase.defence.DefenceTracker.SyncState;
import java.util.Arrays;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

public class EncounterTimelineLegacyComparisonTest
{
	private final SpecHistoryEntry a = new SpecHistoryEntry("Alice", SpecialWeapon.DRAGON_WARHAMMER, 1);
	private final SpecHistoryEntry b = new SpecHistoryEntry("Bob", SpecialWeapon.BANDOS_GODSWORD, 50);
	private final SpecHistoryEntry c = new SpecHistoryEntry("Carol", SpecialWeapon.BANDOS_GODSWORD, 20);

	private SyncState state(long defence, SpecHistoryEntry... events)
	{
		return new SyncState("Chaos Elemental", BossDefence.CHAOS_ELEMENTAL, defence, 0, 270,
				100, 100, 100, 100, 0, 0, false, false, false, true, Arrays.asList(events));
	}
	@Test
	public void reversedTeamOrderIsConflictInBothDirections()
	{
		assertEquals(CONFLICT, EncounterTimeline.compareLegacy(state(139, a, b), state(154, b, a)));
		assertEquals(CONFLICT, EncounterTimeline.compareLegacy(state(154, b, a), state(139, a, b)));
	}
	@Test
	public void sharedPrefixWithDifferentSuffixesCannotBeAppendedInArrivalOrder()
	{
		assertEquals(CONFLICT, EncounterTimeline.compareLegacy(state(139, a, b), state(169, a, c)));
		assertEquals(CONFLICT, EncounterTimeline.compareLegacy(state(169, a, c), state(139, a, b)));
	}
	@Test
	public void longerHistoryIsNotProofWhenItOmitsALocalSpec()
	{
		assertEquals(CONFLICT, EncounterTimeline.compareLegacy(state(220, b), state(169, a, c)));
	}
	@Test
	public void realExtensionSupportsCatchupAndRejectsRollback()
	{
		assertEquals(INITIAL, EncounterTimeline.compareLegacy(null, state(139, a, b)));
		assertEquals(EXTENSION, EncounterTimeline.compareLegacy(state(189, a), state(139, a, b)));
		assertEquals(STALE, EncounterTimeline.compareLegacy(state(139, a, b), state(189, a)));
		assertEquals(SAME, EncounterTimeline.compareLegacy(state(139, a, b), state(139, a, b)));
	}
	@Test
	public void sameHistoryButDifferentValuesIsAlsoAConflict()
	{
		assertEquals(CONFLICT, EncounterTimeline.compareLegacy(state(139, a, b), state(154, a, b)));
	}
}
