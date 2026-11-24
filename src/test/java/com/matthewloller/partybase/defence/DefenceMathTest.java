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
import org.junit.Test;

public class DefenceMathTest
{
	@Test
	public void magicRollMatchesDocumentedFormula()
	{
		assertEquals((9L + 215L) * (100L + 64L), DefenceMath.magicRoll(215, 100));
		assertEquals((9L + 250L) * (0L + 64L), DefenceMath.magicRoll(250, 0));
	}

	@Test
	public void bandosDamageStopsWhenDefenceStopsAtZero()
	{
		DefenceMath.BandosResult result = DefenceMath.applyBandos(310, 0, 200, 180, 200, 350);

		assertEquals(0, result.getDefence());
		assertEquals(160, result.getStrength());
		assertEquals(180, result.getAttack());
		assertEquals(200, result.getMagic());
	}

	@Test
	public void bandosDamageSpillsThroughEveryDrainableStat()
	{
		DefenceMath.BandosResult result = DefenceMath.applyBandos(310, 0, 200, 180, 200, 900);

		assertEquals(0, result.getDefence());
		assertEquals(0, result.getStrength());
		assertEquals(0, result.getAttack());
		assertEquals(0, result.getMagic());
	}

	@Test
	public void bandosDamageDoesNotSpillAboveZeroDefence()
	{
		DefenceMath.BandosResult result = DefenceMath.applyBandos(310, 0, 200, 180, 200, 50);

		assertEquals(260, result.getDefence());
		assertEquals(200, result.getStrength());
		assertEquals(180, result.getAttack());
		assertEquals(200, result.getMagic());
	}
}
