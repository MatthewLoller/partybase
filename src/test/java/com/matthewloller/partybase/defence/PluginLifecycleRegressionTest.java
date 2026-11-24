package com.matthewloller.partybase.defence;

import com.matthewloller.partybase.PartybaseConfig;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.junit.Before;
import org.junit.Test;

public class PluginLifecycleRegressionTest
{
	private DefenceModule plugin;
	private WSClient wsClient;

	@Before
	public void setUp() throws Exception
	{
		plugin = new DefenceModule();
		wsClient = mock(WSClient.class);
		Client client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		PartyService partyService = mock(PartyService.class);
		PluginManager pluginManager = mock(PluginManager.class);
		PartybaseConfig config = mock(PartybaseConfig.class);

		when(pluginManager.getPlugins()).thenReturn(Collections.emptyList());
		when(partyService.isInParty()).thenReturn(false);
		DefenceTracker tracker = new DefenceTracker(client, config);

		set("client", client);
		set("clientThread", clientThread);
		set("partyService", partyService);
		set("wsClient", wsClient);
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
		plugin.startUp(mock(net.runelite.client.plugins.Plugin.class));
		plugin.shutDown();

		verify(wsClient).unregisterMessage(PartybaseDefDefenceSync.class);
		verify(wsClient).unregisterMessage(PartybaseDefSpecEvent.class);
		verify(wsClient).unregisterMessage(PartybaseDefEncounterPresence.class);
		verify(wsClient).unregisterMessage(PartybaseDefBossPresence.class);
		verify(wsClient).unregisterMessage(PartybaseDefEncounterReset.class);
		verify(wsClient).unregisterMessage(PartybaseDefKerisTimer.class);
	}

	private void set(String name, Object value) throws Exception
	{
		Field field = DefenceModule.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(plugin, value);
	}
}
