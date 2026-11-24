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
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

public class ToaEncounterCoordinatorTest
{
	@Test
	public void fullPartyWipeResetsRememberedBossOncePerWipe()
	{
		Client client = mock(Client.class);
		WorldView world = mock(WorldView.class);
		DefenceTracker tracker = mock(DefenceTracker.class);
		when(client.getTopLevelWorldView()).thenReturn(world);
		when(world.isInstance()).thenReturn(true);
		when(client.getVarbitValue(VarbitID.TOA_CLIENT_P0)).thenReturn(30);
		when(client.getVarbitValue(VarbitID.TOA_CLIENT_P1)).thenReturn(30);
		when(tracker.hasRememberedState(BossDefence.BA_BA)).thenReturn(true);

		List<BossDefence> resets = new ArrayList<>();
		ToaEncounterCoordinator coordinator =
			new ToaEncounterCoordinator(client, tracker, resets::add);
		coordinator.onGameTick();
		coordinator.onGameTick();
		assertEquals(List.of(BossDefence.BA_BA), resets);

		when(client.getVarbitValue(VarbitID.TOA_CLIENT_P1)).thenReturn(20);
		coordinator.onGameTick();
		when(client.getVarbitValue(VarbitID.TOA_CLIENT_P1)).thenReturn(30);
		coordinator.onGameTick();
		assertEquals(List.of(BossDefence.BA_BA, BossDefence.BA_BA), resets);
	}
}
