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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The overlay's damage meter (and boss meter) drawn in the DPS tab. It repaints once per game tick while the tab
 * is shown. The overlay reads only snapshots the client thread made, so drawing it on the Swing thread is safe.
 * Swing thread only.
 */
class SidebarMeter extends JComponent
{
	private static final int REFRESH_MS = 600;
	/** Tall enough for 20 bars plus the boss meter. */
	private static final int MAX_HEIGHT = 800;

	private final DpsOverlay overlay;
	private final Timer timer = new Timer(REFRESH_MS, e -> refresh());
	private BufferedImage image;

	SidebarMeter(DpsOverlay overlay)
	{
		this.overlay = overlay;
		timer.setInitialDelay(0);
	}

	void setRunning(boolean running)
	{
		if (running)
		{
			timer.start();
		}
		else
		{
			timer.stop();
		}
	}

	private void refresh()
	{
		int width = getWidth() > 0 ? getWidth() : getParent() == null ? 0 : getParent().getWidth();
		if (width <= 0)
		{
			return;
		}
		BufferedImage canvas = new BufferedImage(width, MAX_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = canvas.createGraphics();
		Dimension size;
		try
		{
			// never compact: the sidebar has room for every column
			size = overlay.draw(g, width, false, false);
		}
		finally
		{
			g.dispose();
		}
		int height = size == null ? 0 : Math.min(size.height, MAX_HEIGHT);
		image = height == 0 ? null : canvas.getSubimage(0, 0, width, height);
		if (getPreferredSize().height != height)
		{
			setPreferredSize(new Dimension(0, height));
			revalidate();
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		if (image != null)
		{
			g.drawImage(image, 0, 0, null);
		}
	}
}
