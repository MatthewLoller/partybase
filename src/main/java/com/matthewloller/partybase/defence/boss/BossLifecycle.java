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
package com.matthewloller.partybase.defence.boss;

import com.matthewloller.partybase.defence.BossDefence;
import net.runelite.api.gameval.NpcID;

// Encounter lifetime rules that do not belong in the boss stat catalogue.
public enum BossLifecycle
{
	STANDARD(false),
	TEKTON(true),
	DREADNOTH(true),
	KEPHRI(true)
	{
		@Override
		public boolean preserveStateAtZeroHp(int npcId)
		{
			// Shielded and weak Kephri can sit at zero HP mid-encounter.
			return npcId == NpcID.TOA_KEPHRI_BOSS_SHIELDED
			|| npcId == NpcID.TOA_KEPHRI_BOSS_WEAK;
		}

		@Override
		public boolean resetsStatsOnActorEntry(int npcId)
		{
			return npcId == NpcID.TOA_KEPHRI_BOSS_ENRAGE;
		}

		@Override
		public boolean endsEncounterOnActorEntry(int npcId)
		{
			return npcId == NpcID.TOA_KEPHRI_BOSS_DEAD;
		}
	},
	SOTETSEG(true),
	TFA_PHASE_PERSISTENT(true)
	{
		@Override
		public boolean preserveStateAtZeroHp(int npcId)
		{
			return true;
		}
	};

	private final boolean phasePersistent;

	BossLifecycle(boolean phasePersistent)
	{
		this.phasePersistent = phasePersistent;
	}

	public boolean isPhasePersistent()
	{
		return phasePersistent;
	}

	public boolean preserveStateAtZeroHp(int npcId)
	{
		return false;
	}

	public boolean resetsStatsOnActorEntry(int npcId)
	{
		return false;
	}

	public boolean endsEncounterOnActorEntry(int npcId)
	{
		return false;
	}

	public static BossLifecycle forBoss(BossDefence boss)
	{
		if (boss == null)
		{
			return STANDARD;
		}
		switch (boss.trackingType())
		{
			case TEKTON:
				return TEKTON;
			case TFA_DREADNOTH:
				return DREADNOTH;
			case KEPHRI:
				return KEPHRI;
			case SOTETSEG:
				return SOTETSEG;
			case TFA_ARCHIVE_GUARDIAN:
			case TFA_XARTH:
			case TFA_OCELLUS_P1:
			case TFA_OCELLUS_P2:
			case TFA_OCELLUS_P3:
				return TFA_PHASE_PERSISTENT;
			default:
				return STANDARD;
		}
	}
}
