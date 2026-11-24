/*
 * Copyright (c) 2026, Matthew Loller <matthewloller@gmail.com>
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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Outlines each marked target in its icon colour and draws its icon above it. */
class MarkedTargetOverlay extends Overlay
{
	private static final int ICON_SIZE = 18;

	private final Client client;
	private final DpsModule dps;

	@Inject
	MarkedTargetOverlay(Client client, DpsModule dps)
	{
		this.client = client;
		this.dps = dps;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		List<MarkedTarget.Mark> marks = dps.marks();
		WorldView wv = marks.isEmpty() ? null : client.getTopLevelWorldView();
		if (wv == null)
		{
			return null;
		}
		Map<String, MarkedTarget.Mark> byKey = new HashMap<>();
		marks.forEach(m -> byKey.put(m.key, m));
		for (NPC npc : wv.npcs())
		{
			MarkedTarget.Mark mark = npc == null ? null : byKey.get(TargetMatcher.key(client.getWorld(), npc.getIndex()));
			if (mark != null)
			{
				draw(g, npc, mark.icon);
			}
		}
		return null;
	}

	private static void draw(Graphics2D g, NPC npc, TargetIcon icon)
	{
		Color color = icon.getColor();
		Shape hull = npc.getConvexHull();
		if (hull != null)
		{
			g.setStroke(new BasicStroke(2));
			g.setColor(color);
			g.draw(hull);
			g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
			g.fill(hull);
		}
		else
		{
			Polygon tile = npc.getCanvasTilePoly();
			if (tile != null)
			{
				OverlayUtil.renderPolygon(g, tile, color);
			}
		}
		// above the head, clear of the defence and hit chance lines
		Point at = npc.getCanvasTextLocation(g, "", npc.getLogicalHeight() + 90);
		if (at != null)
		{
			icon.draw(g, at.getX(), at.getY() - ICON_SIZE / 2.0, ICON_SIZE);
		}
	}
}
