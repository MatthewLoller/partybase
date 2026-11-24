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
import javax.inject.Inject;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

/** Local chat messages (only visible to you). */
class ChatNotifier
{
	private static final String PREFIX = "Partybase: ";
	private static final String LEAVE_COLOR = "c27c00";
	private static final String KICK_COLOR = "d42020";

	/** "<col=" plus six hex digits plus ">" */
	private static final int COLOR_TAG_LENGTH = 12;

	private final Client client;
	private final ChatMessageManager chatMessageManager;
	private final PartybaseConfig config;

	@Inject
	ChatNotifier(Client client, ChatMessageManager chatMessageManager, PartybaseConfig config)
	{
		this.client = client;
		this.chatMessageManager = chatMessageManager;
		this.config = config;
	}

	/** Info and good news (joined, raid started) in the color the player picked; leaving and kicks keep theirs. */
	void info(String text)
	{
		send(colored(chatHex(), PREFIX + text));
	}

	void left(String text)
	{
		send(colored(LEAVE_COLOR, PREFIX + text));
	}

	void kicked(String text)
	{
		send(colored(KICK_COLOR, PREFIX + text));
	}

	/** The chat color changed: give the info messages already in the chatbox the new color. Client thread. */
	void recolorOldMessages()
	{
		ChatLineBuffer buffer = client.getChatLineMap().get(ChatMessageType.CONSOLE.getType());
		if (buffer == null)
		{
			return;
		}
		MessageNode[] lines = buffer.getLines();
		for (int i = 0; i < buffer.getLength(); i++)
		{
			String text = lines[i] == null ? null : lines[i].getRuneLiteFormatMessage();
			if (isInfo(text))
			{
				lines[i].setRuneLiteFormatMessage("<col=" + chatHex() + ">" + text.substring(COLOR_TAG_LENGTH));
			}
		}
		client.refreshChat();
	}

	/** A Partybase message in the chat color (not a leave or kick, which keep their own colors). */
	static boolean isInfo(String text)
	{
		return text != null && text.startsWith("<col=") && text.startsWith(PREFIX, COLOR_TAG_LENGTH)
			&& !text.startsWith(LEAVE_COLOR, 5) && !text.startsWith(KICK_COLOR, 5);
	}

	private String chatHex()
	{
		return String.format("%06x", config.chatColor().getRGB() & 0xffffff);
	}

	private static String colored(String hex, String text)
	{
		return "<col=" + hex + ">" + text + "</col>";
	}

	private void send(String text)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(text)
			.build());
	}
}
