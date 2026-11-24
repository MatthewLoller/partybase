package com.matthewloller.partybase.dps.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class PartyLeaderTest
{
	@Test
	public void aloneInANewPartyYouLead()
	{
		PartyLeader l = new PartyLeader();
		l.joined(50, 1000);
		assertFalse("wait for an answer first", l.tick(2000, Collections.singletonList(50L)));
		assertFalse(l.isLeader());
		assertTrue(l.tick(4000, Collections.singletonList(50L)));
		assertTrue(l.isLeader());
	}

	@Test
	public void joiningAPartyTheLeaderTellsYou()
	{
		PartyLeader l = new PartyLeader();
		l.joined(50, 1000);
		l.announce(20, 20);
		assertEquals(20, l.getLeader());
		assertFalse(l.isLeader());
		// a later claim from someone who is not the leader is ignored
		l.announce(30, 30);
		assertEquals(20, l.getLeader());
		// the leader can hand it over
		l.announce(20, 50);
		assertTrue(l.isLeader());
	}

	@Test
	public void nobodyAnswersFirstJoinedLeads()
	{
		PartyLeader l = new PartyLeader();
		l.joined(50, 1000);
		l.sawJoin(60, 400);
		l.sawJoin(40, 700);
		// 10 is in the party but has no Partybase: cannot lead
		assertTrue(l.tick(5000, Arrays.asList(10L, 40L, 50L, 60L)));
		assertEquals(60, l.getLeader());
	}

	@Test
	public void sameJoinTimeLowestIdLeads()
	{
		PartyLeader l = new PartyLeader();
		l.joined(50, 1000);
		l.sawJoin(40, 1000);
		assertTrue(l.tick(5000, Arrays.asList(40L, 50L)));
		assertEquals(40, l.getLeader());
	}

	@Test
	public void leaderLeavesFirstJoinedIsPicked()
	{
		PartyLeader l = new PartyLeader();
		l.joined(50, 1000);
		l.announce(20, 20);
		l.sawJoin(20, 100);
		l.sawJoin(30, 2000);
		l.memberLeft(20, Arrays.asList(30L, 50L));
		assertEquals("you joined before 30", 50, l.getLeader());
	}

	@Test
	public void leaderHandsOverToAPartybaseMember()
	{
		PartyLeader l = new PartyLeader();
		l.joined(50, 1000);
		l.tick(5000, Collections.singletonList(50L));
		assertTrue(l.isLeader());
		assertFalse("no Partybase", l.handOver(10));
		assertFalse("already you", l.handOver(50));
		l.sawPlugin(30);
		assertTrue(l.handOver(30));
		assertEquals(30, l.getLeader());
		assertFalse(l.isLeader());
		assertFalse("only the leader hands over", l.handOver(30));
	}

	@Test
	public void partyStartIsTheEarliestJoinOfMembersStillThere()
	{
		PartyLeader l = new PartyLeader();
		assertEquals("no party", 0, l.partyStartMs(Collections.emptyList()));
		l.joined(50, 5000);
		assertEquals(5000, l.partyStartMs(Collections.singletonList(50L)));
		l.sawJoin(20, 1000);
		l.sawJoin(30, 3000);
		assertEquals(1000, l.partyStartMs(Arrays.asList(20L, 30L, 50L)));
		l.memberLeft(20, Arrays.asList(30L, 50L));
		assertEquals(3000, l.partyStartMs(Arrays.asList(30L, 50L)));
		l.left();
		assertEquals(0, l.partyStartMs(Arrays.asList(30L, 50L)));
	}

	@Test
	public void onlyTheLeaderCanKickYou()
	{
		PartyLeader l = new PartyLeader();
		l.joined(50, 1000);
		l.announce(20, 20);
		assertFalse("not the leader", l.acceptKick(30, 50));
		assertFalse("kick for someone else", l.acceptKick(20, 60));
		assertTrue(l.acceptKick(20, 50));
	}

	@Test
	public void theLeaderCannotKickThemself()
	{
		PartyLeader l = new PartyLeader();
		l.joined(50, 1000);
		l.tick(5000, Collections.singletonList(50L));
		assertFalse(l.acceptKick(50, 50));
	}

	@Test
	public void leavingForgetsEverything()
	{
		PartyLeader l = new PartyLeader();
		l.joined(50, 1000);
		l.tick(5000, Collections.singletonList(50L));
		l.left();
		assertFalse(l.isJoined());
		assertFalse(l.isLeader());
		assertFalse(l.hasPlugin(50));
	}
}
