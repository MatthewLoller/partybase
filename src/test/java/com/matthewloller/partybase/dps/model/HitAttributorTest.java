/*
 * Copyright (c) 2026, tjeerocka <https://github.com/tjeerocka>
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
package com.matthewloller.partybase.dps.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class HitAttributorTest
{
	private static final String BOSS = "301:10";
	private static final String RAT = "301:20";
	private HitAttributor a;

	@Before
	public void setUp()
	{
		a = new HitAttributor();
	}

	@Test
	public void meleeHitGoesToTheAttacker()
	{
		a.attack(100, "Bob", BOSS);
		assertEquals("Bob", a.assign(101, BOSS));
	}

	@Test
	public void oneAttackGivesOneHit()
	{
		a.attack(100, "Bob", BOSS);
		assertEquals("Bob", a.assign(101, BOSS));
		assertNull("next tick: no attack left", a.assign(102, BOSS));
	}

	@Test
	public void twoPlayersOldestAttackFirst()
	{
		a.attack(100, "Bob", BOSS);   // ranged, lands later
		a.attack(101, "Ann", BOSS);
		assertEquals("Bob", a.assign(103, BOSS));
		assertEquals("Ann", a.assign(104, BOSS));
	}

	@Test
	public void attackOnOtherNpcDoesNotMatch()
	{
		a.attack(100, "Bob", RAT);
		assertNull(a.assign(101, BOSS));
		assertEquals("Bob", a.assign(101, RAT));
	}

	@Test
	public void tooOldAttackIsForgotten()
	{
		a.attack(100, "Bob", BOSS);
		assertNull(a.assign(100 + HitAttributor.MAX_DELAY_TICKS + 1, BOSS));
	}

	@Test
	public void scytheThreeHitsplatsSameTick()
	{
		a.attack(100, "Bob", BOSS);
		assertEquals("Bob", a.assign(101, BOSS));
		assertEquals("Bob", a.assign(101, BOSS));
		assertEquals("Bob", a.assign(101, BOSS));
	}

	@Test
	public void extraHitWithTwoAttackersLandedIsUnknown()
	{
		a.attack(100, "Bob", BOSS);
		a.attack(100, "Ann", BOSS);
		assertEquals("Bob", a.assign(101, BOSS));
		assertEquals("Ann", a.assign(101, BOSS));
		assertNull(a.assign(101, BOSS));
	}

	@Test
	public void animationTwiceInOneAttackIsOneAttack()
	{
		a.attack(100, "Bob", BOSS);
		a.attack(101, "Bob", BOSS); // for example a block animation right after
		assertEquals("Bob", a.assign(102, BOSS));
		assertNull(a.assign(103, BOSS));
	}
}
