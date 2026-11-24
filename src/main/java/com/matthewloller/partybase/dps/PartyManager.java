/*
 * Copyright (c) 2026, tjeerocka <https://github.com/tjeerocka>
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

import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.dps.model.PartyLeader;
import com.matthewloller.partybase.dps.model.PartyNotices;
import com.matthewloller.partybase.dps.model.RaidParty;
import com.matthewloller.partybase.party.PartyLink;
import com.matthewloller.partybase.party.PartybaseMember;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.client.config.ConfigManager;

/**
 * Everything about the RuneLite party: joining and leaving, the Partybase leader and kick,
 * chat notices, the automatic raid party, and which members are in your instance.
 * All methods run on the client thread.
 */
@Slf4j
@Singleton
class PartyManager
{
	/** A member counts as running Partybase for this long after their last update. */
	private static final long REPORTER_MS = 120_000;
	/**
	 * Kicked members are hidden at once, for this long: the party server may list them a little longer.
	 * Members who said bye stay hidden until the server drops them, which can take minutes.
	 */
	private static final long KICK_HIDE_MS = 120_000;
	/** After a logout you stay in the party this long, to pick another world on the login screen. */
	static final long LEAVE_AFTER_LOGOUT_MS = 10_000;
	/** A remembered raid party is kept over a client restart for this long (a long CoX raid). */
	private static final long REMEMBER_MS = 2 * 60 * 60_000;
	private static final String RAID_PARTY_KEY = "raidParty";
	private static final String LAST_PARTY_KEY = "lastParty";

	private final Client client;
	private final PartyLink partyLink;
	private final PartybaseConfig config;
	private final ChatNotifier chat;
	private final RaidPartyDetector raidPartyDetector;
	private final ConfigManager configManager;

	private final PartyLeader leader = new PartyLeader();
	private final MarkedTarget marked;
	/** When the party became active (see PartyLeader.partyStartMs), 0 in none. Read by the Swing thread. */
	private volatile long partyStartMs;
	/** The leader's display name, or null. Read by the Swing thread. */
	private volatile String leaderName;
	/** You lead the party; read by the Swing thread. */
	private volatile boolean youLead;
	private final RaidParty raidParty = new RaidParty();
	private final PartyNotices notices = new PartyNotices();
	private final Map<String, Long> reporters = new HashMap<>();
	/** Members who are gone but still listed by the party server, with the time to show them again. */
	private final Map<Long, Long> hidden = new HashMap<>();
	private final Set<String> instanceMates = new HashSet<>();
	private long localMemberId;
	/** Read by the logout timer, which runs off the client thread. */
	private volatile long loggedOutAtMs;
	/** You left or were kicked: no own party until the next login. */
	private boolean ownPartyStopped;
	/** A boss fight (or Barrows run) started: make your own party if you are in none. */
	private boolean ownPartyWanted;
	/** The party at the last tick, to see you leave your own party in RuneLite's Party panel. */
	private String lastParty;
	/** Partybase changed the party since the last tick. */
	private volatile boolean changedByUs;
	private boolean rememberedLoaded;
	/** The last party you were in, for the Rejoin button. Kept over a client restart. */
	private String lastPartyName;
	private boolean lastPartyLoaded;
	private String rememberedSaved;

	@Inject
	PartyManager(Client client, PartyLink partyLink, PartybaseConfig config, ChatNotifier chat,
		RaidPartyDetector raidPartyDetector, ConfigManager configManager, MarkedTarget marked)
	{
		this.marked = marked;
		this.configManager = configManager;
		this.client = client;
		this.partyLink = partyLink;
		this.config = config;
		this.chat = chat;
		this.raidPartyDetector = raidPartyDetector;
	}

	/** You lead the party (as Partybase sees it). */
	boolean isLeader()
	{
		return inParty() && leader.isLeader();
	}

	boolean inParty()
	{
		return partyLink.isInParty() && partyLink.getLocalMember() != null;
	}

	void join(String name)
	{
		if (partyLink.isInParty())
		{
			return;
		}
		changeParty(name);
		chat.info("you joined party \"" + name + "\".");
	}

	/** A random name for a new party, or null when you are in a party already. */
	String randomName()
	{
		return partyLink.isInParty() ? null : partyLink.generatePassphrase().replace('-', ' ');
	}

	/** Join the last party you were in again. */
	void rejoin()
	{
		String name = lastParty();
		if (name != null)
		{
			join(name);
		}
	}

