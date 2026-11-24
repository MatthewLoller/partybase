package com.matthewloller.partybase.dps.model;

public enum PlayerKind
{
	LOCAL,
	/** Party member running Partybase. */
	PARTY,
	/** Player without Partybase; damage is estimated from attack animations. */
	ESTIMATE,
	/** Damage nobody reported. */
	UNKNOWN,
	/** Poison, venom, burn and bleed. */
	EFFECTS
}
