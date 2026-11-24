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
