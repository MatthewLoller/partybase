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

public final class Format
{
	private Format()
	{
	}

	/** 950, 12.3k, 1.25m */
	public static String amount(double v)
	{
		if (v >= 1_000_000)
		{
			return String.format(Locale.US, "%.2fm", v / 1_000_000);
		}
		if (v >= 10_000)
		{
			return String.format(Locale.US, "%.1fk", v / 1_000);
		}
		return String.format(Locale.US, "%,d", Math.round(v));
	}

	/** DPS with one decimal: 7.4 */
	public static String dps(double v)
	{
		return String.format(Locale.US, "%.1f", v);
	}

	public static String percent(double v)
	{
		return String.format(Locale.US, "%.0f%%", v);
	}

	/** "90% (255/284)", or "-" without attacks. */
	public static String accuracy(int hits, int misses)
	{
		int attacks = hits + misses;
		return attacks == 0 ? "-" : String.format(Locale.US, "%.0f%% (%d/%d)", 100.0 * hits / attacks, hits, attacks);
	}

	/** 0:42, 12:05, 1:02:03 */
	public static String duration(long ms)
	{
		long s = ms / 1000;
		long h = s / 3600;
		long m = (s % 3600) / 60;
		long sec = s % 60;
		if (h > 0)
		{
			return String.format(Locale.US, "%d:%02d:%02d", h, m, sec);
		}
		return String.format(Locale.US, "%d:%02d", m, sec);
	}

	/** Like the game's own timers, to a hundredth of a second: 0:54.60, 12:05.40, 1:02:03.00 */
	public static String preciseDuration(long ms)
	{
		long hundredths = Math.round(ms / 10.0);
		return duration(hundredths / 100 * 1000) + String.format(Locale.US, ".%02d", hundredths % 100);
	}
}
