package com.matthewloller.partybase;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PartyTimerFormatTest
{
	@Test
	public void formatsMinutesAndHours()
	{
		assertEquals("0:00", PartybasePanel.formatDuration(-5));
		assertEquals("0:05", PartybasePanel.formatDuration(5_999));
		assertEquals("12:34", PartybasePanel.formatDuration((12 * 60 + 34) * 1000L));
		assertEquals("1:02:03", PartybasePanel.formatDuration((3600 + 2 * 60 + 3) * 1000L));
	}
}
