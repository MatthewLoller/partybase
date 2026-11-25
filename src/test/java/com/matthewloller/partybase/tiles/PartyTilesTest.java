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
package com.matthewloller.partybase.tiles;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.matthewloller.partybase.dps.DpsModule;
import com.matthewloller.partybase.party.PartyLink;
import com.matthewloller.partybase.party.PartybaseMember;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.party.messages.PartyMemberMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PartyTilesTest
{
	private static final long LEADER = 1;
	private static final long OTHER = 2;
	private static final long ME = 3;

	private final PartyLink party = mock(PartyLink.class);
	private final DpsModule dps = mock(DpsModule.class);
	private int changes;
	private PartyTiles tiles;

	@Before
	public void setUp()
	{
		PartybaseMember me = mock(PartybaseMember.class);
		when(me.getMemberId()).thenReturn(ME);
		when(party.getLocalMember()).thenReturn(me);
		when(party.isInParty()).thenReturn(true);
		when(dps.leaderId()).thenReturn(LEADER);
		tiles = new PartyTiles(mock(Client.class), party, dps, mock(ChatboxPanelManager.class), () -> changes++);
	}

	@Test
	public void onlyTheLeadersTilesAreUsed()
	{
		tiles.received(message(OTHER, PartybasePartyTiles.PUT, tile(1, 1, Color.RED, "no")));
		assertEquals(0, tiles.size());
		tiles.received(message(LEADER, PartybasePartyTiles.PUT, tile(1, 1, Color.RED, "Stand"), tile(2, 1, Color.BLUE, null)));
		assertEquals(2, tiles.size());
		assertEquals(1, changes);

		// a new label or colour replaces the tile at that spot
		tiles.received(message(LEADER, PartybasePartyTiles.PUT, tile(1, 1, Color.GREEN, "Run")));
		assertEquals(2, tiles.size());
		assertEquals("Run", tiles.all().get(0).label);
		assertEquals(Color.GREEN, tiles.all().get(0).color);

		tiles.received(message(LEADER, PartybasePartyTiles.REMOVE, tile(2, 1, null, null)));
		assertEquals(1, tiles.size());
		tiles.received(message(LEADER, PartybasePartyTiles.SET, tile(5, 5, Color.RED, null)));
		assertEquals("set replaces everything", 5, tiles.all().get(0).regionX);
		assertEquals(1, tiles.size());
		tiles.received(message(LEADER, PartybasePartyTiles.CLEAR));
		assertEquals(0, tiles.size());
	}

	@Test
	public void theLeaderSendsChangesAndOthersDoNot()
	{
		when(dps.youLead()).thenReturn(true);
		tiles.put(tile(1, 1, Color.RED, null));
		ArgumentCaptor<PartyMemberMessage> sent = ArgumentCaptor.forClass(PartyMemberMessage.class);
		verify(party).send(sent.capture());
		assertEquals(PartybasePartyTiles.PUT, ((PartybasePartyTiles) sent.getValue()).op);

		PartyTiles follower = followerTiles();
		follower.put(tile(1, 1, Color.RED, null));
		assertEquals("a follower can still mark locally (solo use), but sends nothing", 1, follower.size());
	}

	@Test
	public void newMembersGetEveryTileInBatches()
	{
		when(dps.youLead()).thenReturn(true);
		List<TilePoint> many = new ArrayList<>();
		for (int i = 0; i < 320; i++)
		{
			many.add(tile(i % 64, i / 64, Color.RED, null));
		}
		for (TilePoint t : many)
		{
			tiles.put(t);
		}
		org.mockito.Mockito.clearInvocations(party);
		tiles.sendAllIfLeader();
		ArgumentCaptor<PartyMemberMessage> sent = ArgumentCaptor.forClass(PartyMemberMessage.class);
		verify(party, org.mockito.Mockito.times(3)).send(sent.capture());
		List<PartyMemberMessage> msgs = sent.getAllValues();
		assertEquals(PartybasePartyTiles.SET, ((PartybasePartyTiles) msgs.get(0)).op);
		assertEquals(PartybasePartyTiles.PUT, ((PartybasePartyTiles) msgs.get(1)).op);
		int total = 0;
		for (PartyMemberMessage m : msgs)
		{
			total += ((PartybasePartyTiles) m).tiles.size();
		}
		assertEquals(320, total);
	}

	@Test
	public void leavingThePartyClearsTiles()
	{
		tiles.received(message(LEADER, PartybasePartyTiles.PUT, tile(1, 1, Color.RED, null)));
		tiles.partyChanged();
		assertEquals(0, tiles.size());
		assertEquals("12850:1:1:0", PartyTiles.key(tile(1, 1, null, null)));
	}

	private PartyTiles followerTiles()
	{
		PartyLink p = mock(PartyLink.class);
		DpsModule d = mock(DpsModule.class);
		when(p.isInParty()).thenReturn(true);
		PartyTiles t = new PartyTiles(mock(Client.class), p, d, mock(ChatboxPanelManager.class), () ->
		{
		});
		verify(p, never()).send(any());
		return t;
	}

	private static PartybasePartyTiles message(long from, String op, TilePoint... list)
	{
		PartybasePartyTiles m = new PartybasePartyTiles();
		m.setMemberId(from);
		m.op = op;
		m.tiles = new ArrayList<>(Arrays.asList(list));
		return m;
	}

	private static TilePoint tile(int x, int y, Color color, String label)
	{
		TilePoint t = new TilePoint();
		t.regionId = 12850;
		t.regionX = x;
		t.regionY = y;
		t.color = color;
		t.label = label;
		return t;
	}
}
