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
