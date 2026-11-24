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