	/** The last party you were in, or null. Your own party is left out: nobody else is in it. */
	String lastParty()
	{
		if (!lastPartyLoaded)
		{
			lastPartyLoaded = true;
			lastPartyName = configManager.getConfiguration(PartybaseConfig.STATE_GROUP, LAST_PARTY_KEY);
		}
		return lastPartyName;
	}

	private void rememberLastParty()
	{
		String current = partyLink.isInParty() ? partyLink.getPartyPassphrase() : null;
		if (current == null || RaidParty.isOwn(current) || current.equals(lastParty()))
		{
			return;
		}
		lastPartyName = current;
		configManager.setConfiguration(PartybaseConfig.STATE_GROUP, LAST_PARTY_KEY, current);
	}

	void leave()
	{
		if (partyLink.isInParty())
		{
			ownPartyStopped = true;
			changeParty(null);
			chat.info("you left the party.");
		}
	}

	void kick(long memberId, String name)
	{
		if (!leader.isLeader() || !partyLink.isInParty())
		{
			chat.info("only the party leader can kick.");
			return;
		}
		partyLink.send(message(PartybaseDpsPartyMessage.KICK, memberId));
		hidden.put(memberId, System.currentTimeMillis() + KICK_HIDE_MS);
		notices.kicked(memberId);
		chat.kicked("you removed " + name + " from the party.");
	}

	private void changeParty(String passphrase)
	{
		// RuneLite just closes the connection, and the party server keeps listing you for minutes
		send(PartybaseDpsPartyMessage.BYE);
		changedByUs = true;
		partyLink.changeParty(passphrase);
		leader.left();
	}

	/** A Partybase message arrived from this member. */
	void pluginSeen(long memberId)
	{
		leader.sawPlugin(memberId);
	}

	/** A Partybase update arrived from this player. */
	void reporterSeen(String name, long now)
	{
		reporters.put(name, now);
	}

	boolean isReporter(String name, long now)
	{
		Long last = reporters.get(name);
		return last != null && now - last <= REPORTER_MS;
	}

	/** The member was seen in your instance since you entered it. */
	boolean isInstanceMate(String name)
	{
		WorldView wv = client.getTopLevelWorldView();
		return wv != null && wv.isInstance() && instanceMates.contains(name);
	}

	void onMessage(PartybaseDpsPartyMessage m)
	{
		PartybaseMember local = partyLink.getLocalMember();
		if (m.type == null || local == null || m.getMemberId() == local.getMemberId())
		{
			return;
		}
		leader.sawJoin(m.getMemberId(), m.since);
		switch (m.type)
		{
			case PartybaseDpsPartyMessage.HI:
				leader.announce(m.getMemberId(), m.leader);
				if (leader.isLeader())
				{
					send(PartybaseDpsPartyMessage.LEADER);
					sendMarks();
				}
				else
				{
					send(PartybaseDpsPartyMessage.HERE);
				}
				break;
			case PartybaseDpsPartyMessage.LEADER:
			{
				long before = leader.getLeader();
				leader.announce(m.getMemberId(), m.leader);
				if (leader.isLeader() && before != leader.getLeader())
				{
					chat.info("you are the party leader now: shift right-click a monster to mark it as the target.");
				}
				break;
			}
			case PartybaseDpsPartyMessage.HERE:
				leader.announce(m.getMemberId(), m.leader);
				break;
			case PartybaseDpsPartyMessage.MARK:
				if (m.getMemberId() == leader.getLeader() && m.npcKey != null)
				{
					boolean isNew = !marked.is(m.npcKey);
					TargetIcon icon = TargetIcon.of(m.icon);
					String name = Names.clean(m.npcName);
					marked.set(m.npcKey, m.npcId, name, icon);
					if (isNew)
					{
						chat.info(memberNameOr(m.getMemberId(), "the leader") + " marked " + name + " ("
							+ icon.getLabel().toLowerCase() + ").");
					}
				}
				break;
			case PartybaseDpsPartyMessage.UNMARK:
				if (m.getMemberId() == leader.getLeader())
				{
					if (m.npcKey == null)
					{
						marked.clear();
					}
					else
					{
						marked.remove(m.npcKey);
					}
				}
				break;
			case PartybaseDpsPartyMessage.KICK:
				onKick(m);
				break;
			case PartybaseDpsPartyMessage.BYE:
				hidden.put(m.getMemberId(), Long.MAX_VALUE);
				onMemberLeft(m.getMemberId());
				break;
			default:
				break;
		}
	}

