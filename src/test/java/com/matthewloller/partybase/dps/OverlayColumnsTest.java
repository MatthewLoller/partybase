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
package com.matthewloller.partybase.dps;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import com.matthewloller.partybase.dps.model.FightReport;
import com.matthewloller.partybase.dps.model.FightTracker;
import com.matthewloller.partybase.dps.model.HealType;
import com.matthewloller.partybase.dps.model.MeterMode;
import com.matthewloller.partybase.dps.model.PlayerKind;
import org.junit.Test;

public class OverlayColumnsTest
{
	@Test
	public void everyModeHasAValueForEveryColumn()
	{
		FightTracker t = new FightTracker();
		t.playerHit(1000, "Me", PlayerKind.LOCAL, "301:5", 1, "Rat", false, 51, false);
		t.heal(2000, "Me", PlayerKind.LOCAL, HealType.FOOD, 40, 16);
		t.damageTaken(2000, "Me", PlayerKind.LOCAL, 7);
		FightReport r = FightReport.of(t.getCurrent(), false, 24_000);
		for (MeterMode m : MeterMode.values())
		{
			for (boolean max : new boolean[] {false, true})
			{
				String[] labels = DpsOverlay.columnLabels(m, max);
				assertEquals(m.name(), labels.length, DpsOverlay.columnValues(r.row("Me"), m, r.total(m), max).length);
			}
		}
	}

	@Test
	public void damageColumns()
	{
		FightTracker t = new FightTracker();
		// hits every 8 s or less: the clock never pauses
		t.playerHit(1000, "Me", PlayerKind.LOCAL, "301:5", 1, "Rat", false, 20, false);
		t.playerHit(9000, "Me", PlayerKind.LOCAL, "301:5", 1, "Rat", false, 0, true);
		t.playerHit(17000, "Me", PlayerKind.LOCAL, "301:5", 1, "Rat", false, 31, false);
		FightReport r = FightReport.of(t.getCurrent(), false, 24_000);
		assertArrayEquals(new String[] {"Damage", "DPS", "%"}, DpsOverlay.columnLabels(MeterMode.DAMAGE, false));
		// 51 damage in 23 s = 2.2 per second, shown without "/s" because the column says DPS
		assertArrayEquals(new String[] {"51", "2.2", "100%"},
			DpsOverlay.columnValues(r.row("Me"), MeterMode.DAMAGE, r.total(MeterMode.DAMAGE), false));
		assertArrayEquals(new String[] {"Damage", "Max", "DPS", "%"}, DpsOverlay.columnLabels(MeterMode.DAMAGE, true));
		assertArrayEquals(new String[] {"51", "31", "2.2", "100%"},
			DpsOverlay.columnValues(r.row("Me"), MeterMode.DAMAGE, r.total(MeterMode.DAMAGE), true));
	}
}
