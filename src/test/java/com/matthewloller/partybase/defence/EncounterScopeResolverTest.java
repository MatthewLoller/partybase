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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.party.PartyService;
import org.junit.Test;

public class EncounterScopeResolverTest
{
	@Test
	public void healthPercentageClampsKnownValuesAndPreservesUnknownValues()
	{
		NPC npc = mock(NPC.class);
		when(npc.getHealthRatio()).thenReturn(3);
		when(npc.getHealthScale()).thenReturn(4);
		assertEquals(75, EncounterScopeResolver.healthPercent(npc));

		when(npc.getHealthRatio()).thenReturn(-1);
		assertEquals(-1, EncounterScopeResolver.healthPercent(npc));
		assertEquals(-1, EncounterScopeResolver.healthPercent(null));
	}

	@Test
	public void healthToleranceMatchesOnlyWhenBothClientsKnowTheValue()
	{
		NPC npc = mock(NPC.class);
		when(npc.getHealthRatio()).thenReturn(50);
		when(npc.getHealthScale()).thenReturn(100);

		assertTrue(EncounterScopeResolver.healthMatches(npc, 70));
		assertFalse(EncounterScopeResolver.healthMatches(npc, 71));
		assertTrue(EncounterScopeResolver.healthMatches(npc, -1));

		when(npc.getHealthRatio()).thenReturn(-1);
		assertTrue(EncounterScopeResolver.healthMatches(npc, 20));
	}

	@Test
	public void missingRaidControllerUsesInstanceForTransportButNotRaidLifecycle()
	{
		Client client = mock(Client.class);
		WorldView world = mock(WorldView.class);
		when(client.getTopLevelWorldView()).thenReturn(world);
		when(world.isInstance()).thenReturn(true);
		when(world.getBaseX()).thenReturn(1000);
		when(world.getBaseY()).thenReturn(2000);
		EncounterScopeResolver resolver = new EncounterScopeResolver(client, mock(PartyService.class));
		int fingerprint = EncounterScopes.instanceFingerprint(world);

		assertEquals(new SyncScope(PartybaseDefenceSync.SCOPE_INSTANCE, fingerprint),
			resolver.forBoss(BossDefence.TEKTON, null));
		assertNull(resolver.currentRaidScopeForBoss(BossDefence.TEKTON));
		assertTrue(resolver.scopeMatches(PartybaseDefenceSync.SCOPE_INSTANCE, fingerprint, BossDefence.TEKTON));
		assertFalse(resolver.scopeMatches(PartybaseDefenceSync.SCOPE_INSTANCE, fingerprint + 1, BossDefence.TEKTON));

		when(client.getVarpValue(VarPlayerID.RAIDS_PARTY_GROUPHOLDER)).thenReturn(42);
		assertEquals(new SyncScope(PartybaseDefenceSync.SCOPE_COX, 42),
			resolver.forBoss(BossDefence.TEKTON, null));
		assertEquals(new SyncScope(PartybaseDefenceSync.SCOPE_COX, 42),
			resolver.currentRaidScopeForBoss(BossDefence.TEKTON));
	}
}
