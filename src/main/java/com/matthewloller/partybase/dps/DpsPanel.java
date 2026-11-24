package com.matthewloller.partybase.dps;

import static com.matthewloller.partybase.dps.PanelWidgets.GOLD;
import static com.matthewloller.partybase.dps.PanelWidgets.actionRow;
import static com.matthewloller.partybase.dps.PanelWidgets.bold;
import static com.matthewloller.partybase.dps.PanelWidgets.button;
import static com.matthewloller.partybase.dps.PanelWidgets.line;
import static com.matthewloller.partybase.dps.PanelWidgets.note;
import static com.matthewloller.partybase.dps.PanelWidgets.section;
import static com.matthewloller.partybase.dps.PanelWidgets.small;
import com.matthewloller.partybase.dps.model.Fight;
import com.matthewloller.partybase.dps.model.FightReport;
import com.matthewloller.partybase.dps.model.Format;
import com.matthewloller.partybase.dps.model.KillRates;
import com.matthewloller.partybase.dps.model.MonsterStats;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** The DPS tab: a live bar meter, then the fight details. Swing thread only. */
class DpsPanel extends JPanel
{
	/** Ids of the two fight list choices that are not a saved fight. */
	static final String LIVE_CHOICE = "live";
	static final String SESSION_CHOICE = "session";

	static class Choice
	{
		final String id;
		final String label;

		Choice(String id, String label)
		{
			this.id = id;
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** Everything the panel shows, built on the client thread. */
	static class Data
	{
		List<Choice> choices = new ArrayList<>();
		FightReport report;
		boolean running;
		boolean paused;
		boolean bossOnly;
		String endReason;
		int fights;
		long startMs;
		DpsPartyPanel.View party = new DpsPartyPanel.View();
		/** Boss damage over the boss time; null without a boss or in boss-only mode. */
		FightReport bossReport;
		/** {label, value} lines comparing a boss kill with the personal bests. */
		List<String[]> killLines = new ArrayList<>();
		/** In a raid: {boss, time, "PB 1:50" / "NEW PB!" / ""} for every boss so far. */
		List<String[]> bossTimes = new ArrayList<>();
		/** {label, value} lines of the monsters whose defence special attacks lowered. */
		List<String[]> defence = new ArrayList<>();
		/** Every monster, raid and Barrows run with its personal bests and stats, last kill first. */
		List<MonsterStats.Entry> monsters = new ArrayList<>();
		List<KillRates.Rate> killRates = new ArrayList<>();
		/** The live Barrows run time, or null without a run. */
		String barrowsRunTime;
		boolean barrowsRunPaused;
		/** The party's marked targets, in marking order. */
		List<MarkedTarget.Mark> marks = new ArrayList<>();
		/** You may mark and clear targets (you lead, or you are solo). */
		boolean canMark;
	}

	/** Sections you can show and hide; the name is saved, the flag is the default. */
	private enum Part
	{
		PER_HOUR(true),
		CHESTS(true),
		RAIDS(true),
		BOSSES(true),
		MONSTERS(false),
		FIGHT(true),
		BOSS_TIMES(true),
		DEFENCE(true),
		PLAYERS(true),
		TARGETS(false),
		MONSTER_STATS(false);

		final boolean openByDefault;

		Part(boolean openByDefault)
		{
			this.openByDefault = openByDefault;
		}
	}

	private static final String NO_FIGHTS = "No fights yet. Hit something!";

	private final DpsModule plugin;
	private final JComboBox<Choice> fightBox = new JComboBox<>();
	/**
	 * GoldenDPS's own party tab (join by name, who runs the plugin, kick). Not shown: the Party info tab has
	 * the party controls. Kept up to date so it can be merged into Party info later.
	 */
	private final DpsPartyPanel partyPanel;
	private final SidebarMeter meter;
	private final JPanel markList = new JPanel();
	private final JButton clearMarks;
	private final JCheckBox bossOnlyBox = new JCheckBox("Boss only");
	private final JPanel content = new JPanel();
	private volatile String selectedId = LIVE_CHOICE;
	private volatile boolean active;
	private boolean updating;
	/** Sections whose show/hide differs from the default. */
	private final Set<Part> toggled = EnumSet.noneOf(Part.class);
	/** Weapons in the breakdown that show their accuracy (this session only). */
	private final Set<String> openSources = new HashSet<>();
	/** Monsters in the monster stats that show their card (this session only). */
	private final Set<String> openMonsters = new HashSet<>();

	DpsPanel(DpsModule plugin, DpsOverlay overlay)
	{
		this.plugin = plugin;
		this.partyPanel = new DpsPartyPanel(plugin);
		this.meter = new SidebarMeter(overlay);
		this.clearMarks = button("Clear all", "Remove every marked target", () -> plugin.clearMark(null));
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout(0, 8));

		JPanel top = new JPanel(new GridLayout(0, 1, 0, 4));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);

