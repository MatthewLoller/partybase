package com.matthewloller.partybase.dps;

import com.google.gson.annotations.SerializedName;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Party control, since RuneLite parties have no leader or kick:
 * <ul>
 * <li>{@code hi}: a member runs Partybase and names the leader it knows (0 if none)</li>
 * <li>{@code leader}: the leader announces itself</li>
 * <li>{@code kick}: the leader asks the {@link #target} member to leave</li>
 * <li>{@code bye}: the member leaves now (the party server keeps listing it for minutes)</li>
 * <li>{@code here}: answer to {@code hi} from members who don't lead, so the new member learns join times</li>
 * <li>{@code mark}: the leader marks the NPC {@link #npcKey} as the party's target</li>
 * <li>{@code unmark}: the leader clears the marked target</li>
 * </ul>
 * Every message carries {@link #since}, when the sender joined the party: the earliest one is the party's age,
 * and the member who joined first leads when nobody says who does.
 * The party server identifies messages by class name, which must therefore never change.
 */
public class PartybaseDpsPartyMessage extends PartyMemberMessage
{
	static final String HI = "hi";
	static final String LEADER = "leader";
	static final String KICK = "kick";
	static final String BYE = "bye";
	static final String HERE = "here";
	static final String MARK = "mark";
	static final String UNMARK = "unmark";

	@SerializedName("v")
	public int version = 1;
	public String type;
	public long leader;
	public long target;
	/** When the sender joined the party, epoch ms on its clock; 0 = unknown. */
	public long since;
	/** Marked target: world:npcIndex, see TargetMatcher.key. */
	public String npcKey;
	public int npcId;
	public String npcName;

	public PartybaseDpsPartyMessage()
	{
	}

	PartybaseDpsPartyMessage(String type, long leader, long target)
	{
		this.type = type;
		this.leader = leader;
		this.target = target;
	}
}
