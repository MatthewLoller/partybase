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

import java.util.Random;

/** Random hungry lines for the top healer: one line for a few seconds, then a pause. */
public class HungryTalk
{
	static final long SHOW_MS = 5000;
	static final long MIN_PAUSE_MS = 6000;
	static final long MAX_PAUSE_MS = 14000;

	static final String[] LINES =
	{
		"So hungry...",
		"Is that a shark?",
		"Anyone got fries?",
		"Nom nom nom",
		"I could eat a whole Vorkath",
		"Brb, eating",
		"Who ate my karambwan?",
		"One more burger won't hurt",
		"Snack time!",
		"Healing? I just love food",
	};

	private final Random random;
	private String line;
	private long showUntil;
	private long nextAt;

	public HungryTalk(Random random)
	{
		this.random = random;
	}

	/** The line to show now, or null during a pause. */
	public String line(long now)
	{
		if (now < showUntil)
		{
			return line;
		}
		if (now < nextAt)
		{
			return null;
		}
		String next;
		do
		{
			next = LINES[random.nextInt(LINES.length)];
		}
		while (next.equals(line));
		line = next;
		showUntil = now + SHOW_MS;
		nextAt = showUntil + MIN_PAUSE_MS + (long) (random.nextDouble() * (MAX_PAUSE_MS - MIN_PAUSE_MS));
		return line;
	}
}
