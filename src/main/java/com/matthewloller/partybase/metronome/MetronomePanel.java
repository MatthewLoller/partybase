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
package com.matthewloller.partybase.metronome;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** The Metronome tab: turn it on, follow the leader, and pick 1-10 ticks per cycle. Swing thread only. */
class MetronomePanel extends JPanel
{
	static final int MAX_TICKS = 10;
	private static final Color GOLD = new Color(255, 196, 0);

	private final MetronomeModule metronome;
	private final JCheckBox show = new JCheckBox("Show metronome");
	private final JCheckBox sync = new JCheckBox("Sync with party leader");
	private final JLabel status = small("");
	private final JButton[] ticks = new JButton[MAX_TICKS];
	private final Timer refresher = new Timer(1000, e -> refresh());
	private boolean updating;

	MetronomePanel(MetronomeModule metronome)
	{
		this.metronome = metronome;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		for (JCheckBox box : new JCheckBox[] {show, sync})
		{
			box.setOpaque(false);
			box.setFocusable(false);
			box.setForeground(Color.WHITE);
		}
		show.addActionListener(e ->
		{
			if (!updating)
			{
				metronome.setEnabled(show.isSelected());
			}
		});
		sync.setToolTipText("Follow the party leader's tick position and tick count");
		sync.addActionListener(e ->
		{
			if (!updating)
			{
				metronome.setSyncLeader(sync.isSelected());
			}
		});

		JPanel grid = new JPanel(new GridLayout(2, 5, 4, 4));
		grid.setOpaque(false);
		for (int i = 0; i < MAX_TICKS; i++)
		{
			int count = i + 1;
			JButton b = new JButton(String.valueOf(count));
			b.setFocusable(false);
			b.setFont(FontManager.getRunescapeBoldFont());
			b.setToolTipText("A new cycle every " + count + (count == 1 ? " tick" : " ticks"));
			b.addActionListener(e -> metronome.setTickCount(count));
			ticks[i] = b;
			grid.add(b);
		}

		add(show);
		add(sync);
		add(Box.createVerticalStrut(4));
		add(status);
		add(Box.createVerticalStrut(10));
		add(heading("Ticks per cycle"));
		add(Box.createVerticalStrut(4));
		add(grid);
		add(Box.createVerticalStrut(6));
		add(small("<html>More options (colours, overlays, hotkeys) are in the Metronome settings.</html>"));
		for (Component c : getComponents())
		{
			((JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
		}
		refresh();
	}

	void setShown(boolean shown)
	{
		if (shown)
		{
			refresh();
			refresher.start();
		}
		else
		{
			refresher.stop();
		}
	}

	void refresh()
	{
		updating = true;
		try
		{
			show.setSelected(metronome.isEnabled());
			sync.setSelected(metronome.isSyncLeader());
		}
		finally
		{
			updating = false;
		}
		boolean following = metronome.isFollowingLeader();
		status.setText(following ? "Following the leader's metronome."
			: metronome.isLeading() ? "You lead: the party follows your metronome."
			: !metronome.isEnabled() ? "The metronome is off." : " ");
		int current = metronome.getTickCount();
		for (int i = 0; i < MAX_TICKS; i++)
		{
			JButton b = ticks[i];
			boolean selected = i + 1 == current;
			b.setEnabled(!following);
			b.setForeground(selected ? GOLD : Color.WHITE);
			b.setBorder(BorderFactory.createLineBorder(selected ? GOLD : ColorScheme.MEDIUM_GRAY_COLOR, selected ? 2 : 1));
		}
		if (following)
		{
			for (JButton b : ticks)
			{
				b.setToolTipText("Following the leader; untick Sync to pick your own");
			}
		}
	}

	private static JLabel heading(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(GOLD);
		return l;
	}

	private static JLabel small(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return l;
	}
}