	private void onKick(PartybaseDpsPartyMessage m)
	{
		leader.sawPlugin(m.getMemberId());
		String by = memberName(m.getMemberId());
		String bySuffix = by != null ? " by " + by + "." : ".";
		if (leader.acceptKick(m.getMemberId(), m.target))
		{
			ownPartyStopped = true;
			changeParty(null);
			chat.kicked("you were removed from the party" + bySuffix);
		}
		else if (m.leader == m.getMemberId() && m.leader == leader.getLeader())
		{
			notices.kicked(m.target);
			String who = memberName(m.target);
			if (who != null)
			{
				chat.kicked(who + " was removed from the party" + bySuffix);
			}
		}
	}

	void onMemberLeft(long memberId)
	{
		leader.memberLeft(memberId, memberIds());
	}

	/**
	 * You logged out or hopped: tell again who is in the party after the next login.
	 * @param toLoginScreen a real logout (not a world hop): your own party may be made again
	 */
	void loggedOut(boolean toLoginScreen)
	{
		notices.reset();
		if (!toLoginScreen)
		{
			return;
		}
		ownPartyStopped = false;
		loggedOutAtMs = System.currentTimeMillis();
	}

	/**
	 * Some time after a logout: still logged out, so leave. RuneLite keeps you in the party while
	 * logged out, and the others would see you there for nothing. Runs on the timer thread: on the
	 * login screen the client thread does not reliably run queued work, and nothing else touches
	 * the party there.
	 */
	void leaveIfStillLoggedOut()
	{
		// a little slack: the timer may fire a moment early; a newer logout has its own timer
		GameState state = client.getGameState();
		long outMs = System.currentTimeMillis() - loggedOutAtMs;
		boolean stillOut = state == GameState.LOGIN_SCREEN && outMs >= LEAVE_AFTER_LOGOUT_MS - 500;
		log.debug("Partybase logout check: state={}, out for {} ms, setting={}, in party={}", state, outMs,
			config.leaveOnLogout(), partyLink.isInParty());
		if (stillOut && config.leaveOnLogout() && partyLink.isInParty())
		{
			changeParty(null);
		}
	}

	/** Closing the client: tell the party server you are gone, instead of leaving a stale member behind. */
	void clientClosed()
	{
		if (config.leaveOnLogout() && partyLink.isInParty())
		{
			changeParty(null);
		}
	}

	/** @param inRaid you are inside a raid */
	void tick(long now, boolean inRaid)
	{
		updateInstanceMates();
		watchOwnParty();
		rememberLastParty();
		updateRaidParty(inRaid);
		updateOwnParty();
		updateMembership(now);
	}

	private void updateMembership(long now)
	{
		List<Long> listed = new ArrayList<>();
		for (PartybaseMember m : partyLink.getMembers())
		{
			listed.add(m.getMemberId());
		}
		hidden.entrySet().removeIf(e -> now > e.getValue() || !listed.contains(e.getKey()));

		PartybaseMember local = partyLink.isInParty() ? partyLink.getLocalMember() : null;
		if (local == null)
		{
			if (leader.isJoined())
			{
				leader.left();
				// the mark belonged to that party
				marked.clear();
			}
			localMemberId = 0;
			notices.reset();
			partyStartMs = 0;
			leaderName = null;
			youLead = false;
			return;
		}
		if (local.getMemberId() != localMemberId)
		{
			// RuneLite gives you a new member id in every party
			localMemberId = local.getMemberId();
			leader.joined(localMemberId, now);
			marked.clear();
			send(PartybaseDpsPartyMessage.HI);
		}
		if (leader.tick(now, memberIds()) && leader.isLeader())
		{
			send(PartybaseDpsPartyMessage.LEADER);
		}
		partyStartMs = leader.partyStartMs(memberIds());
		youLead = leader.isLeader();
		long leaderId = leader.getLeader();
		leaderName = leaderId == 0 ? null : leaderId == localMemberId ? Names.localName(client) : memberName(leaderId);

		Map<Long, String> named = new HashMap<>();
		for (PartybaseMember m : partyLink.getMembers())
		{
			String n = Names.clean(m.getDisplayName());
			if (n != null && m.getMemberId() != localMemberId && !hidden.containsKey(m.getMemberId()))
			{
				named.put(m.getMemberId(), n);
			}
		}
		for (PartyNotices.Notice n : notices.tick(now, localMemberId, named))
		{
			if (n.joined)
			{
				chat.info(n.name + " joined the party.");
			}
			else
			{
				chat.left(n.name + " left the party.");
			}
		}
		List<String> others = notices.welcome(now);
		if (others != null)
		{
			chat.info(PartyNotices.welcomeText(partyLink.getPartyPassphrase(), others));
		}
	}

