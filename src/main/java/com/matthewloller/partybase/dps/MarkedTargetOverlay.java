package com.matthewloller.partybase.dps;

import com.matthewloller.partybase.dps.model.TargetMatcher;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Outlines the party's marked target and writes "Target" above it. */
class MarkedTargetOverlay extends Overlay
{
	private static final Color COLOR = new Color(255, 196, 0);
	private static final Color FILL = new Color(255, 196, 0, 30);

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
		String key = dps.markedKey();
		WorldView wv = key == null ? null : client.getTopLevelWorldView();
		if (wv == null)
		{
			return null;
		}
		for (NPC npc : wv.npcs())
		{
			if (npc != null && key.equals(TargetMatcher.key(client.getWorld(), npc.getIndex())))
			{
				draw(g, npc);
				break;
			}
		}
		return null;
	}

	private static void draw(Graphics2D g, NPC npc)
	{
		Shape hull = npc.getConvexHull();
		if (hull != null)
		{
			g.setStroke(new BasicStroke(2));
			g.setColor(COLOR);
			g.draw(hull);
			g.setColor(FILL);
			g.fill(hull);
		}
		else
		{
			Polygon tile = npc.getCanvasTilePoly();
			if (tile != null)
			{
				OverlayUtil.renderPolygon(g, tile, COLOR);
			}
		}
		String text = "Target";
		Point at = npc.getCanvasTextLocation(g, text, npc.getLogicalHeight() + 60);
		if (at != null)
		{
			OverlayUtil.renderTextLocation(g, at, text, COLOR);
		}
	}
}
