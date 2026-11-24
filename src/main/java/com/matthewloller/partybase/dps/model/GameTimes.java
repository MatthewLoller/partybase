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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The times the game prints in the chat when a boss or raid is done, so the meter shows the
 * same time as the game ("Fight duration: 0:54.60").
 */
public final class GameTimes
{
	private static final String TIME = "(\\d+(?::\\d{1,2}){0,2}(?:\\.\\d+)?)";

	/** "Theatre of Blood total completion time: 20:00.00", CoX "your raid is complete! ... Duration: 25:13.20". */
	private static final Pattern RAID = Pattern.compile(
			"(?i)(?:total completion time|raid is complete!.*?\\bduration):\\s*" + TIME);

	/**
	 * "Fight duration:", "Challenge duration:" (Gauntlet), "...complete! Duration:" (ToB rooms) and
	 * "Challenge complete: Path of Het. Duration:" (ToA rooms). Not "Wave duration" or "Lap duration".
	 */
	private static final Pattern BOSS = Pattern.compile(
			"(?i)(?:\\b(?:fight|challenge) duration|(?:^|[.!])\\s*duration):\\s*" + TIME);

	/** A time from the chat. */
	public static final class Time
	{
		public final long ms;
		/** The time of a whole raid, else of one boss. */
		public final boolean raid;

		Time(long ms, boolean raid)
		{
			this.ms = ms;
			this.raid = raid;
		}
	}

	private GameTimes()
	{
	}

	/** The time in this game message, or null. */
	public static Time parse(String message)
	{
		String text = ChatText.withoutTags(message);
		Matcher raid = RAID.matcher(text);
		if (raid.find())
		{
			return new Time(toMs(raid.group(1)), true);
		}
		Matcher boss = BOSS.matcher(text);
		return boss.find() ? new Time(toMs(boss.group(1)), false) : null;
	}

	/** "1:02:03.40", "0:54.60" or "54" to milliseconds. */
	static long toMs(String time)
	{
		String[] parts = time.split(":");
		double seconds = 0;
		for (String part : parts)
		{
			seconds = seconds * 60 + Double.parseDouble(part);
		}
		return Math.round(seconds * 1000);
	}
}
