/*
 * Copyright (c) 2026, Heisty <https://github.com/Falsary>
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
package com.matthewloller.partybase.defence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.party.PartyLink;
import java.lang.reflect.Field;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.junit.Before;
import org.junit.Test;

public class PluginLifecycleRegressionTest
{
	private DefenceModule plugin;
	private PartyLink partyLink;

	@Before
	public void setUp() throws Exception
	{
		plugin = new DefenceModule();
		Client client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		partyLink = mock(PartyLink.class);
		PluginManager pluginManager = mock(PluginManager.class);
		PartybaseConfig config = mock(PartybaseConfig.class);

		when(pluginManager.getPlugins()).thenReturn(Collections.emptyList());
		when(partyLink.isInParty()).thenReturn(false);
		DefenceTracker tracker = new DefenceTracker(client, config);

		set("client", client);
		set("clientThread", clientThread);
		set("partyLink", partyLink);
		set("pluginManager", pluginManager);
		set("overlayManager", mock(OverlayManager.class));
		set("itemManager", mock(ItemManager.class));
		set("skillIconManager", mock(SkillIconManager.class));
		set("defenceTracker", tracker);
		set("localSpecDetector", mock(LocalDefenceSpecDetector.class));
		set("config", config);
		set("configManager", mock(ConfigManager.class));
		set("infoBoxManager", mock(InfoBoxManager.class));
	}

	@Test
	public void shutdownUnregistersEveryRegisteredMessageType()
	{
		plugin.startUp(mock(com.matthewloller.partybase.PartybasePlugin.class));
		plugin.shutDown();

		verify(partyLink).unregisterMessage(PartybaseDefenceSync.class);
		verify(partyLink).unregisterMessage(PartybaseDefenceSpecEvent.class);
		verify(partyLink).unregisterMessage(PartybaseDefenceEncounterPresence.class);
		verify(partyLink).unregisterMessage(PartybaseDefenceBossPresence.class);
		verify(partyLink).unregisterMessage(PartybaseDefenceEncounterReset.class);
		verify(partyLink).unregisterMessage(PartybaseDefenceKerisTimer.class);
	}

	private void set(String name, Object value) throws Exception
	{
		Field field = DefenceModule.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(plugin, value);
	}
}