		fightBox.setFocusable(false);
		fightBox.addActionListener(e ->
		{
			if (updating)
			{
				return;
			}
			Choice c = (Choice) fightBox.getSelectedItem();
			selectedId = c == null ? LIVE_CHOICE : c.id;
			plugin.requestPanelUpdate();
		});
		top.add(fightBox);

		bossOnlyBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bossOnlyBox.setFocusable(false);
		bossOnlyBox.addActionListener(e ->
		{
			if (!updating)
			{
				plugin.setBossOnly(bossOnlyBox.isSelected());
			}
		});
		top.add(bossOnlyBox);
		top.add(fightButtons());
		top.add(reportButtons());

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		markList.setLayout(new BoxLayout(markList, BoxLayout.Y_AXIS));
		markList.setOpaque(false);
		markList.setVisible(false);

		JPanel meterAndMark = new JPanel(new BorderLayout(0, 4));
		meterAndMark.setOpaque(false);
		meterAndMark.add(markList, BorderLayout.NORTH);
		meterAndMark.add(meter, BorderLayout.CENTER);

		JPanel north = new JPanel(new BorderLayout(0, 8));
		north.setOpaque(false);
		north.add(meterAndMark, BorderLayout.NORTH);
		north.add(top, BorderLayout.CENTER);
		add(north, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);

		loadToggles(plugin.panelToggles());
		content.add(note(NO_FIGHTS));
	}

