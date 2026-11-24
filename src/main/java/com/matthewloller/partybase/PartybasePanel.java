/*
 * Copyright (c) 2020, TheStonedTurtle <https://github.com/TheStonedTurtle>
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
package com.matthewloller.partybase;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.matthewloller.partybase.data.PartyPlayer;
import com.matthewloller.partybase.ui.ControlsPanel;
import com.matthewloller.partybase.ui.PlayerPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class PartybasePanel extends PluginPanel
{
	private static final String TAB_PARTY = "Party info";
	private static final String TAB_DPS = "DPS";
	private static final Color GOLD = new Color(255, 196, 0);

	private final PartybasePlugin plugin;
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cards = new JPanel(cardLayout);
	private final JButton partyTab = tabButton(TAB_PARTY);
	private final JButton dpsTab = tabButton(TAB_DPS);
	private final JLabel partyTimer = new JLabel();
	private final JLabel leaderLabel = new JLabel();
	private final Timer clock = new Timer(1000, e -> updateHeader());
	private String shownTab = TAB_PARTY;
	private boolean active;
	@Getter
	private final HashMap<Long, PlayerPanel> playerPanelMap = new HashMap<>();
	private final JPanel basePanel = new JPanel();
	private final JPanel passphrasePanel = new JPanel();
	private final JLabel passphraseLabel = new JLabel();
	@Getter
	private final ControlsPanel controlsPanel;

	@Inject
	PartybasePanel(final PartybasePlugin plugin)
	{
		super(false);
		this.plugin = plugin;
		this.setLayout(new BorderLayout());

		basePanel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));
		basePanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));

		final JPanel topPanel = new JPanel();
		topPanel.setBorder(new EmptyBorder(BORDER_OFFSET, 2, BORDER_OFFSET, 2));
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

		passphrasePanel.setBorder(new EmptyBorder(4, 0, 0, 0));
		passphrasePanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));

		final JLabel passphraseTopLabel = new JLabel("Party Passphrase");
		passphraseTopLabel.setForeground(Color.WHITE);
		passphraseTopLabel.setHorizontalTextPosition(JLabel.CENTER);
		passphraseTopLabel.setHorizontalAlignment(JLabel.CENTER);

		final JMenuItem copyOpt = new JMenuItem("Copy Passphrase");
		copyOpt.addActionListener(e ->
		{
			final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(passphraseLabel.getText()), null);
		});

		final JPopupMenu copyPopup = new JPopupMenu();
		copyPopup.setBorder(new EmptyBorder(5, 5, 5, 5));
		copyPopup.add(copyOpt);

		passphraseLabel.setText(plugin.getPartyPassphrase());
		passphraseLabel.setHorizontalTextPosition(JLabel.CENTER);
		passphraseLabel.setHorizontalAlignment(JLabel.CENTER);
		passphraseLabel.setComponentPopupMenu(copyPopup);

		passphrasePanel.add(passphraseTopLabel);
		passphrasePanel.add(passphraseLabel);
		syncPartyPassphraseVisibility();

		controlsPanel = new ControlsPanel(plugin);
		topPanel.add(controlsPanel);
		topPanel.add(passphrasePanel);

		final JPanel partyCard = new JPanel(new BorderLayout());
		partyCard.add(topPanel, BorderLayout.NORTH);

		// Wrap content to anchor to top and prevent expansion
		final JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.add(basePanel, BorderLayout.NORTH);
		final JScrollPane scrollPane = new JScrollPane(northPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		partyCard.add(scrollPane, BorderLayout.CENTER);

		final JPanel dpsWrap = new JPanel(new BorderLayout());
		dpsWrap.add(plugin.getDps().getPanel(), BorderLayout.NORTH);
		final JScrollPane dpsScroll = new JScrollPane(dpsWrap);
		dpsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		dpsScroll.getVerticalScrollBar().setUnitIncrement(16);

		cards.add(partyCard, TAB_PARTY);
		cards.add(dpsScroll, TAB_DPS);

		this.add(header(), BorderLayout.NORTH);
		this.add(cards, BorderLayout.CENTER);
		showTab(TAB_PARTY);
		updateHeader();
	}

	/** How long the party has been active, the leader, and the tab buttons. */
	private JPanel header()
	{
		partyTimer.setForeground(Color.WHITE);
		partyTimer.setFont(FontManager.getRunescapeBoldFont());
		partyTimer.setHorizontalAlignment(JLabel.CENTER);
		partyTimer.setToolTipText("Time since the longest-standing member of this party joined it");
		leaderLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		leaderLabel.setFont(FontManager.getRunescapeSmallFont());
		leaderLabel.setHorizontalAlignment(JLabel.CENTER);
		leaderLabel.setToolTipText("The leader marks targets (shift right-click a monster). "
			+ "The leader can right-click a member below to make them the leader.");

		final JPanel status = new JPanel(new GridLayout(0, 1, 0, 2));
		status.setOpaque(false);
		status.add(partyTimer);
		status.add(leaderLabel);

		partyTab.addActionListener(e -> showTab(TAB_PARTY));
		dpsTab.addActionListener(e -> showTab(TAB_DPS));
		final JPanel tabs = new JPanel(new GridLayout(1, 2, 0, 0));
		tabs.setOpaque(false);
		tabs.add(partyTab);
		tabs.add(dpsTab);

		final JPanel header = new JPanel(new BorderLayout(0, 6));
		header.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, 0, BORDER_OFFSET));
		header.add(status, BorderLayout.NORTH);
		header.add(tabs, BorderLayout.CENTER);
		return header;
	}

	private static JButton tabButton(String text)
	{
		final JButton b = new JButton(text);
		b.setFocusable(false);
		b.setContentAreaFilled(false);
		b.setFont(FontManager.getRunescapeBoldFont());
		return b;
	}

	private void showTab(String tab)
	{
		shownTab = tab;
		cardLayout.show(cards, tab);
		styleTab(partyTab, TAB_PARTY.equals(tab));
		styleTab(dpsTab, TAB_DPS.equals(tab));
		plugin.getDps().setPanelVisible(active && TAB_DPS.equals(tab));
	}

	private static void styleTab(JButton b, boolean selected)
	{
		b.setForeground(selected ? GOLD : ColorScheme.LIGHT_GRAY_COLOR);
		b.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 2, 0, selected ? GOLD : ColorScheme.MEDIUM_GRAY_COLOR),
			BorderFactory.createEmptyBorder(4, 0, 4, 0)));
	}

	@Override
	public void onActivate()
	{
		active = true;
		clock.start();
		updateHeader();
		plugin.getDps().setPanelVisible(TAB_DPS.equals(shownTab));
	}

	@Override
	public void onDeactivate()
	{
		active = false;
		clock.stop();
		plugin.getDps().setPanelVisible(false);
	}

	private void updateHeader()
	{
		final long start = plugin.getDps().partyStartMs();
		if (start <= 0 || !plugin.isInParty())
		{
			partyTimer.setText("Not in a party");
			leaderLabel.setText(" ");
			return;
		}
		partyTimer.setText("Party active " + formatDuration(System.currentTimeMillis() - start));
		final String leader = plugin.getDps().leaderName();
		leaderLabel.setText(plugin.getDps().youLead() ? "You are the leader"
			: leader != null ? "Leader: " + leader : "Leader: working it out...");
	}

	/** 0:05, 12:34 or 1:02:03. */
	static String formatDuration(long ms)
	{
		final long total = Math.max(0, ms / 1000);
		final long h = total / 3600;
		final long m = total / 60 % 60;
		final long sec = total % 60;
		return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%d:%02d", m, sec);
	}

	void clearSidebar()
	{
		basePanel.removeAll();
		playerPanelMap.clear();

		basePanel.revalidate();
		basePanel.repaint();
	}

	/**
	 * Shows all members of the party, excluding the local player. See {@link com.matthewloller.partybase.ui.PlayerBanner )
	 */
	void renderSidebar()
	{
		// Sort by their RSN first; If it doesn't exist sort by their Discord name instead
		// you first, then the others by name
		final List<PartyPlayer> players = plugin.getPartyMembers().values()
			.stream()
			.sorted(Comparator.<PartyPlayer, Boolean>comparing(o -> !plugin.isLocalPlayer(o.getMember().getMemberId()))
			.thenComparing(o -> Strings.isNullOrEmpty(o.getUsername()) ? o.getMember().getDisplayName() : o.getUsername()))
			.collect(Collectors.toList());

		for (final PartyPlayer player : players)
		{
			drawPlayerPanel(player);
		}

		if (getComponentCount() == 0)
		{
			basePanel.add(new JLabel("There are no members in your party"));
		}

		basePanel.revalidate();
		basePanel.repaint();
	}

	void drawPlayerPanel(PartyPlayer player)
	{
		drawPlayerPanel(player, false);
	}

	void drawPlayerPanel(PartyPlayer player, boolean hasBreakingBannerChange)
	{
		PlayerPanel panel = playerPanelMap.get(player.getMember().getMemberId());
		if (panel != null)
		{
			panel.updatePlayerData(player, true);
			return;
		}

		panel = new PlayerPanel(player, plugin.getConfig(), plugin.spriteManager, plugin.itemManager);
		final long memberId = player.getMember().getMemberId();
		final boolean you = plugin.isLocalPlayer(memberId);
		if (you)
		{
			panel.setExpandable(false);
		}
		playerPanelMap.put(memberId, panel);
		panel.updatePlayerData(player, hasBreakingBannerChange);
		if (you)
		{
			// you go at the top
			basePanel.add(panel, 0);
		}
		else
		{
			basePanel.add(panel);
		}
		basePanel.revalidate();
		basePanel.repaint();
		if (you)
		{
			return;
		}
		panel.addBannerMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				maybeShowLeaderMenu(e, memberId);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				maybeShowLeaderMenu(e, memberId);
			}
		});
	}

	/** The leader right-clicks a member: "Make party leader". */
	private void maybeShowLeaderMenu(MouseEvent e, long memberId)
	{
		if (!e.isPopupTrigger() || !plugin.getDps().youLead())
		{
			return;
		}
		final JMenuItem item = new JMenuItem("Make party leader");
		item.addActionListener(a -> plugin.getDps().makeLeader(memberId));
		final JPopupMenu menu = new JPopupMenu();
		menu.add(item);
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	void removePartyPlayer(final PartyPlayer player)
	{
		if (player != null)
		{
			final PlayerPanel p = playerPanelMap.remove(player.getMember().getMemberId());
			if (p != null)
			{
				basePanel.remove(p);
				renderSidebar();
			}
		}
	}

	void updatePartyMembersExpand(boolean expand)
	{
		for (PlayerPanel panel : playerPanelMap.values())
		{
			if (!panel.isExpandable())
			{
				continue;
			}
			panel.setShowInfo(expand);
			panel.getBanner().setExpandIcon(expand);
			panel.updatePanel();
		}
	}

	public void updatePartyControls()
	{
		controlsPanel.setVisible(plugin.getConfig().showPartyControls());
	}

	public void syncPartyPassphraseVisibility()
	{
		passphraseLabel.setText(plugin.getPartyPassphrase());
		passphrasePanel.setVisible(plugin.getConfig().showPartyPassphrase() && plugin.isInParty());
	}

	public void updateParty()
	{
		controlsPanel.updateControls();
		syncPartyPassphraseVisibility();
	}

	public void updateDisplayVirtualLevels()
	{
		playerPanelMap.values().forEach(PlayerPanel::updateDisplayVirtualLevels);
	}

	public void updateDisplayPlayerWorlds()
	{
		playerPanelMap.values().forEach(PlayerPanel::updateDisplayPlayerWorlds);
	}
}
