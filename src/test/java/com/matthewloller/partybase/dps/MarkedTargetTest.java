/*
 * Copyright (c) 2026, Matthew Loller <matthewloller@gmail.com>
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
package com.matthewloller.partybase.dps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MarkedTargetTest
{
	@Test
	public void severalMarksEachWithAnIcon()
	{
		MarkedTarget m = new MarkedTarget();
		assertTrue(m.isEmpty());
		assertEquals(TargetIcon.STAR, m.nextIcon());

		m.set("302:10", 1, "Vorkath", m.nextIcon());
		m.set("302:11", 2, "Goblin", m.nextIcon());
		assertEquals(TargetIcon.STAR, m.get("302:10").icon);
		assertEquals("new marks get the next free icon", TargetIcon.CIRCLE, m.get("302:11").icon);
		assertEquals(2, m.all().size());
		assertEquals("marking order", "Vorkath", m.all().get(0).name);

		// marking again changes the icon and keeps the order
		m.set("302:10", 1, "Vorkath", TargetIcon.SKULL);
		assertEquals(TargetIcon.SKULL, m.get("302:10").icon);
		assertEquals("Vorkath", m.all().get(0).name);
		assertEquals("star is free again", TargetIcon.STAR, m.nextIcon());

		assertEquals("Goblin", m.remove("302:11").name);
		assertNull(m.remove("302:11"));
		assertFalse(m.is("302:11"));
		assertTrue(m.is("302:10"));
		m.clear();
		assertTrue(m.isEmpty());
	}

	@Test
	public void unknownIconNamesFallBackToStar()
	{
		assertEquals(TargetIcon.MOON, TargetIcon.of("MOON"));
		assertEquals(TargetIcon.STAR, TargetIcon.of("RAINBOW"));
		assertEquals(TargetIcon.STAR, TargetIcon.of(null));
	}
}
