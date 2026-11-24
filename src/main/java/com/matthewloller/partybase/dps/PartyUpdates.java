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

import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.dps.model.DefenceDrains;
import com.matthewloller.partybase.dps.model.Fight;
import com.matthewloller.partybase.dps.model.FightTracker;
import com.matthewloller.partybase.dps.model.HealType;
import com.matthewloller.partybase.dps.model.HealthWatcher;
import com.matthewloller.partybase.dps.model.PlayerKind;
import com.matthewloller.partybase.dps.model.TargetMatcher;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;

/**
 * The {@link PartybaseDpsUpdate} of every tick: collects what you did, sends it to the party, and
 * counts the updates that other members send. Client thread only.
 */
class PartyUpdates
{
	/** Other clients send the source as free text; anything this long is not a weapon or spell name. */
	private static final int MAX_SOURCE_LENGTH = 64;

	private final Client client;
	private final PartyService partyService;
	private final PartyManager party;
	private final PartybaseConfig config;
	private final FightTracker tracker;
	private final Bosses bosses;
	private final TargetMatcher matcher;
	private final DefenceDrains defence;

	private PartybaseDpsUpdate outgoing = new PartybaseDpsUpdate();

	PartyUpdates(Client client, PartyService partyService, PartyManager party, PartybaseConfig config,
		FightTracker tracker, Bosses bosses, TargetMatcher matcher, DefenceDrains defence)
	{
		this.client = client;
		this.partyService = partyService;
		this.party = party;
		this.config = config;
		this.tracker = tracker;
		this.bosses = bosses;
		this.matcher = matcher;
		this.defence = defence;
	}

	void addHit(PartybaseDpsUpdate.Hit hit)
	{
		outgoing.hits.add(hit);
	}

	void addDamageTaken(int amount)
	{
		outgoing.taken += amount;
	}

	void addHeal(HealthWatcher.Heal heal)
	{
		switch (heal.type)
		{
			case FOOD:
				outgoing.food += heal.amount;
				outgoing.foodOverheal += heal.overheal;
				break;
			case LIFESTEAL:
				outgoing.lifesteal += heal.amount;
				outgoing.lifestealOverheal += heal.overheal;
				break;
			case REGEN:
				// not counted any more, still sent for older Partybase versions
				outgoing.regen += heal.amount;
				break;
			case HEAL_OTHER:
				outgoing.healOther += heal.amount;
				break;
			default:
				break;
		}
	}

	/** Forgets what was collected but not sent yet. */
	void clear()
	{
		outgoing = new PartybaseDpsUpdate();
	}

	/** Sends this tick's update (when there is something to share) and starts the next one. */
	void send()
	{
		PartybaseDpsUpdate update = outgoing;
		outgoing = new PartybaseDpsUpdate();
		if (update.isEmpty() || !config.shareWithParty() || !party.inParty())
		{
			return;
		}
		update.world = client.getWorld();
		update.name = Names.localName(client);
		partyService.send(update);
	}

	/** Adds a party member's update to the fight. */
	void receive(PartybaseDpsUpdate update)
	{
		PartyMember local = partyService.getLocalMember();
		if (local != null && update.getMemberId() == local.getMemberId())
		{
			return;
		}
		party.pluginSeen(update.getMemberId());
		String name = senderName(update);
		if (name == null || name.equals(Names.localName(client)))
		{
			return;
		}

		long now = System.currentTimeMillis();
		party.reporterSeen(name, now);
		boolean together = !config.othersOnlyTogether() || party.isInstanceMate(name);
		boolean counted = together;
		if (update.hits != null)
		{
			for (PartybaseDpsUpdate.Hit hit : update.hits)
			{
				// party members on an older Partybase send the name with its color tags
				String npcName = Names.clean(hit.npcName);
				boolean boss = hit.boss || bosses.isBoss(hit.npcId, npcName);
				// away from you, only hits on a boss you can see count
				if (!together && !(boss && npcInScene(update.world, hit) != null))
				{
					continue;
				}
				counted = true;
				String source = hit.source == null || hit.source.length() > MAX_SOURCE_LENGTH ? null : hit.source;
				String targetKey = partyTargetKey(update.world, hit, name);
				tracker.playerHit(now, name, PlayerKind.PARTY, targetKey, hit.npcId, npcName, boss, hit.amount, hit.miss,
					source);
				defence.hit(client.getTickCount(), targetKey, hit.npcId, npcName, name, source, hit.amount, hit.miss);
			}
		}
		Fight current = tracker.getCurrent();
		if (!counted && (current == null || !current.players.containsKey(name)))
		{
			return;
		}
		tracker.damageTaken(now, name, PlayerKind.PARTY, update.taken);
		tracker.heal(now, name, PlayerKind.PARTY, HealType.FOOD, update.food, update.foodOverheal);
		tracker.heal(now, name, PlayerKind.PARTY, HealType.LIFESTEAL, update.lifesteal, update.lifestealOverheal);
		tracker.heal(now, name, PlayerKind.PARTY, HealType.HEAL_OTHER, update.healOther);
	}

	/** The name in the update, or the member's party display name when the update has none. */
	private String senderName(PartybaseDpsUpdate update)
	{
		String name = Names.clean(update.name);
		if (name != null)
		{
			return name;
		}
		PartyMember member = partyService.getMemberById(update.getMemberId());
		return member == null ? null : Names.clean(member.getDisplayName());
	}

	/** The NPC in your scene that a party member hit, or null when you cannot see it. */
	private NPC npcInScene(int world, PartybaseDpsUpdate.Hit hit)
	{
		WorldView wv = client.getTopLevelWorldView();
		if (world != client.getWorld() || wv == null)
		{
			return null;
		}
		NPC npc = wv.npcs().byIndex(hit.npcIndex);
		return npc != null && npc.getId() == hit.npcId ? npc : null;
	}

	/** Your key for the NPC when it is the same one you see, otherwise a key of its own. */
	private String partyTargetKey(int world, PartybaseDpsUpdate.Hit hit, String sender)
	{
		NPC npc = npcInScene(world, hit);
		WorldPoint pos = npc == null ? null : npc.getWorldLocation();
		if (pos != null)
		{
			matcher.seen(TargetMatcher.key(world, hit.npcIndex), pos.getX(), pos.getY(), pos.getPlane());
		}
		return matcher.partyKey(client.getWorld(), world, hit.npcIndex, hit.x, hit.y, hit.plane, sender);
	}
}
