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

import com.matthewloller.partybase.defence.boss.OcellusActors;
import java.util.Iterator;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;

// The Fractured Archive actor transitions that affect encounter ownership.
final class TfaEncounterMechanics
{
	private TfaEncounterMechanics()
	{
	}

	static PhasePresence scan(WorldView world)
	{
		boolean phaseThree = world != null
			&& TargetResolver.uniqueLiveNpc(world, BossDefence.TFA_OCELLUS_P3) != null;
		boolean phaseTwo = false;
		boolean phaseOneDefeated = false;
		if (world != null && world.npcs() != null)
		{
			Iterator<? extends NPC> iterator = world.npcs().iterator();
			while (iterator != null && iterator.hasNext())
			{
				NPC npc = iterator.next();
				phaseTwo |= isOcellusPhaseTwoActor(npc);
				phaseOneDefeated |= isOcellusPhaseOneDefeated(npc);
			}
		}
		return new PhasePresence(phaseTwo, phaseThree, phaseOneDefeated);
	}

	static boolean isOcellusPhaseTwoActor(NPC npc)
	{
		return OcellusActors.marksPhaseTwo(npc);
	}

	static boolean isOcellusPhaseOneDefeated(NPC npc)
	{
		return OcellusActors.isPhaseOneDefeated(npc);
	}

	static final class PhasePresence
	{
		final boolean phaseTwo;
		final boolean phaseThree;
		final boolean phaseOneDefeated;

		private PhasePresence(boolean phaseTwo, boolean phaseThree, boolean phaseOneDefeated)
		{
			this.phaseTwo = phaseTwo;
			this.phaseThree = phaseThree;
			this.phaseOneDefeated = phaseOneDefeated;
		}

		boolean hasLeftPhaseOne()
		{
			return phaseTwo || phaseThree || phaseOneDefeated;
		}
	}
}
