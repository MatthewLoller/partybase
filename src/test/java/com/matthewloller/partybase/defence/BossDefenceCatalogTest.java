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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import org.junit.Test;

public class BossDefenceCatalogTest
{
	@Test
	public void everyBossHasValidImmutableStatData()
	{
		for (BossDefence boss : BossDefence.values())
		{
			assertNotNull(boss.name(), boss.getNpcName());
			assertTrue(boss.name(), boss.getBaseDef() >= 0);
			assertTrue(boss.name(), boss.getBaseMagic() >= 0);
			assertTrue(boss.name(), boss.getBaseAtk() >= 0);
			assertTrue(boss.name(), boss.getBaseStr() >= 0);
			assertTrue(boss.name(), boss.getBaseMagicDef() >= 0);
			assertTrue(boss.name(), boss.getMinDef() >= 0);
			assertTrue(boss.name(), boss.getMinDef() <= boss.getBaseDef());
		}
	}

	@Test
	public void mutableFlagCollectionIsNotExposed()
	{
		boolean hasFlagGetter = Arrays.stream(BossDefence.class.getMethods())
			.anyMatch(method -> method.getName().equals("getFlags"));

		assertFalse(hasFlagGetter);
	}
}
