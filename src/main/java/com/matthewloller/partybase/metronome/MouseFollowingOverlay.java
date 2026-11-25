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
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

public class MouseFollowingOverlay extends Overlay
{
	private final Client client;
	private final PartybaseConfig config;
	private final MetronomeModule plugin;

	@Inject
	public MouseFollowingOverlay(Client client, PartybaseConfig config, MetronomeModule plugin, PartybasePlugin owner)
	{
		// the owner plugin makes the overlay belong to Partybase (Configure opens its settings)
		super(owner);
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.mouseFollowingTick())
		{
			Point mousePos = client.getMouseCanvasPosition();
			if (mousePos != null)
			{
				// Set font
				Font font = config.fontType() == FontTypes.REGULAR
					? FontManager.getRunescapeFont().deriveFont(Font.PLAIN, config.fontSize())
					: new Font(config.fontType().toString(), Font.PLAIN, config.fontSize());
				graphics.setFont(font);

				// Set text
				String text = config.tickCount() == 1
					? String.valueOf(plugin.currentColorIndex)
					: String.valueOf(plugin.tickCounter);

				// Apply configurable offsets using getX()/getY()
				Point textPosition = new Point(
					(int)mousePos.getX() + config.mouseOffsetX(),
					(int)mousePos.getY() + config.mouseOffsetY()
				);

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

				// Render
				OverlayUtil.renderTextLocation(graphics, textPosition, text, numberColor);
			}
		}
		return null;
	}
}