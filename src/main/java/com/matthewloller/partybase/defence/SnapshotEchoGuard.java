/*
 * Copyright (c) 2026, Heisty <https://github.com/Falsary>
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
package com.matthewloller.partybase.defence;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

// Ignore one duplicate party message for each adopted snapshot row.
final class SnapshotEchoGuard<K>
{
	private final Map<K, ArrayDeque<Integer>> credits = new HashMap<>();
	private final int ttl;

	SnapshotEchoGuard(int ttl)
	{
		this.ttl = ttl;
	}

	void add(K key, int tick)
	{
		credits.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(tick);
	}

	boolean consume(K key, int tick)
	{
		prune(tick);
		ArrayDeque<Integer> queue = credits.get(key);
		if (queue == null)
		{
			return false;
		}
		queue.removeFirst();
		if (queue.isEmpty())
		{
			credits.remove(key);
		}
		return true;
	}

	void prune(int tick)
	{
		credits.entrySet().removeIf(entry ->
		{
			ArrayDeque<Integer> queue = entry.getValue();
			while (!queue.isEmpty() && (tick < queue.peekFirst() || tick - queue.peekFirst() > ttl))
			{
				queue.removeFirst();
			}
			return queue.isEmpty();
		});
	}

	void clear()
	{
		credits.clear();
	}
}