	private void updateInstanceMates()
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null || !wv.isInstance() || !partyLink.isInParty())
		{
			instanceMates.clear();
			return;
		}
		Set<String> party = new HashSet<>();
		for (PartybaseMember m : partyLink.getMembers())
		{
			String n = Names.clean(m.getDisplayName());
			if (n != null)
			{
				party.add(n);
			}
		}
		for (Player p : wv.players())
		{
			String n = p == null ? null : Names.clean(p.getName());
			if (n != null && party.contains(n))
			{
				instanceMates.add(n);
			}
		}
	}

	private void updateRaidParty(boolean inRaid)
	{
		if (!config.autoRaidParty())
		{
			return;
		}
		RaidPartyDetector.Detected raid = raidPartyDetector.current();
		String current = partyLink.isInParty() ? partyLink.getPartyPassphrase() : null;
		loadRememberedRaidParty();
		RaidParty.Action action = raidParty.tick(raid.partyName, current, inRaid);
		saveRememberedRaidParty();
		if (action.join != null)
		{
			changeParty(action.join);
			chat.info("you joined the " + raid.raid + " raid party automatically.");
		}
		else if (action.leave)
		{
			changeParty(null);
			chat.info("the raid is over: you left the raid party automatically.");
		}
	}

	/** After a client restart: the raid party you were in, if it is recent. */
	private void loadRememberedRaidParty()
	{
		if (rememberedLoaded)
		{
			return;
		}
		rememberedLoaded = true;
		String saved = configManager.getConfiguration(PartybaseConfig.STATE_GROUP, RAID_PARTY_KEY);
		int split = saved == null ? -1 : saved.lastIndexOf('|');
		if (split <= 0)
		{
			return;
		}
		try
		{
			long savedAt = Long.parseLong(saved.substring(split + 1));
			if (System.currentTimeMillis() - savedAt <= REMEMBER_MS)
			{
				raidParty.remember(saved.substring(0, split));
			}
		}
		catch (NumberFormatException ignored)
		{
			// an unreadable value is the same as none
		}
		rememberedSaved = raidParty.getRemembered();
	}

	private void saveRememberedRaidParty()
	{
		String name = raidParty.getRemembered();
		if (Objects.equals(name, rememberedSaved))
		{
			return;
		}
		rememberedSaved = name;
		if (name == null)
		{
			configManager.unsetConfiguration(PartybaseConfig.STATE_GROUP, RAID_PARTY_KEY);
		}
		else
		{
			configManager.setConfiguration(PartybaseConfig.STATE_GROUP, RAID_PARTY_KEY,
				name + "|" + System.currentTimeMillis());
		}
	}

	/** A party lost without Partybase (RuneLite's Party panel, a dropped connection) stops own parties until logout. */
	private void watchOwnParty()
	{
		String current = partyLink.isInParty() ? partyLink.getPartyPassphrase() : null;
		boolean lostByOthers = lastParty != null && !lastParty.equals(current) && !changedByUs;
		// a party you lose (left in RuneLite's panel, or a dropped connection) must not be swapped
		// for an own party behind your back: you may want to join your friends again
		if (lostByOthers && (current == null || RaidParty.isOwn(lastParty)))
		{
			ownPartyStopped = true;
		}
		lastParty = current;
		changedByUs = false;
	}

	/** You hit a boss: the fight timer runs. */
	void bossHit()
	{
		ownPartyWanted = true;
	}

	/** Setting "Own party": when a boss fight starts and you are in no party, make one just for you. */
	private void updateOwnParty()
	{
		boolean wanted = ownPartyWanted;
		ownPartyWanted = false;
		if (!wanted || !config.ownParty() || ownPartyStopped || partyLink.isInParty()
			|| client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		changeParty(RaidParty.OWN_PREFIX + partyLink.generatePassphrase().replace('-', ' '));
		chat.info("you are in your own party now, so boss reports also work solo (setting: Own party).");
	}

	DpsPartyPanel.View view(String localName)
	{
		DpsPartyPanel.View v = new DpsPartyPanel.View();
		v.inParty = partyLink.isInParty();
		v.lastParty = lastParty();
		if (!v.inParty)
		{
			return v;
		}
		v.code = partyLink.getPartyPassphrase();
		v.youLead = leader.isLeader();
		v.autoLabel = RaidParty.isOwn(v.code) ? "Own party" : RaidParty.isAuto(v.code) ? "Raid party" : null;

		long now = System.currentTimeMillis();
		PartybaseMember local = partyLink.getLocalMember();
		for (PartybaseMember m : partyLink.getMembers())
		{
			if (hidden.containsKey(m.getMemberId()))
			{
				continue;
			}
			DpsPartyPanel.Member row = new DpsPartyPanel.Member();
			row.id = m.getMemberId();
			row.you = local != null && m.getMemberId() == local.getMemberId();
			String name = Names.clean(m.getDisplayName());
			// no name and logged out: a connection the party server has not dropped yet (a closed client)
			if (!row.you && name == null && !m.isLoggedIn())
			{
				continue;
			}
			row.name = name != null ? name : row.you ? localName : "(joining...)";
			row.online = m.isLoggedIn();
			row.leader = m.getMemberId() == leader.getLeader();
			row.hasPlugin = row.you || leader.hasPlugin(m.getMemberId()) || (name != null && isReporter(name, now));
			v.members.add(row);
		}
		v.members.sort((a, b) -> a.leader != b.leader ? (a.leader ? -1 : 1)
			: a.you != b.you ? (a.you ? -1 : 1) : a.name.compareToIgnoreCase(b.name));
		return v;
	}

	private String memberNameOr(long memberId, String fallback)
	{
		String name = memberName(memberId);
		return name != null ? name : fallback;
	}

	/** You can mark a target: you lead, or you are in no party (solo). */
	boolean canMark()
	{
		return !inParty() || leader.isLeader();
	}

	/** Marks the NPC (or changes its icon) for you and, when you lead, for the party. */
	void mark(String npcKey, int npcId, String npcName, TargetIcon icon)
	{
		boolean isNew = !marked.is(npcKey);
		marked.set(npcKey, npcId, npcName, icon);
		if (isNew)
		{
			chat.info("marked " + npcName + " (" + icon.getLabel().toLowerCase() + "). It is tracked as a boss"
				+ " fight until it dies.");
		}
		sendMark(marked.get(npcKey));
	}

	/** Removes one mark, or all of them when npcKey is null. */
	void unmark(String npcKey)
	{
		if (npcKey == null)
		{
			marked.clear();
		}
		else
		{
			marked.remove(npcKey);
		}
		if (isLeader())
		{
			PartybaseDpsPartyMessage m = message(PartybaseDpsPartyMessage.UNMARK, 0);
			m.npcKey = npcKey;
			partyLink.send(m);
		}
	}

	private void sendMarks()
	{
		for (MarkedTarget.Mark mark : marked.all())
		{
			sendMark(mark);
		}
	}

	private void sendMark(MarkedTarget.Mark mark)
	{
		if (!isLeader() || mark == null)
		{
			return;
		}
		PartybaseDpsPartyMessage m = message(PartybaseDpsPartyMessage.MARK, 0);
		m.npcKey = mark.key;
		m.npcId = mark.npcId;
		m.npcName = mark.name;
		m.icon = mark.icon.name();
		partyLink.send(m);
	}

	/**
	 * You lead and hand it to another member who runs Partybase.
	 *
	 * @return false when you don't lead or they don't run Partybase
	 */
	boolean makeLeader(long memberId)
	{
		if (!inParty() || !leader.handOver(memberId))
		{
			if (isLeader())
			{
				chat.info(memberNameOr(memberId, "that member") + " does not run Partybase, so they can't lead.");
			}
			return false;
		}
		youLead = false;
		send(PartybaseDpsPartyMessage.LEADER);
		chat.info(memberNameOr(memberId, "they") + " is the party leader now.");
		return true;
	}

	/** Safe from any thread. */
	boolean youLead()
	{
		return youLead;
	}

	long getLeaderId()
	{
		return inParty() ? leader.getLeader() : 0;
	}

	long getPartyStartMs()
	{
		return partyStartMs;
	}

	String getLeaderName()
	{
		return leaderName;
	}

	private String memberName(long memberId)
	{
		PartybaseMember m = partyLink.getMemberById(memberId);
		return m == null ? null : Names.clean(m.getDisplayName());
	}

	private void send(String type)
	{
		if (inParty())
		{
			partyLink.send(message(type, 0));
		}
	}

	private PartybaseDpsPartyMessage message(String type, long target)
	{
		PartybaseDpsPartyMessage m = new PartybaseDpsPartyMessage(type, leader.getLeader(), target);
		m.since = leader.getJoinedMs();
		return m;
	}

	private List<Long> memberIds()
	{
		List<Long> ids = new ArrayList<>();
		for (PartybaseMember m : partyLink.getMembers())
		{
			if (!hidden.containsKey(m.getMemberId()))
			{
				ids.add(m.getMemberId());
			}
		}
		return ids;
	}
}
