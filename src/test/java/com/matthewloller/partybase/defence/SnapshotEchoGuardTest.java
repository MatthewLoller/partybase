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

import static org.junit.Assert.*;
import org.junit.Test;

public class SnapshotEchoGuardTest
{
	@Test
	public void creditIsConsumedOnlyOnce()
	{
		SnapshotEchoGuard<String> guard = new SnapshotEchoGuard<>(2);
		guard.add("boss1/Alice/BGS/50", 10);
		assertTrue(guard.consume("boss1/Alice/BGS/50", 10));
		assertFalse(guard.consume("boss1/Alice/BGS/50", 11));
	}
	@Test
	public void repeatedNewRowsReceiveSeparateCredits()
	{
		SnapshotEchoGuard<String> guard = new SnapshotEchoGuard<>(2);
		guard.add("same", 10);
		guard.add("same", 10);
		assertTrue(guard.consume("same", 11));
		assertTrue(guard.consume("same", 11));
		assertFalse(guard.consume("same", 11));
	}
	@Test
	public void anotherTargetAndExpiredEchoCannotSuppressAHit()
	{
		SnapshotEchoGuard<String> guard = new SnapshotEchoGuard<>(2);
		guard.add("boss1", 10);
		assertFalse(guard.consume("boss2", 10));
		assertFalse(guard.consume("boss1", 13));
	}
	@Test
	public void resetAndTickRollbackDiscardCredits()
	{
		SnapshotEchoGuard<String> guard = new SnapshotEchoGuard<>(2);
		guard.add("a", 10);
		guard.clear();
		assertFalse(guard.consume("a", 11));
		guard.add("a", 10);
		assertFalse(guard.consume("a", 1));
	}
}
