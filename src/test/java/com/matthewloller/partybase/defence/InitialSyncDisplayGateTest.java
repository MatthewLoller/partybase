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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class InitialSyncDisplayGateTest
{
	private DefenceTracker.DefenceState state(int index, boolean drained)
	{
		return new DefenceTracker.DefenceState(index, "Example boss",
				100, 0, 100, 0, 0, 0, 0, 0, 0, drained, false);
	}

	@Test
	public void waitsOnlyForTheShortInitialPeriod()
	{
		InitialSyncDisplayGate gate = new InitialSyncDisplayGate();
		gate.arm(7, BossDefence.TEKTON, 100, 4);
		assertTrue(gate.isWaiting(state(7, false), 100));
		assertTrue(gate.isWaiting(state(7, false), 103));
		assertFalse(gate.isWaiting(state(7, false), 104));
		gate.expire(104);
		assertFalse(gate.isWaiting(state(7, false), 104));
	}

	@Test
	public void doesNotHideDrainAndDoesNotAffectOtherTargets()
	{
		InitialSyncDisplayGate gate = new InitialSyncDisplayGate();
		gate.arm(7, BossDefence.TEKTON, 100, 4);
		assertFalse(gate.isWaiting(state(7, true), 101));
		assertFalse(gate.isWaiting(state(8, false), 101));
	}

	@Test
	public void releaseTreatsEnragedTektonAsTekton()
	{
		InitialSyncDisplayGate gate = new InitialSyncDisplayGate();
		gate.arm(7, BossDefence.TEKTON, 100, 4);
		gate.release(7, BossDefence.TEKTON_ENRAGED);
		assertFalse(gate.isWaiting(state(7, false), 101));
	}

	@Test
	public void duplicatePresenceDoesNotExtendGracePeriod()
	{
		InitialSyncDisplayGate gate = new InitialSyncDisplayGate();
		gate.arm(7, BossDefence.TEKTON, 100, 4);
		gate.arm(7, BossDefence.TEKTON, 102, 4);
		assertFalse(gate.isWaiting(state(7, false), 104));
	}

	@Test
	public void clearOnRaidExitOrReset()
	{
		InitialSyncDisplayGate gate = new InitialSyncDisplayGate();
		gate.arm(7, BossDefence.TEKTON, 100, 4);
		gate.clear();
		assertFalse(gate.isWaiting(state(7, false), 100));
	}

	@Test
	public void clearForBossLeavesOtherBossGatesUntouched()
	{
		InitialSyncDisplayGate gate = new InitialSyncDisplayGate();
		gate.arm(7, BossDefence.TEKTON, 100, 4);
		gate.arm(8, BossDefence.VORKATH, 100, 4);
		gate.clearForBoss(BossDefence.TEKTON);
		assertFalse(gate.isWaiting(state(7, false), 101));
		assertTrue(gate.isWaiting(state(8, false), 101));
	}
}
