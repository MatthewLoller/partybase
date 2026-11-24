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
package com.matthewloller.partybase.dps.model;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The CoX points the game tells at the end of a raid ("Total points: 31,408, Personal points: 31,408 (100%)"). */
public class RaidPoints
{
	private static final Pattern POINTS = Pattern.compile(
			"^Total points: ([\\d,]+), Personal points: ([\\d,]+)");

	private long total;
	private long personal;
	private long atMs;

	/** A game message. */
	public void message(long now, String text)
	{
		Matcher m = POINTS.matcher(ChatText.withoutTags(text));
		if (m.find())
		{
			total = Long.parseLong(m.group(1).replace(",", ""));
			personal = Long.parseLong(m.group(2).replace(",", ""));
			atMs = now;
		}
	}

	/** "31,408 pts", or "12,000 of 31,408 pts" in a group; null when none were told close to {@code atMs}. */
	public String near(long atMs)
	{
		if (this.atMs == 0 || Math.abs(this.atMs - atMs) > KillCounts.NEAR_MS)
		{
			return null;
		}
		return personal == total ? exact(total) + " pts" : exact(personal) + " of " + exact(total) + " pts";
	}

	/** Points are shown exactly, as the game does: 31,408 (not 31.4k). */
	private static String exact(long points)
	{
		return String.format(Locale.US, "%,d", points);
	}
}
