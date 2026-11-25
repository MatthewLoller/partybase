/*
 * Copyright (c) 2022, TrevorMDev <https://github.com/TrevorMDev>
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
package com.matthewloller.partybase.tiles;

import com.google.common.base.Strings;
import com.matthewloller.partybase.PartybasePlugin;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Draws the shared tile packs that are turned on, the way Tile Packs draws its tiles (ported from it). */
class SharedTilesOverlay extends Overlay
{
	private static final int MAX_DRAW_DISTANCE = 32;
	private static final Stroke BORDER = new BasicStroke(2f);
	private static final Color FILL = new Color(0, 0, 0, 50);

	private final Client client;
	private final TileModule tiles;

	@Inject
	SharedTilesOverlay(Client client, TileModule tiles, PartybasePlugin owner)
	{
		super(owner);
		this.client = client;
		this.tiles = tiles;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<TileModule.DrawTile> list = tiles.drawTiles();
		Player player = client.getLocalPlayer();
		if (list.isEmpty() || player == null)
		{
			return null;
		}
		WorldPoint playerLocation = player.getWorldLocation();
		boolean topLevel = player.getWorldView().isTopLevel();
		for (TileModule.DrawTile t : list)
		{
			if (t.point.getPlane() != t.worldView.getPlane())
			{
				continue;
			}
			if (topLevel && t.point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE)
			{
				continue;
			}
			LocalPoint lp = LocalPoint.fromWorld(t.worldView, t.point);
			if (lp == null)
			{
				continue;
			}
			Polygon poly = Perspective.getCanvasTilePoly(client, lp);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, t.color, FILL, BORDER);
			}
			if (!Strings.isNullOrEmpty(t.label))
			{
				Point at = Perspective.getCanvasTextLocation(client, graphics, lp, t.label, 0);
				if (at != null)
				{
					OverlayUtil.renderTextLocation(graphics, at, t.label, t.color);
				}
			}
		}
		return null;
	}
}
