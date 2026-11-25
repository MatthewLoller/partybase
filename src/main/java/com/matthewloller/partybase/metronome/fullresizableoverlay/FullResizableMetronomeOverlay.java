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
package com.matthewloller.partybase.metronome.fullresizableoverlay;

import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.PartybasePlugin;
import com.matthewloller.partybase.metronome.FontTypes;
import com.matthewloller.partybase.metronome.MetronomeModule;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public abstract class FullResizableMetronomeOverlay extends Overlay
{
	protected final PartybaseConfig config;
	protected final MetronomeModule plugin;

	private static final int MINIMUM_SIZE = 16;

	protected FullResizableMetronomeOverlay(PartybaseConfig config, MetronomeModule plugin, PartybasePlugin owner)
	{
		// the owner plugin makes the overlay belong to Partybase (Configure opens its settings)
		super(owner);
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setMinimumSize(MINIMUM_SIZE);
		setResizable(true);
	}

	protected abstract boolean isVisible();

	protected abstract String getTickText();

	protected abstract Color getTextColor();

	protected boolean showText()
	{
		return true;
	}

	protected boolean showBackgroundColor()
	{
		return true;
	}

	@Override
	public final Dimension render(Graphics2D graphics)
	{
		Dimension preferredSize = getPreferredSize();
		if (preferredSize == null)
		{
			preferredSize = new Dimension(config.boxWidth(), config.boxWidth());
			setPreferredSize(preferredSize);
		}

		if (isVisible())
		{
			if (showBackgroundColor())
			{
				graphics.setColor(plugin.getCurrentColor());
				graphics.fillRect(0, 0, preferredSize.width, preferredSize.height);
			}

			if (showText())
			{
				final String text = getTickText();
				if (config.disableFontScaling())
				{
					final int padding = Math.min(preferredSize.width, preferredSize.height) / 2 - 4;
					graphics.setColor(getTextColor());
					graphics.drawString(text, padding, preferredSize.height - padding);
				}
				else
				{
					final Font baseFont = (config.fontType() == FontTypes.REGULAR)
						? new Font(FontManager.getRunescapeFont().getName(), Font.PLAIN, 1)
						: new Font(config.fontType().toString(), Font.PLAIN, 1);
					graphics.setFont(getBestFitFont(graphics, baseFont, text, preferredSize.width, preferredSize.height));
					final Point center = getCenteredTextPoint(graphics, text, preferredSize.width, preferredSize.height);
					OverlayUtil.renderTextLocation(graphics, center, text, getTextColor());
				}
			}
		}

		return preferredSize;
	}

	private Font getBestFitFont(Graphics2D graphics, Font baseFont, String text, int boxWidth, int boxHeight)
	{
		final int maxSize = Math.max(MINIMUM_SIZE, Math.min(boxWidth, boxHeight));
		final int horizontalPadding = Math.max(2, boxWidth / 12);
		final int verticalPadding = Math.max(2, boxHeight / 12);
		final int availableWidth = Math.max(1, boxWidth - horizontalPadding);
		final int availableHeight = Math.max(1, boxHeight - verticalPadding);

		final Font maxFont = baseFont.deriveFont((float) maxSize);
		final FontMetrics maxMetrics = graphics.getFontMetrics(maxFont);
		final Rectangle maxBounds = maxMetrics.getStringBounds(text, graphics).getBounds();
		final int maxTextHeight = maxMetrics.getAscent() + maxMetrics.getDescent();

		final double widthScale = (maxBounds.width <= 0) ? 1.0 : (double) availableWidth / maxBounds.width;
		final double heightScale = (maxTextHeight <= 0) ? 1.0 : (double) availableHeight / maxTextHeight;
		final double fitScale = Math.min(1.0, Math.min(widthScale, heightScale));
		final double sizeBoost = (config.fontType() == FontTypes.SEGOE_UI) ? 1.45 : 1.2;
		final int fittedSize = Math.min(
				maxSize,
				Math.max(MINIMUM_SIZE, (int) Math.floor(maxSize * fitScale * sizeBoost))
			);

		return baseFont.deriveFont((float) fittedSize);
	}

	private Point getCenteredTextPoint(Graphics2D graphics, String text, int boxWidth, int boxHeight)
	{
		final FontMetrics metrics = graphics.getFontMetrics();
		final Rectangle textBounds = metrics.getStringBounds(text, graphics).getBounds();
		final int textX = (boxWidth - textBounds.width) / 2;
		final int visualNudgeDown = (config.fontType() == FontTypes.REGULAR) ? Math.max(1, boxHeight / 8) : 0;
		final int textY = (boxHeight - (metrics.getAscent() + metrics.getDescent())) / 2 + metrics.getAscent() + visualNudgeDown;
		return new Point(textX, textY);
	}
}
