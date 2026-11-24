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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/** The icons the leader can give a marked target, drawn with shapes (no images needed). */
enum TargetIcon
{
	STAR("Star", new Color(255, 214, 0)),
	CIRCLE("Circle", new Color(255, 140, 0)),
	DIAMOND("Diamond", new Color(200, 80, 230)),
	TRIANGLE("Triangle", new Color(60, 210, 60)),
	MOON("Moon", new Color(190, 210, 235)),
	SQUARE("Square", new Color(40, 150, 255)),
	CROSS("Cross", new Color(230, 40, 40)),
	SKULL("Skull", new Color(245, 245, 245));

	private static final Color OUTLINE = new Color(0, 0, 0, 200);

	private final String label;
	private final Color color;

	TargetIcon(String label, Color color)
	{
		this.label = label;
		this.color = color;
	}

	String getLabel()
	{
		return label;
	}

	Color getColor()
	{
		return color;
	}

	/** The icon by its saved or sent name; STAR for unknown names. */
	static TargetIcon of(String name)
	{
		for (TargetIcon icon : values())
		{
			if (icon.name().equals(name))
			{
				return icon;
			}
		}
		return STAR;
	}

	/** The first icon none of these marks use, so new marks get a different one. */
	static TargetIcon firstUnused(Iterable<TargetIcon> used)
	{
		for (TargetIcon icon : values())
		{
			boolean taken = false;
			for (TargetIcon u : used)
			{
				taken |= u == icon;
			}
			if (!taken)
			{
				return icon;
			}
		}
		return STAR;
	}

	/** A small picture for menus and the panel. */
	ImageIcon image(int size)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try
		{
			draw(g, size / 2.0, size / 2.0, size - 2);
		}
		finally
		{
			g.dispose();
		}
		return new ImageIcon(img);
	}

	/** Draws the icon centred on cx, cy, about size pixels wide, with a dark outline. */
	void draw(Graphics2D g, double cx, double cy, double size)
	{
		Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Shape shape = shape(cx, cy, size / 2.0);
		g.setColor(color);
		g.fill(shape);
		g.setColor(OUTLINE);
		g.setStroke(new BasicStroke(1.2f));
		g.draw(shape);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
	}

	private Shape shape(double cx, double cy, double r)
	{
		switch (this)
		{
			case STAR:
				return star(cx, cy, r, r * 0.45, 5);
			case CIRCLE:
				return new Ellipse2D.Double(cx - r * 0.85, cy - r * 0.85, r * 1.7, r * 1.7);
			case DIAMOND:
				return polygon(new double[] {cx, cx + r * 0.75, cx, cx - r * 0.75}, new double[] {cy - r, cy, cy + r, cy});
			case TRIANGLE:
				return polygon(new double[] {cx, cx + r, cx - r}, new double[] {cy - r, cy + r * 0.8, cy + r * 0.8});
			case MOON:
			{
				Area moon = new Area(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
				moon.subtract(new Area(new Ellipse2D.Double(cx - r * 0.35, cy - r * 1.15, r * 1.9, r * 1.9)));
				return moon;
			}
			case SQUARE:
				return new Rectangle2D.Double(cx - r * 0.8, cy - r * 0.8, r * 1.6, r * 1.6);
			case CROSS:
			{
				double t = r * 0.32;
				Area cross = new Area(new Rectangle2D.Double(cx - r, cy - t, r * 2, t * 2));
				cross.add(new Area(new Rectangle2D.Double(cx - t, cy - r, t * 2, r * 2)));
				cross.transform(java.awt.geom.AffineTransform.getRotateInstance(Math.PI / 4, cx, cy));
				return cross;
			}
			case SKULL:
			default:
			{
				Area skull = new Area(new Ellipse2D.Double(cx - r * 0.85, cy - r, r * 1.7, r * 1.5));
				skull.add(new Area(new RoundRectangle2D.Double(cx - r * 0.5, cy + r * 0.2, r, r * 0.75, r * 0.2, r * 0.2)));
				skull.subtract(new Area(new Ellipse2D.Double(cx - r * 0.55, cy - r * 0.45, r * 0.42, r * 0.45)));
				skull.subtract(new Area(new Ellipse2D.Double(cx + r * 0.13, cy - r * 0.45, r * 0.42, r * 0.45)));
				return skull;
			}
		}
	}

	private static Polygon polygon(double[] xs, double[] ys)
	{
		Polygon p = new Polygon();
		for (int i = 0; i < xs.length; i++)
		{
			p.addPoint((int) Math.round(xs[i]), (int) Math.round(ys[i]));
		}
		return p;
	}

	private static Shape star(double cx, double cy, double outer, double inner, int points)
	{
		java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
		for (int i = 0; i < points * 2; i++)
		{
			double r = i % 2 == 0 ? outer : inner;
			double a = Math.PI * i / points - Math.PI / 2;
			double x = cx + r * Math.cos(a);
			double y = cy + r * Math.sin(a);
			if (i == 0)
			{
				path.moveTo(x, y);
			}
			else
			{
				path.lineTo(x, y);
			}
		}
		path.closePath();
		return path;
	}
}
