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
package com.matthewloller.partybase.party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.gson.Gson;
import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.party.events.PartybaseMemberData;
import com.matthewloller.partybase.party.events.PartybaseUserJoin;
import com.matthewloller.partybase.party.events.PartybaseUserPart;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.messages.PartyData;
import net.runelite.client.party.messages.PartyMemberMessage;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

/**
 * Two real PartyLink clients against a deployed relay server. Skipped unless PARTYBASE_SERVER_URL is set (and
 * PARTYBASE_SERVER_KEY for a server with keys):
 * PARTYBASE_SERVER_URL=wss://... PARTYBASE_SERVER_KEY=pb_... gradlew test --tests '*PartyLinkRelayLiveTest'
 */
public class PartyLinkRelayLiveTest
{
	/** A Partybase-style message type for the test. */
	public static class Hello extends PartyMemberMessage
	{
		public int n;
	}

	public static class Recorder
	{
		final List<Object> events = new CopyOnWriteArrayList<>();

		@Subscribe
		public void onHello(Hello e)
		{
			events.add(e);
		}

		@Subscribe
		public void onPartybaseUserJoin(PartybaseUserJoin e)
		{
			events.add(e);
		}

		@Subscribe
		public void onPartybaseUserPart(PartybaseUserPart e)
		{
			events.add(e);
		}

		@Subscribe
		public void onPartybaseMemberData(PartybaseMemberData e)
		{
			events.add(e);
		}
	}

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final OkHttpClient http = new OkHttpClient();
	private PartyLink a;
	private PartyLink b;

	private PartyLink link(Recorder recorder)
	{
		String url = System.getenv("PARTYBASE_SERVER_URL");
		String key = System.getenv("PARTYBASE_SERVER_KEY");
		PartybaseConfig config = mock(PartybaseConfig.class);
		when(config.serverUrl()).thenReturn(url);
		when(config.serverKey()).thenReturn(key == null ? "" : key);
		EventBus bus = new EventBus();
		bus.register(recorder);
		PartyLink link = new PartyLink(mock(Client.class), mock(ClientThread.class), bus, mock(PartyService.class),
			mock(WSClient.class), http, new Gson(), executor, mock(ChatMessageManager.class), config);
		link.startUp();
		link.registerMessage(Hello.class);
		return link;
	}

	@After
	public void tearDown()
	{
		if (a != null)
		{
			a.shutDown();
		}
		if (b != null)
		{
			b.shutDown();
		}
		executor.shutdownNow();
	}

	@Test
	public void twoClientsShareMessagesMemberDataAndPresence() throws Exception
	{
		Assume.assumeTrue("set PARTYBASE_SERVER_URL to run", System.getenv("PARTYBASE_SERVER_URL") != null);
		Recorder ra = new Recorder();
		Recorder rb = new Recorder();
		a = link(ra);
		b = link(rb);
		assertTrue(a.isRelay());

		String party = "live-test-" + System.currentTimeMillis();
		a.changeParty(party);
		waitFor(a::isConnected, "A joined");
		b.changeParty(party);
		waitFor(b::isConnected, "B joined");
		long aId = a.getLocalMember().getMemberId();
		long bId = b.getLocalMember().getMemberId();
		waitFor(() -> a.getMemberById(bId) != null, "A sees B");
		assertEquals(2, b.getMembers().size());
		assertEquals(PartyLink.passphraseToId(party), a.getPartyId());

		Hello hello = new Hello();
		hello.n = 42;
		a.send(hello);
		waitFor(() -> rb.events.stream().anyMatch(e -> e instanceof Hello && ((Hello) e).n == 42), "B gets Hello");
		Hello got = (Hello) rb.events.stream().filter(e -> e instanceof Hello).findFirst().get();
		assertEquals("sender id is set from the relay", aId, got.getMemberId());

		PartyData data = new PartyData();
		data.setCharacterName("Alice");
		data.setSpecEnergy(100);
		a.send(data);
		waitFor(() -> "Alice".equals(b.getMemberById(aId).getDisplayName()), "B sees Alice's data");
		assertEquals(100, b.getMemberById(aId).getSpecEnergy());

		b.changeParty(null);
		waitFor(() -> ra.events.stream().anyMatch(e -> e instanceof PartybaseUserPart
			&& ((PartybaseUserPart) e).getMemberId() == bId), "A told B left");
		assertNotNull(a.getLocalMember());
	}

	private static void waitFor(BooleanSupplier condition, String what) throws InterruptedException
	{
		for (int i = 0; i < 100; i++)
		{
			if (condition.getAsBoolean())
			{
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("timed out waiting for: " + what);
	}
}