	private JComponent fightButtons()
	{
		JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 0));
		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttons.add(button("End fight", "End the running fight now", plugin::endFight));
		buttons.add(button("New session", "Start a new session total", plugin::resetSession));
		buttons.add(button("Clear", "Delete all saved fights", this::confirmClearHistory));
		return buttons;
	}

	private void confirmResetMonster(String name)
	{
		if (askYes("Delete the personal bests and stats of " + name + "?"))
		{
			plugin.resetMonster(name);
		}
	}

	private void confirmClearHistory()
	{
		if (askYes("Delete all saved fights for this account?"))
		{
			plugin.clearHistory();
		}
	}

	/** "Report to party" with a button per report kind. */
	private JComponent reportButtons()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.add(small("Report:", ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.WEST);
		JPanel buttons = new JPanel(new GridLayout(1, ReportKind.values().length, 4, 0));
		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (ReportKind kind : ReportKind.values())
		{
			String text = kind.label.replace(" damage", "");
			buttons.add(button(text, "Send the top 5 (" + kind.label.toLowerCase() + ") of the shown fight to your party chat",
				() -> plugin.reportPanelToParty(kind)));
		}
		row.add(buttons, BorderLayout.CENTER);
		return row;
	}

	/** Called by the Partybase panel when the DPS tab comes on or goes off screen. */
	void setShown(boolean shown)
	{
		active = shown;
		meter.setRunning(shown);
		if (shown)
		{
			plugin.requestPanelUpdate();
		}
	}

	/** Safe to call from any thread. */
	boolean isPanelOpen()
	{
		return active;
	}

	String getSelectedId()
	{
		return selectedId;
	}

	static String label(Fight f, long now)
	{
		String time = new SimpleDateFormat("HH:mm").format(new Date(f.startMs));
		// bosses inside a raid are listed under the raid
		return time + "  " + (f.partOf != null ? "> " : "") + f.title() + "  " + Format.preciseDuration(f.durationMs(now));
	}

	/** One row per marked target: icon, name, and (for the leader) a remove button. */
	private void updateMarks(List<MarkedTarget.Mark> marks, boolean canMark)
	{
		markList.removeAll();
		markList.setVisible(!marks.isEmpty());
		if (!marks.isEmpty())
		{
			JLabel title = bold("Marked targets", GOLD);
			title.setToolTipText("Marked by the party leader: tracked as a boss fight until they die. "
				+ "Shift right-click a monster to mark it.");
			JPanel head = new JPanel(new BorderLayout(4, 0));
			head.setOpaque(false);
			head.add(title, BorderLayout.CENTER);
			if (canMark && marks.size() > 1)
			{
				head.add(clearMarks, BorderLayout.EAST);
			}
			markList.add(head);
		}
		for (MarkedTarget.Mark mark : marks)
		{
			JLabel name = new JLabel(mark.name, mark.icon.image(14), JLabel.LEFT);
			name.setForeground(mark.icon.getColor());
			name.setFont(FontManager.getRunescapeSmallFont());
			name.setIconTextGap(6);
			JPanel row = new JPanel(new BorderLayout(4, 0));
			row.setOpaque(false);
			row.setBorder(new EmptyBorder(2, 0, 0, 0));
			row.add(name, BorderLayout.CENTER);
			if (canMark)
			{
				row.add(button("x", "Remove this mark", () -> plugin.clearMark(mark.key)), BorderLayout.EAST);
			}
			markList.add(row);
		}
		markList.revalidate();
		markList.repaint();
	}

	void setPartyName(String name)
	{
		partyPanel.setPartyName(name);
	}

	void update(Data d)
	{
		updating = true;
		try
		{
			updateChoices(d.choices);
			bossOnlyBox.setSelected(d.bossOnly);
			updateMarks(d.marks, d.canMark);
			partyPanel.update(d.party);
		}
		finally
		{
			updating = false;
		}

		content.removeAll();
		if (d.barrowsRunTime != null)
		{
			JPanel run = section();
			run.add(line("Barrows run", (d.barrowsRunPaused ? "paused " : "") + d.barrowsRunTime,
				d.barrowsRunPaused ? Color.YELLOW : GOLD));
			content.add(run);
		}
		addPerHour(d.killRates);
		addFight(d);
		addMonsterStats(d.monsters);
		content.revalidate();
		content.repaint();
	}

	/** Kills per hour first, so chests and raids are always at the top. */
	private void addPerHour(List<KillRates.Rate> rates)
	{
		if (rates.isEmpty() || !addHeading("Per hour (this session)", Part.PER_HOUR, 0))
		{
			return;
		}
		addRates("Chests", rates, KillRates.Kind.RUN, Part.CHESTS);
		addRates("Raids", rates, KillRates.Kind.RAID, Part.RAIDS);
		addRates("Bosses", rates, KillRates.Kind.BOSS, Part.BOSSES);
		addRates("Monsters", rates, KillRates.Kind.MONSTER, Part.MONSTERS);
	}

	private void addFight(Data d)
	{
		FightReport r = d.report;
		if (r == null)
		{
			addHeading("Fight", null, 0);
			content.add(Box.createVerticalStrut(4));
			content.add(note(SESSION_CHOICE.equals(selectedId) ? "No fights in this session yet." : NO_FIGHTS));
			return;
		}
		if (addHeading("Fight", Part.FIGHT, 0))
		{
			content.add(Box.createVerticalStrut(4));
			content.add(FightCards.summary(d));
		}
		if (!d.bossTimes.isEmpty() && addHeading("Boss times", Part.BOSS_TIMES, d.bossTimes.size()))
		{
			content.add(Box.createVerticalStrut(4));
			content.add(FightCards.bossTimes(d.bossTimes));
		}
		if (!d.defence.isEmpty() && addHeading("Defence", Part.DEFENCE, 0))
		{
			content.add(Box.createVerticalStrut(4));
			content.add(FightCards.lines(d.defence));
		}

		List<FightReport.PlayerRow> rows = new ArrayList<>(r.getRows());
		rows.sort((a, b) -> Long.compare(b.damage, a.damage));
		boolean solo = r.isSolo();
		if (!rows.isEmpty() && addHeading(solo ? "Details" : "Players", Part.PLAYERS, solo ? 0 : rows.size()))
		{
			for (FightReport.PlayerRow row : rows)
			{
				content.add(Box.createVerticalStrut(4));
				content.add(FightCards.player(row, d.bossReport == null ? null : d.bossReport.row(row.name), solo,
					openSources, this::toggleSource));
			}
		}

		if (!r.getTargets().isEmpty() && addHeading("Damage per target", Part.TARGETS, r.getTargets().size()))
		{
			for (FightReport.TargetRow t : r.getTargets())
			{
				content.add(Box.createVerticalStrut(4));
				content.add(FightCards.target(t));
			}
		}
	}

	/** Every monster you killed; a click opens its personal bests and stats. */
	private void addMonsterStats(List<MonsterStats.Entry> entries)
	{
		if (entries.isEmpty())
		{
			return;
		}
		boolean open = addHeading("Monster stats", Part.MONSTER_STATS, entries.size());
		resetAllOnRightClick((JComponent) content.getComponent(content.getComponentCount() - 1));
		if (!open)
		{
			return;
		}
		content.add(Box.createVerticalStrut(4));
		JPanel list = section();
		for (MonsterStats.Entry e : entries)
		{
			JComponent row = line((openMonsters.contains(e.name) ? "- " : "+ ") + e.name, FightCards.monsterSummary(e));
			row.setToolTipText("Click for the personal bests and stats of " + e.name);
			PanelWidgets.clickable(row, () -> toggleMonster(e.name));
			list.add(row);
			if (openMonsters.contains(e.name))
			{
				FightCards.addMonster(list, e, openSources, this::toggleSource,
					() -> confirmResetMonster(e.name));
			}
		}
		resetAllOnRightClick(list);
		content.add(list);
	}

	private void resetAllOnRightClick(JComponent c)
	{
		PanelWidgets.rightClickMenu(c, "Reset all monster stats", this::confirmResetAllMonsters);
	}

	/** Asked twice: it deletes every personal best, and that cannot be undone. */
	private void confirmResetAllMonsters()
	{
		if (askYes("Delete the personal bests and stats of every monster, raid and Barrows run?")
			&& askYes("Are you sure? This cannot be undone."))
		{
			plugin.resetAllMonsters();
		}
	}

	private boolean askYes(String question)
	{
		return JOptionPane.showConfirmDialog(this, question, "Partybase", JOptionPane.YES_NO_OPTION)
			== JOptionPane.YES_OPTION;
	}

	private void toggleMonster(String name)
	{
		if (!openMonsters.remove(name))
		{
			openMonsters.add(name);
		}
		plugin.requestPanelUpdate();
	}

	private boolean isOpen(Part part)
	{
		return part.openByDefault != toggled.contains(part);
	}

	private void toggleSource(String key)
	{
		if (!openSources.remove(key))
		{
			openSources.add(key);
		}
		plugin.requestPanelUpdate();
	}

	private void toggle(Part part)
	{
		if (!toggled.remove(part))
		{
			toggled.add(part);
		}
		StringBuilder saved = new StringBuilder();
		for (Part p : toggled)
		{
			saved.append(saved.length() > 0 ? "," : "").append(p.name());
		}
		plugin.savePanelToggles(saved.toString());
		plugin.requestPanelUpdate();
	}

	/** @param saved part names, comma separated; unknown names are skipped */
	private void loadToggles(String saved)
	{
		for (String name : saved.split(","))
		{
			for (Part part : Part.values())
			{
				if (part.name().equals(name))
				{
					toggled.add(part);
				}
			}
		}
	}

	/** A click on the row shows or hides the part; "show" or "hide" on the right says which. */
	private JComponent toggleRow(JLabel title, Part part, int count)
	{
		if (count > 0)
		{
			title.setText(title.getText() + " (" + count + ")");
		}
		if (part == null)
		{
			return actionRow(title, null, null, null);
		}
		boolean open = isOpen(part);
		return actionRow(title, open ? "hide" : "show", open ? "Click to hide" : "Click to show", () -> toggle(part));
	}

	/** One card per kind: "Vorkath    12 kc  18.5/hr". */
	private void addRates(String title, List<KillRates.Rate> rates, KillRates.Kind kind, Part part)
	{
		int count = (int) rates.stream().filter(r -> r.kind == kind).count();
		if (count == 0)
		{
			return;
		}
		JPanel card = section();
		card.add(toggleRow(bold(title, Color.WHITE), part, count));
		for (KillRates.Rate rate : rates)
		{
			if (rate.kind == kind && isOpen(part))
			{
				card.add(rateLine(rate));
			}
		}
		content.add(Box.createVerticalStrut(4));
		content.add(card);
	}

	private static JComponent rateLine(KillRates.Rate rate)
	{
		String perHour = String.format("%.1f/hr", rate.perHour);
		JComponent line = line(rate.name, rate.kills + " kc  " + perHour);
		String counted = rate.kind == KillRates.Kind.RAID ? " completions" : rate.kind == KillRates.Kind.RUN ? " chests" : " kills";
		line.setToolTipText(rate.name + ": " + rate.kills + counted + ", average " + Format.duration(rate.averageMs));
		return line;
	}

	/** Adds a gold heading; returns true when its part is shown. */
	private boolean addHeading(String text, Part part, int count)
	{
		content.add(Box.createVerticalStrut(content.getComponentCount() == 0 ? 0 : 10));
		content.add(toggleRow(bold(text, GOLD), part, count));
		return part == null || isOpen(part);
	}

	private void updateChoices(List<Choice> choices)
	{
		if (fightBox.isPopupVisible())
		{
			// a new list would close the open list under your mouse
			return;
		}
		DefaultComboBoxModel<Choice> model = new DefaultComboBoxModel<>();
		Choice select = null;
		for (Choice c : choices)
		{
			model.addElement(c);
			if (c.id.equals(selectedId))
			{
				select = c;
			}
		}
		if (select == null && !choices.isEmpty())
		{
			// the selected fight is gone: fall back to the live view
			select = choices.get(0);
			selectedId = select.id;
		}
		fightBox.setModel(model);
		if (select != null)
		{
			fightBox.setSelectedItem(select);
		}
	}
}
