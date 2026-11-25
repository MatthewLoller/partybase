/*
 * Copyright (c) 2020, vincent0955 <https://github.com/vincent0955>
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
package com.matthewloller.partybase.metronome;

import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.PartybasePlugin;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;


public class MetronomeNumberOverlay extends Overlay
{

	private final Client client;
	private final PartybaseConfig config;
	private final MetronomeModule plugin;

	@Inject
	public MetronomeNumberOverlay(Client client, PartybaseConfig config, MetronomeModule plugin, PartybasePlugin owner)
	{
		// the owner plugin makes the overlay belong to Partybase (Configure opens its settings)
		super(owner);
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		setPriority(OverlayPriority.MED);

	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.showPlayerTick())
		{
			if (config.fontType() == FontTypes.REGULAR)
			{
				graphics.setFont(new Font(FontManager.getRunescapeFont().getName(), Font.PLAIN, config.fontSize()));
			}
			else
			{
				graphics.setFont(new Font(config.fontType().toString(), Font.PLAIN, config.fontSize()));
			}

			// Set which color to use
			Color numberColor;
			if (config.overheadUseCurrentColor())
			{
				numberColor = plugin.currentColor;
			}
			else
			{
				numberColor = config.NumberColor();
			}

			final int height = client.getLocalPlayer().getLogicalHeight() + config.overheadHeight();
			final LocalPoint localLocation = client.getLocalPlayer().getLocalLocation();
			final Point playerPoint = Perspective.localToCanvas(client, localLocation, client.getPlane(), height);
			final int valueX = playerPoint.getX() + config.overheadXCenterOffset();
			final int valueY = playerPoint.getY();
			final Point tickPoint = new Point(valueX, valueY);

			if (config.tickCount() == 1)
			{
				OverlayUtil.renderTextLocation(graphics, tickPoint, String.valueOf(plugin.currentColorIndex), numberColor);
			}
			else
			{
				OverlayUtil.renderTextLocation(graphics, tickPoint, String.valueOf(plugin.tickCounter), numberColor);
			}

			if (config.enableCycle2() && config.showCycle2Overhead())
			{
				final int valueX2 = valueX - config.overheadCyclesGapDistance();
				final Point tick2Point = new Point(valueX2, valueY);
				OverlayUtil.renderTextLocation(graphics, tick2Point, String.valueOf(plugin.tickCounter2), config.cycle2Color());
			}

			if (config.enableCycle3() && config.showCycle3Overhead())
			{
				final int valueX3 = valueX + config.overheadCyclesGapDistance();
				final Point tick3Point = new Point(valueX3, valueY);
				OverlayUtil.renderTextLocation(graphics, tick3Point, String.valueOf(plugin.tickCounter3), config.cycle3Color());
			}

		}
		return null;
	}

}


