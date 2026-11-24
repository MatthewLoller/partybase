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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class NpcTargetMatchingTest
{
	private final WorldView worldView = mock(WorldView.class);
	private final List<NPC> sceneNpcs = new ArrayList<>();

	@SuppressWarnings("unchecked")
	private void loadScene()
	{
		IndexedObjectSet<? extends NPC> npcs = mock(IndexedObjectSet.class);
		doReturn(npcs).when(worldView).npcs();
		when(npcs.iterator()).thenAnswer(invocation -> sceneNpcs.iterator());
	}

	private NPC npc(String name, int id, int index, WorldPoint location)
	{
		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn(name);
		when(npc.getId()).thenReturn(id);
		when(npc.getIndex()).thenReturn(index);
		when(npc.getWorldLocation()).thenReturn(location);
		when(npc.getHealthRatio()).thenReturn(1);
		sceneNpcs.add(npc);
		return npc;
	}

	@Test
	public void twoSameTypeActorsRemainAmbiguousWithoutNewEvidence()
	{
		loadScene();
		npc("Chaos Elemental", 1, 10, new WorldPoint(1000, 1000, 0));
		npc("Chaos Elemental", 2, 11, new WorldPoint(1001, 1000, 0));

		assertNull(TargetResolver.uniqueLiveNpc(worldView, BossDefence.CHAOS_ELEMENTAL));
	}

	@Test
	public void senderCoordinatesSelectOneSameTypeActor()
	{
		loadScene();
		NPC first = npc("Chaos Elemental", 1, 10, new WorldPoint(1000, 1000, 0));
		npc("Chaos Elemental", 2, 11, new WorldPoint(1020, 1020, 0));

		assertEquals(first, TargetResolver.uniqueLiveNpcNear(
			worldView, BossDefence.CHAOS_ELEMENTAL, 1000, 1000, 0, 2));
	}

	@Test
	public void rememberedNpcIdAndLocationSelectTheOriginalTarget()
	{
		loadScene();
		npc("Chaos Elemental", 1, 10, new WorldPoint(1000, 1000, 0));
		NPC second = npc("Chaos Elemental", 2, 11, new WorldPoint(1020, 1020, 0));

		assertEquals(second, TargetResolver.uniqueLiveNpcFor(
			worldView, BossDefence.CHAOS_ELEMENTAL, 2, new WorldPoint(1020, 1020, 0), 2));
	}

	@Test
	public void rememberedTargetDoesNotMatchActorWithoutCurrentTile()
	{
		loadScene();
		npc("Chaos Elemental", 1, 10, new WorldPoint(1000, 1000, 0));
		NPC replacement = npc("Chaos Elemental", 1, 10, null);

		assertFalse(TargetResolver.matches(
			replacement, BossDefence.CHAOS_ELEMENTAL, 1, new WorldPoint(1000, 1000, 0), 2));
	}

	@Test
	public void locationToleranceIncludesDiagonalBoundaryButNotAnotherPlane()
	{
		loadScene();
		NPC target = npc("Chaos Elemental", 1, 10, new WorldPoint(1002, 1002, 0));
		NPC otherPlane = npc("Chaos Elemental", 1, 11, new WorldPoint(1000, 1000, 1));
		WorldPoint origin = new WorldPoint(1000, 1000, 0);

		assertEquals(target, TargetResolver.uniqueLiveNpcNear(
			worldView, BossDefence.CHAOS_ELEMENTAL, 1000, 1000, 0, 2));
		assertEquals(target, TargetResolver.uniqueLiveNpcFor(
			worldView, BossDefence.CHAOS_ELEMENTAL, 1, origin, 2));
		assertTrue(TargetResolver.matches(target, BossDefence.CHAOS_ELEMENTAL, 1, origin, 2));
		assertFalse(TargetResolver.matches(otherPlane, BossDefence.CHAOS_ELEMENTAL, 1, origin, 2));
	}

	@Test
	public void queuedEvidenceKeepsActorIdentityAndLocationGuards()
	{
		NPC target = npc("Chaos Elemental", 1, 10, new WorldPoint(1000, 1000, 0));
		NPC replacement = npc("Chaos Elemental", 1, 10, new WorldPoint(1000, 1000, 0));
		PendingSpecQueue.TargetEvidence exact = PendingSpecQueue.TargetEvidence.from(target);
		PendingSpecQueue.TargetEvidence location = new PendingSpecQueue.TargetEvidence(
			BossDefence.CHAOS_ELEMENTAL, 1, new WorldPoint(1000, 1000, 0), null);

		assertTrue(exact.matches(target));
		assertFalse(exact.matches(replacement));
		assertTrue(location.matches(replacement));
		when(replacement.getWorldLocation()).thenReturn(new WorldPoint(1003, 1000, 0));
		assertFalse(location.matches(replacement));
	}
}
