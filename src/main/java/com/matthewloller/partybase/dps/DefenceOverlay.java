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

import com.matthewloller.partybase.dps.model.TargetMatcher;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Draws above monsters their defence after special attacks lowered it, and your hit chance. */
class DefenceOverlay extends Overlay
{
	/** Above the health bar. */
	private static final int ABOVE_HEAD = 60;
	private static final int LINE_HEIGHT = 14;
	private static final Color DEFENCE_COLOR = new Color(255, 255, 0);

	private final Client client;
	private final DpsModule plugin;

	@Inject
	DefenceOverlay(Client client, DpsModule plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Map<String, String[]> labels = plugin.defenceLabels();
		WorldView wv = client.getTopLevelWorldView();
		if (labels.isEmpty() || wv == null)
		{
			return null;
		}
		g.setFont(FontManager.getRunescapeSmallFont());
		for (NPC npc : wv.npcs())
		{
			String[] lines = npc == null ? null : labels.get(TargetMatcher.key(client.getWorld(), npc.getIndex()));
			if (lines != null)
			{
				draw(g, npc, lines);
			}
		}
		return null;
	}

	/** The first line in yellow, the others in white below it. */
	private void draw(Graphics2D g, NPC npc, String[] lines)
	{
		int height = npc.getLogicalHeight() + ABOVE_HEAD;
		Point top = npc.getCanvasTextLocation(g, lines[0], height);
		if (top == null)
		{
			return;
		}
		for (int i = 0; i < lines.length; i++)
		{
			Point centered = npc.getCanvasTextLocation(g, lines[i], height);
			if (centered != null)
			{
				Point at = new Point(centered.getX(), top.getY() + i * LINE_HEIGHT);
				OverlayUtil.renderTextLocation(g, at, lines[i], i == 0 ? DEFENCE_COLOR : Color.WHITE);
			}
		}
	}
}
