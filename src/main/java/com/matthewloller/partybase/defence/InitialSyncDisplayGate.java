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

import java.util.HashMap;
import java.util.Map;

// Hide the first undrained frame while peer state loads.
final class InitialSyncDisplayGate
{
	private static final class Gate
	{
		private final BossDefence boss;
		private final int expiresAtTick;

		private Gate(BossDefence boss, int expiresAtTick)
		{
			this.boss = boss;
			this.expiresAtTick = expiresAtTick;
		}
	}

	private final Map<Integer, Gate> gates = new HashMap<>();

	// Do not extend an active grace period.
	void arm(int npcIndex, BossDefence boss, int tick, int graceTicks)
	{
		boss = boss == null ? null : boss.trackingType();
		if (npcIndex < 0 || boss == null || graceTicks <= 0)
		{
			return;
		}

		Gate existing = gates.get(npcIndex);
		if (existing != null && tick < existing.expiresAtTick && existing.boss == boss)
		{
			return;
		}
		gates.put(npcIndex, new Gate(boss, tick + graceTicks));
	}

	// Validated party state releases the gate.
	void release(int npcIndex, BossDefence boss)
	{
		boss = boss == null ? null : boss.trackingType();
		Gate gate = gates.get(npcIndex);
		if (gate != null && gate.boss == boss)
		{
			gates.remove(npcIndex);
		}
	}

	// Clear gates for an ended encounter.
	void clearForBoss(BossDefence boss)
	{
		if (boss == null)
		{
			return;
		}
		BossDefence trackingBoss = boss.trackingType();
		gates.entrySet().removeIf(entry -> entry.getValue().boss == trackingBoss);
	}

	// Drained state is always displayable.
	boolean isWaiting(DefenceTracker.DefenceState state, int tick)
	{
		if (state == null || state.isDrained())
		{
			return false;
		}
		Gate gate = gates.get(state.getNpcIndex());
		return gate != null && tick < gate.expiresAtTick;
	}

	void expire(int tick)
	{
		gates.entrySet().removeIf(entry -> tick >= entry.getValue().expiresAtTick);
	}

	void clear()
	{
		gates.clear();
	}
}
