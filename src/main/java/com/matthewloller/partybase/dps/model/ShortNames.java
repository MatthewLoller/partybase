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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Short names for the compact meter: two letters, more where two players would look the same. */
public final class ShortNames
{
	private static final int LETTERS = 2;

	private ShortNames()
	{
	}

	/** "Franz" and "Bob" give "Fr" and "Bo"; "Franz" and "Frank" grow until they differ: "Franz", "Frank". */
	public static Map<String, String> of(Collection<String> names)
	{
		Map<String, Integer> length = new HashMap<>();
		names.forEach(n -> length.put(n, LETTERS));
		boolean grew = true;
		while (grew)
		{
			grew = false;
			Map<String, List<String>> byShort = names.stream().distinct()
				.collect(Collectors.groupingBy(n -> cut(n, length.get(n))));
			for (List<String> same : byShort.values())
			{
				if (same.size() < 2)
				{
					continue;
				}
				for (String n : same)
				{
					if (length.get(n) < n.length())
					{
						length.put(n, length.get(n) + 1);
						grew = true;
					}
				}
			}
		}
		Map<String, String> out = new HashMap<>();
		names.forEach(n -> out.put(n, cut(n, length.get(n))));
		return out;
	}

	private static String cut(String name, int letters)
	{
		return name.substring(0, Math.min(letters, name.length()));
	}
}
