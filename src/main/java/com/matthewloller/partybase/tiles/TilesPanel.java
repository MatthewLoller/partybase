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
package com.matthewloller.partybase.tiles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** The Tiles tab: your Tile Packs packs to share, and the packs shared with the party. Swing thread only. */
class TilesPanel extends JPanel
{
	private static final Color GOLD = new Color(255, 196, 0);
	/** Your packs shown at once; type to search the rest. */
	private static final int MAX_LISTED = 40;

	private final TileModule tiles;
	private final JPanel sharedList = new JPanel();
	private final JPanel yourList = new JPanel();
	private final JLabel status = small("", ColorScheme.LIGHT_GRAY_COLOR);
	private final JTextField search = new JTextField();
	private final JButton shareAll = new JButton("Share all turned on");
	private List<TilePackInfo> yours = new ArrayList<>();

	TilesPanel(TileModule tiles)
	{
		this.tiles = tiles;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		sharedList.setLayout(new BoxLayout(sharedList, BoxLayout.Y_AXIS));
		sharedList.setOpaque(false);
		yourList.setLayout(new BoxLayout(yourList, BoxLayout.Y_AXIS));
		yourList.setOpaque(false);

		add(heading("Shared with the party"));
		add(Box.createVerticalStrut(4));
		add(sharedList);
		add(Box.createVerticalStrut(12));
		add(heading("Your Tile Packs"));
		add(Box.createVerticalStrut(4));
		add(status);
		add(Box.createVerticalStrut(4));

		shareAll.setFocusable(false);
		shareAll.setToolTipText("Share every pack you have turned on in Tile Packs");
		shareAll.addActionListener(e -> shareAllEnabled());
		JPanel shareRow = new JPanel(new GridLayout(1, 1));
		shareRow.setOpaque(false);
		shareRow.add(shareAll);
		add(shareRow);
		add(Box.createVerticalStrut(4));

		search.setToolTipText("Search your packs");
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				listYours();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				listYours();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				listYours();
			}
		});
		add(search);
		add(Box.createVerticalStrut(4));
		add(yourList);

		for (Component c : getComponents())
		{
			((JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
		}
		refreshShared();
	}

	/** Reads your packs again (they may have changed in Tile Packs). */
	void reload()
	{
		yours = tiles.yourPacks();
		long on = yours.stream().filter(TilePackInfo::isEnabled).count();
		status.setText(!tiles.isTilePacksInstalled()
			? "<html>Tile Packs is not installed: only packs saved in its settings are listed.</html>"
			: yours.size() + " packs, " + on + " turned on in Tile Packs");
		shareAll.setText("Share all turned on (" + on + ")");
		listYours();
		refreshShared();
	}

	void refreshShared()
	{
		sharedList.removeAll();
		sharedList.add(partyTilesRow());
		sharedList.add(Box.createVerticalStrut(2));
		List<TileModule.SharedPack> packs = tiles.sharedPacks();
		if (packs.isEmpty())
		{
			sharedList.add(small("Nothing shared yet.", ColorScheme.LIGHT_GRAY_COLOR));
		}
		for (TileModule.SharedPack p : packs)
		{
			JComponent row = sharedRow(p);
			row.setAlignmentX(LEFT_ALIGNMENT);
			sharedList.add(row);
			sharedList.add(Box.createVerticalStrut(2));
		}
		boolean inParty = tiles.canShare();
		shareAll.setEnabled(inParty);
		sharedList.revalidate();
		sharedList.repaint();
		listYours();
	}

	/** The leader's party tiles: show or hide, save, and (for the leader) clear. */
	private JComponent partyTilesRow()
	{
		int count = tiles.partyTileCount();
		JCheckBox show = new JCheckBox("Party tiles (" + count + ")", tiles.isPartyTilesVisible());
		show.setOpaque(false);
		show.setFocusable(false);
		show.setForeground(Color.WHITE);
		show.setToolTipText("Tiles the party leader marked (shift right-click a tile to mark one if you lead)");
		show.addActionListener(e -> tiles.setPartyTilesVisible(show.isSelected()));

		JButton save = button("Save", "Add the party tiles to Tile Packs as one custom pack");
		save.setEnabled(count > 0);
		save.addActionListener(e ->
		{
			int id = tiles.savePartyTiles();
			if (id >= 0)
			{
				JOptionPane.showMessageDialog(this, "The party tiles were added to Tile Packs (pack " + id + ").\n"
					+ "Restart Tile Packs (or the client) to see them there.", "Partybase", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		JButton clear = button("Clear", "Remove every party tile, for the whole party");
		clear.setEnabled(count > 0 && tiles.canMarkTiles());
		if (!tiles.canMarkTiles())
		{
			clear.setToolTipText("Only the party leader can clear party tiles; untick to hide them");
		}
		clear.addActionListener(e ->
		{
			if (JOptionPane.showConfirmDialog(this, "Remove all " + count + " party tiles for the whole party?", "Partybase",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			{
				tiles.clearPartyTiles();
			}
		});

		JPanel buttons = new JPanel(new GridLayout(1, 0, 2, 0));
		buttons.setOpaque(false);
		buttons.add(save);
		buttons.add(clear);

		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(2, 4, 2, 4));
		row.add(show, BorderLayout.CENTER);
		row.add(small("shift right-click a tile to mark (leader)", ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.SOUTH);
		row.add(buttons, BorderLayout.EAST);
		row.setAlignmentX(LEFT_ALIGNMENT);
		return row;
	}

	private JComponent sharedRow(TileModule.SharedPack p)
	{
		JCheckBox show = new JCheckBox(p.name, p.visible);
		show.setOpaque(false);
		show.setFocusable(false);
		show.setForeground(Color.WHITE);
		show.setToolTipText("Show " + p.name + " (" + p.points.size() + " tiles, from " + p.fromName + ")");
		show.addActionListener(e -> tiles.setVisible(p, show.isSelected()));

		JButton save = button("Save", "Add it to Tile Packs as a custom pack (shows there after Tile Packs restarts)");
		save.addActionListener(e ->
		{
			int id = tiles.saveToTilePacks(p);
			JOptionPane.showMessageDialog(this, p.name + " was added to Tile Packs (pack " + id + ").\n"
				+ "Restart Tile Packs (or the client) to see it there.", "Partybase", JOptionPane.INFORMATION_MESSAGE);
		});
		JButton remove = button("x", "Remove this shared pack");
		remove.addActionListener(e -> tiles.remove(p));

		JPanel buttons = new JPanel(new GridLayout(1, 2, 2, 0));
		buttons.setOpaque(false);
		buttons.add(save);
		buttons.add(remove);

		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(2, 4, 2, 4));
		row.add(show, BorderLayout.CENTER);
		row.add(small("from " + p.fromName, ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.SOUTH);
		row.add(buttons, BorderLayout.EAST);
		return row;
	}

	private void listYours()
	{
		yourList.removeAll();
		String q = search.getText().trim().toLowerCase(Locale.ROOT);
		boolean inParty = tiles.canShare();
		int shown = 0;
		for (TilePackInfo pack : yours)
		{
			if (!q.isEmpty() && !pack.name.toLowerCase(Locale.ROOT).contains(q))
			{
				continue;
			}
			if (shown++ >= MAX_LISTED)
			{
				yourList.add(small("Type to search for more...", ColorScheme.LIGHT_GRAY_COLOR));
				break;
			}
			JLabel name = small(pack.name, pack.enabled ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
			name.setToolTipText(pack.enabled ? "Turned on in Tile Packs" : "Turned off in Tile Packs");
			JButton share = button("Share", inParty ? "Share " + pack.name + " with the party" : "Join a party first");
			share.setEnabled(inParty);
			share.addActionListener(e -> tiles.share(pack));
			JPanel row = new JPanel(new BorderLayout(4, 0));
			row.setOpaque(false);
			row.setBorder(new EmptyBorder(1, 0, 1, 0));
			row.add(name, BorderLayout.CENTER);
			row.add(share, BorderLayout.EAST);
			row.setAlignmentX(LEFT_ALIGNMENT);
			yourList.add(row);
		}
		if (yours.isEmpty())
		{
			yourList.add(small("No tile packs found.", ColorScheme.LIGHT_GRAY_COLOR));
		}
		yourList.revalidate();
		yourList.repaint();
	}

	private void shareAllEnabled()
	{
		for (TilePackInfo pack : yours)
		{
			if (pack.enabled)
			{
				tiles.share(pack);
			}
		}
	}

	private static JLabel heading(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(GOLD);
		l.setAlignmentX(LEFT_ALIGNMENT);
		return l;
	}

	private static JLabel small(String text, Color color)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(color);
		l.setAlignmentX(LEFT_ALIGNMENT);
		return l;
	}

	private static JButton button(String text, String tip)
	{
		JButton b = new JButton(text);
		b.setFocusable(false);
		b.setToolTipText(tip);
		b.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
		return b;
	}
}
