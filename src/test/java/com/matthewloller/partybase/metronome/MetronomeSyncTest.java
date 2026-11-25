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
package com.matthewloller.partybase.metronome;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.dps.DpsModule;
import java.awt.Color;
import java.lang.reflect.Field;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;

public class MetronomeSyncTest
{
	private final MetronomeModule module = new MetronomeModule();
	private final PartybaseConfig config = mock(PartybaseConfig.class);
	private final ConfigManager configManager = mock(ConfigManager.class);
	private final DpsModule dps = mock(DpsModule.class);

	@Before
	public void setUp() throws Exception
	{
		when(config.metronomeSyncLeader()).thenReturn(true);
		when(config.tickCount()).thenReturn(4);
		when(config.tickCount2()).thenReturn(1);
		when(config.tickCount3()).thenReturn(1);
		when(config.colorCycle()).thenReturn(2);
		when(config.getTockColor()).thenReturn(Color.RED);
		when(dps.leaderId()).thenReturn(1L);
		set("config", config);
		set("configManager", configManager);
		set("dps", dps);
	}

	@Test
	public void followersCopyTheLeader()
	{
		module.applySync(sync(1));
		assertEquals(3, module.getTickCounter());
		assertEquals(2, module.getCurrentColorIndex());
		assertEquals(Color.RED, module.getCurrentColor());
		verify(configManager).setConfiguration(PartybaseConfig.GROUP, "tickCount", 5);
		// unchanged values are not written again
		verify(configManager, never()).setConfiguration(PartybaseConfig.GROUP, "colorCycle", 2);
	}

	@Test
	public void onlyTheLeaderIsFollowed()
	{
		module.applySync(sync(2));
		assertEquals(0, module.getTickCounter());
		when(dps.youLead()).thenReturn(true);
		module.applySync(sync(1));
		assertEquals("the leader keeps its own metronome", 0, module.getTickCounter());
		when(dps.youLead()).thenReturn(false);
		when(config.metronomeSyncLeader()).thenReturn(false);
		module.applySync(sync(1));
		assertEquals("sync turned off", 0, module.getTickCounter());
		verify(configManager, never()).setConfiguration(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
	}

	@Test
	public void tickButtonsStayBetweenOneAndTen() throws Exception
	{
		set("clientThread", mock(net.runelite.client.callback.ClientThread.class));
		module.setTickCount(7);
		module.setTickCount(15);
		module.setTickCount(0);
		verify(configManager).setConfiguration(PartybaseConfig.GROUP, "tickCount", 7);
		verify(configManager).setConfiguration(PartybaseConfig.GROUP, "tickCount", 10);
		verify(configManager).setConfiguration(PartybaseConfig.GROUP, "tickCount", 1);
	}

	@Test
	public void followingOnlyWhileTheLeadersSyncArrives()
	{
		assertEquals(false, module.isFollowingLeader());
		module.applySync(sync(1));
		assertEquals(true, module.isFollowingLeader());
		when(config.metronomeSyncLeader()).thenReturn(false);
		assertEquals("sync turned off", false, module.isFollowingLeader());
	}

	private static PartybaseMetronomeSync sync(long from)
	{
		PartybaseMetronomeSync m = new PartybaseMetronomeSync();
		m.setMemberId(from);
		m.tickCounter = 3;
		m.colorIndex = 2;
		m.colorCycle = 2;
		m.tickCount = 5;
		m.tickCount2 = 1;
		m.tickCount3 = 1;
		return m;
	}

	private void set(String name, Object value) throws Exception
	{
		Field f = MetronomeModule.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(module, value);
	}
}
