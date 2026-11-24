/*
 * Copyright (c) 2026, tjeerocka <https://github.com/tjeerocka>
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

import com.google.gson.Gson;
import com.google.inject.Injector;
import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.PartybaseModule;
import com.matthewloller.partybase.PartybasePlugin;
import com.matthewloller.partybase.dps.model.DefenceDrains;
import com.matthewloller.partybase.dps.model.Fight;
import com.matthewloller.partybase.dps.model.FightReport;
import com.matthewloller.partybase.dps.model.FightTracker;
import com.matthewloller.partybase.dps.model.GameTimes;
import com.matthewloller.partybase.dps.model.HealType;
import com.matthewloller.partybase.dps.model.HealthWatcher;
import com.matthewloller.partybase.dps.model.HistoryStore;
import com.matthewloller.partybase.dps.model.KillCounts;
import com.matthewloller.partybase.dps.model.MeterMode;
import com.matthewloller.partybase.dps.model.NpcStats;
import com.matthewloller.partybase.dps.model.PlayerKind;
import com.matthewloller.partybase.dps.model.RaidPoints;
import com.matthewloller.partybase.dps.model.RaidScaling;
import com.matthewloller.partybase.dps.model.TargetMatcher;
import com.matthewloller.partybase.dps.model.WeaponStyles;
import java.io.IOException;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.slayer.SlayerPluginService;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.Text;

/**
 * Damage and healing meter, ported from GoldenDPS (BSD 2-Clause, Copyright (c) 2026, tjeerocka).
 * PartybasePlugin starts it, registers it on the event bus, and shows its panel in the DPS tab.
 * The plugin depends on the Slayer plugin for the slayer task used by the hit chance.
 * Game data is read and counted on the client thread; party messages
 * are moved there from the websocket thread, and the panel receives finished reports on the
 * Swing thread.
 */
@Slf4j
@Singleton
public class DpsModule implements PartybaseModule, FightTracker.Listener
{
	private static final int PANEL_EVERY_TICKS = 2;
	private static final String PANEL_TOGGLES_KEY = "panelToggles";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PartybaseConfig config;

	@Inject
	private OverlayManager overlayManager;




	@Inject
	private ItemManager itemManager;

	@Inject
	private SlayerPluginService slayer;

	@Inject
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private ChatNotifier chat;

	@Inject
	private PartyManager party;

	@Inject
	private LocalHealing healing;

	@Inject
	private BarrowsTracker barrows;

	@Inject
	private MarkedTarget marked;


	private final FightTracker tracker = new FightTracker();
	private final Bosses bosses = new Bosses();
	private final TargetMatcher matcher = new TargetMatcher();
	private final FunEffects fun = new FunEffects();
	/** Kept over plugin restarts, like the game chat it is read from. */
	private final KillCounts killCounts = new KillCounts();
	private final RaidPoints raidPoints = new RaidPoints();

	// made in startUp, when the injected fields are set
	private FightStorage storage;
	private PartyUpdates partyUpdates;
	private CombatEvents combat;
	private RaidFights raids;
	private PartyReports reports;
	private MeterViews views;
	private DefenceDrains defence;
	private final RaidScaling raidScaling = new RaidScaling();
	private TargetHitChance hitChance;
	/** Your hit chance on the monster you attack, null when unknown; read by the panel. */
	private volatile TargetHitChance.Result hitChanceCache;

	/** Makes the overlays in startUp: they need this module, so they can't be injected into it. */
	@Inject
	private Injector injector;

	private DpsOverlay overlay;
	private HeadIconOverlay headIconOverlay;
	private DefenceOverlay defenceOverlay;
	private MarkedTargetOverlay markedOverlay;
	private DpsPanel panel;
	private int ticks;
	private volatile FightReport overlayCache;
	private volatile BossMeter bossCache;
	/** Target key to the lines above that NPC. */
	private volatile Map<String, String[]> defenceCache = Collections.emptyMap();
	/** Worked out on the client thread: the overlay must not read the fight history. */
	private volatile boolean overallGoingOn;
	private volatile boolean overallPaused;

	@Override
	public void startUp(PartybasePlugin plugin) throws IOException
	{
		createParts(plugin.getDataDirectory());
		tracker.setListener(this);

		overlay = injector.getInstance(DpsOverlay.class);
		headIconOverlay = injector.getInstance(HeadIconOverlay.class);
		defenceOverlay = injector.getInstance(DefenceOverlay.class);
		markedOverlay = injector.getInstance(MarkedTargetOverlay.class);

		overlayManager.add(overlay);
		overlayManager.add(headIconOverlay);
		overlayManager.add(defenceOverlay);
		overlayManager.add(markedOverlay);
		wsClient.registerMessage(PartybaseDpsUpdate.class);
		wsClient.registerMessage(PartybaseDpsPartyMessage.class);

		panel = new DpsPanel(this, overlay);

		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				storage.loadAccount(client.getAccountHash());
			}
		});
	}

	private void createParts(Filepath dataDir) throws IOException
	{
		HistoryStore store = new HistoryStore(gson, dataDir);
		storage = new FightStorage(tracker, store, executor, clientThread, () ->
		{
			refreshOverlay();
			updatePanel();
		});
		NpcStats npcStats = NpcStats.load();
		defence = new DefenceDrains(npcStats, raidScaling);
		partyUpdates = new PartyUpdates(client, partyService, party, config, tracker, bosses, matcher, defence);
		combat = new CombatEvents(client, partyService, config, tracker, bosses, matcher, healing, barrows, party,
			partyUpdates, defence, marked);
		hitChance = new TargetHitChance(client, itemManager, slayer, npcStats, WeaponStyles.load(), defence,
			raidScaling, combat, bosses);
		raids = new RaidFights(client, chat, tracker);
		reports = new PartyReports(config, partyService, party, chat, storage, killCounts, raidPoints);
		views = new MeterViews(config, tracker, storage, new BossHealth(client), barrows, party, fun, defence);
	}

	@Override
	public void shutDown()
	{
		clientThread.invoke(() ->
		{
			tracker.endNow(System.currentTimeMillis(), "Plugin turned off");
			storage.save();
			storage.unload();
			tracker.clearHistory();
			overlayCache = null;
			bossCache = null;
			hitChanceCache = null;
			defence.clear();
			defenceCache = Collections.emptyMap();
			reports.clear();
			healing.reset();
			partyUpdates.clear();
			marked.clear();
		});
		wsClient.unregisterMessage(PartybaseDpsUpdate.class);
		wsClient.unregisterMessage(PartybaseDpsPartyMessage.class);
		overlayManager.remove(overlay);
		overlayManager.remove(headIconOverlay);
		overlayManager.remove(defenceOverlay);
		overlayManager.remove(markedOverlay);
		panel = null;
	}

	/** The DPS tab content; null while stopped. Swing thread. */
	public JComponent getPanel()
	{
		return panel;
	}

	/** Tells the DPS tab whether it is on screen, so it is only rebuilt while visible. Swing thread. */
	public void setPanelVisible(boolean visible)
	{
		DpsPanel p = panel;
		if (p != null)
		{
			p.setShown(visible);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (PartybaseConfig.GROUP.equals(e.getGroup()))
		{
			clientThread.invoke(() ->
			{
				refreshOverlay();
				updatePanel();
				if ("chatColor".equals(e.getKey()))
				{
					chat.recolorOldMessages();
				}
			});
		}
	}

	// ---- game events ----

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		combat.hitsplatApplied(e.getActor(), e.getHitsplat());
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged e)
	{
		combat.graphicChanged(e.getActor());
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved e)
	{
		combat.projectileMoved(e.getProjectile());
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		combat.animationChanged(e.getActor());
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		if (e.getSkill() == Skill.HITPOINTS)
		{
			healing.hpChanged(e.getLevel(), e.getBoostedLevel());
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		String option = Text.removeTags(e.getMenuOption());
		if (option == null)
		{
			return;
		}
		int tick = client.getTickCount();
		if (option.equals("Eat") || option.equals("Drink") || option.equals("Quaff"))
		{
			healing.eatClicked(e.getItemId(), tick);
		}
		else if (option.equals("Cast"))
		{
			String target = Text.removeTags(e.getMenuTarget());
			if (target == null)
			{
				return;
			}
			if (target.startsWith("Heal Other") || target.startsWith("Heal Group"))
			{
				healing.healOtherCast(tick);
			}
			else if (target.contains("->"))
			{
				combat.spellCastClicked(tick, target);
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() == InventoryID.INV)
		{
			healing.inventoryChanged(e.getItemContainer(), client.getTickCount());
		}
	}

	/** While Partybase shows a line over a player, their typed chat only shows in the chatbox. */
	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged e)
	{
		if (e.getActor() instanceof Player && hasFunLine((Player) e.getActor()))
		{
			e.getActor().setOverheadText("");
		}
	}

	private boolean hasFunLine(Player p)
	{
		String name = Names.clean(p.getName());
		return name != null && config.healerSnack() && fun.line(name, System.currentTimeMillis()) != null;
	}

	/** Hides chat that was already over a head when a Partybase line starts. */
	private void hideOverheadChat()
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null || !fun.anyone())
		{
			return;
		}
		for (Player p : wv.players())
		{
			String text = p == null ? null : p.getOverheadText();
			if (text != null && !text.isEmpty() && hasFunLine(p))
			{
				p.setOverheadText("");
			}
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		NPC npc = e.getNpc();
		String key = TargetMatcher.key(client.getWorld(), npc.getIndex());
		defence.remove(key);
		MarkedTarget.Mark mark = marked.remove(key);
		boolean wasMarked = mark != null;
		if (wasMarked && npc.isDead())
		{
			boolean last = marked.isEmpty();
			chat.info(mark.name + " (" + mark.icon.getLabel().toLowerCase() + ") is dead."
				+ (last && party.isLeader() && config.autoBossReport() ? " The boss report goes to the party." : ""));
		}
		if (npc.isDead())
		{
			// the fight may end with this death, so take it before
			Fight fight = tracker.getCurrent();
			tracker.npcDied(System.currentTimeMillis(), key,
				wasMarked || bosses.isBoss(npc.getId(), Names.clean(npc.getName())));
			if (fight != null)
			{
				storage.monsterDied(fight, key);
			}
			barrows.died(System.currentTimeMillis(), Names.clean(npc.getName()));
		}
		else
		{
			tracker.npcGone(System.currentTimeMillis(), key);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() == ChatMessageType.GAMEMESSAGE)
		{
			long now = System.currentTimeMillis();
			killCounts.message(now, e.getMessage());
			raidPoints.message(now, e.getMessage());
			raids.gameMessage(now, e.getMessage());
			barrows.gameMessage(now, e.getMessage(), tracker);
			takeGameTime(now, e.getMessage());
			combat.gameMessage(e.getMessage());
		}
	}

	private void takeGameTime(long now, String message)
	{
		GameTimes.Time time = GameTimes.parse(message);
		Fight ended = time == null ? null : tracker.gameTime(now, time);
		if (ended != null)
		{
			storage.gameTimeChanged(ended);
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		long now = System.currentTimeMillis();
		int tick = client.getTickCount();

		updateRaidScaling();
		combat.tick(tick);

		List<HealthWatcher.Heal> heals = healing.endTick(tick);
		if (tracker.getCurrent() != null)
		{
			heals.forEach(heal -> addOwnHeal(now, heal));
		}

		tracker.tick(now);
		barrows.tick(now, tick);
		partyUpdates.send();
		reports.sendDue(tick);
		storage.tick(now);
		raids.tick(now);
		party.tick(now, raids.isInsideRaid());
		refreshOverlay();
		hideOverheadChat();
		if (++ticks % PANEL_EVERY_TICKS == 0)
		{
			updatePanel();
		}
	}

	private void addOwnHeal(long now, HealthWatcher.Heal heal)
	{
		if (heal.type == HealType.RECEIVED)
		{
			return;
		}
		tracker.heal(now, localName(), PlayerKind.LOCAL, heal.type, heal.amount, heal.overheal);
		partyUpdates.addHeal(heal);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		GameState state = e.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			storage.loadAccount(client.getAccountHash());
			barrows.loggedIn();
		}
		else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			// the party first: an error in the clean-up below must not keep you in the party
			party.loggedOut(state == GameState.LOGIN_SCREEN);
			if (state == GameState.LOGIN_SCREEN)
			{
				executor.schedule(party::leaveIfStillLoggedOut, PartyManager.LEAVE_AFTER_LOGOUT_MS,
					TimeUnit.MILLISECONDS);
			}
			long now = System.currentTimeMillis();
			String reason = state == GameState.HOPPING ? "World hop" : "Logged out";
			healing.reset();
			combat.reset();
			matcher.clear();
			defence.clear();
			raids.reset();
			barrows.loggedOut(now);
			partyUpdates.clear();
			tracker.endRaid(now, reason);
			tracker.endNow(now, reason);
			storage.save();
			refreshOverlay();
		}
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown e)
	{
		// not on the client thread, but the game loop has stopped by now
		try
		{
			long now = System.currentTimeMillis();
			party.clientClosed();
			barrows.clientClosed(now);
			tracker.endNow(now, "Client closed");
			Future<?> write = storage.saveNow();
			if (write != null)
			{
				e.waitFor(write);
			}
		}
		catch (ConcurrentModificationException ex)
		{
			log.warn("Could not save on shutdown", ex);
		}
	}

	@Override
	public void fightEnded(Fight fight)
	{
		storage.fightEnded(fight);
		reports.fightEnded(fight, client.getTickCount());
	}

	/** Shift right-click on an NPC: the leader (or a solo player) can mark it as the party's target. */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (e.getType() != MenuAction.EXAMINE_NPC.getId() || !client.isKeyPressed(KeyCode.KC_SHIFT) || !party.canMark())
		{
			return;
		}
		NPC npc = e.getMenuEntry().getNpc();
		if (npc == null)
		{
			return;
		}
		String key = TargetMatcher.key(client.getWorld(), npc.getIndex());
		String name = Names.clean(npc.getName());
		MarkedTarget.Mark current = marked.get(key);
		if (current != null)
		{
			client.createMenuEntry(-1)
			.setOption("Unmark target")
			.setTarget(e.getTarget())
			.setType(MenuAction.RUNELITE)
			.onClick(click -> clientThread.invokeLater(() ->
			{
				party.unmark(key);
				updatePanel();
			}));
		}
		// the parent entry marks with the next free icon; its submenu picks one
		TargetIcon next = current != null ? current.icon : marked.nextIcon();
		MenuEntry parent = client.createMenuEntry(-1)
			.setOption(current != null ? "Change target icon" : "Mark target")
			.setTarget(e.getTarget())
			.setType(MenuAction.RUNELITE)
			.onClick(click -> markNpc(key, npc.getId(), name, next));
		Menu icons = parent.createSubMenu();
		for (TargetIcon icon : TargetIcon.values())
		{
			icons.createMenuEntry(-1)
			.setOption(ColorUtil.wrapWithColorTag(icon.getLabel(), icon.getColor()))
			.setType(MenuAction.RUNELITE)
			.onClick(click -> markNpc(key, npc.getId(), name, icon));
		}
	}

	private void markNpc(String key, int npcId, String name, TargetIcon icon)
	{
		clientThread.invokeLater(() ->
		{
			party.mark(key, npcId, name, icon);
			updatePanel();
		});
	}

	// ---- party ----

	@Subscribe
	public void onPartybaseDpsUpdate(PartybaseDpsUpdate update)
	{
		clientThread.invokeLater(() -> partyUpdates.receive(update));
	}

	@Subscribe
	public void onPartybaseDpsPartyMessage(PartybaseDpsPartyMessage message)
	{
		clientThread.invokeLater(() ->
		{
			party.onMessage(message);
			updatePanel();
		});
	}

	@Subscribe
	public void onUserPart(UserPart e)
	{
		clientThread.invokeLater(() -> party.onMemberLeft(e.getMemberId()));
	}

	// ---- overlay and panel ----

	FightReport overlayReport()
	{
		return overlayCache;
	}

	BossMeter bossMeter()
	{
		return bossCache;
	}

	boolean isFightRunning()
	{
		return overallGoingOn;
	}

	boolean isFightPaused()
	{
		return overallPaused;
	}

	String barrowsRunTime()
	{
		return barrows.liveTime();
	}

	boolean isBarrowsRunPaused()
	{
		return barrows.isPaused();
	}

	/** The overlays draw on their own thread, so what they show is built here on the client thread. */
	private void refreshOverlay()
	{
		long now = System.currentTimeMillis();
		overlayCache = views.overlayReport(now);
		overallGoingOn = tracker.isOverallGoingOn(now);
		// between two fights of the overall meter, or no hits for a while in the running fight
		Fight running = tracker.getCurrent();
		overallPaused = running != null ? running.isPaused(now) : overallGoingOn;
		views.updateFun(now);
		bossCache = views.bossMeter(now);
		hitChanceCache = config.showHitChance() ? hitChance.now() : null;
		defenceCache = views.defenceLabels(config.showDefence(), hitChanceCache);
	}

	/**
	 * What the raid scales its monsters by. The highest Hitpoints level in the party is not shown by the game,
	 * so your own is used.
	 */
	private void updateRaidScaling()
	{
		raidScaling.update(tracker.getRaid(), client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSIZE),
			client.getVarbitValue(VarbitID.RAIDS_CHALLENGE_MODE) == 1, client.getRealSkillLevel(Skill.HITPOINTS),
			client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL));
	}

	/** Target key to the lines above that NPC; read by the defence overlay. */
	Map<String, String[]> defenceLabels()
	{
		return defenceCache;
	}

	FunEffects fun()
	{
		return fun;
	}

	/** Builds the panel data on the client thread and hands it to Swing. */
	void updatePanel()
	{
		DpsPanel p = panel;
		if (p == null || !p.isPanelOpen())
		{
			return;
		}
		DpsPanel.Data data = views.panelData(p.getSelectedId(), localName(), System.currentTimeMillis(),
			hitChanceCache);
		data.marks = marked.all();
		data.canMark = party.canMark();
		SwingUtilities.invokeLater(() -> p.update(data));
	}

	// ---- actions from the overlay menu and the panel (Swing thread) ----

	void joinParty(String name)
	{
		clientThread.invoke(() ->
		{
			party.join(name);
			updatePanel();
		});
	}

	void randomPartyName()
	{
		clientThread.invoke(() ->
		{
			String name = party.randomName();
			DpsPanel p = panel;
			if (name != null && p != null)
			{
				SwingUtilities.invokeLater(() -> p.setPartyName(name));
			}
		});
	}

	void rejoinParty()
	{
		clientThread.invoke(() ->
		{
			party.rejoin();
			updatePanel();
		});
	}

	/** Removes one mark, or all with null (DPS tab buttons). */
	void clearMark(String key)
	{
		clientThread.invoke(() ->
		{
			party.unmark(key);
			updatePanel();
		});
	}

	/** The current marks; read by the marked target overlay. */
	List<MarkedTarget.Mark> marks()
	{
		return marked.all();
	}

	// ---- read by the Partybase panel (any thread) ----

	/** When the current party became active (epoch ms), 0 in no party. */
	public long partyStartMs()
	{
		return party.getPartyStartMs();
	}

	/** The party leader's name, or null. */
	public String leaderName()
	{
		return party.getLeaderName();
	}

	/** Member id of the party leader, 0 when unknown. Client thread. */
	public long leaderId()
	{
		return party.getLeaderId();
	}

	/** You lead the party. Any thread. */
	public boolean youLead()
	{
		return party.youLead();
	}

	/** Hands the leader role to the member (right-click in Party info). */
	public void makeLeader(long memberId)
	{
		clientThread.invoke(() ->
		{
			party.makeLeader(memberId);
			updatePanel();
		});
	}

	void leaveParty()
	{
		clientThread.invoke(() ->
		{
			party.leave();
			updatePanel();
		});
	}

	void kickFromParty(long memberId, String name)
	{
		clientThread.invoke(() ->
		{
			party.kick(memberId, name);
			updatePanel();
		});
	}

	/** Reports what the overlay shows: the fight (or session), or the boss of the boss meter. */
	void reportOverlayToParty(ReportKind kind)
	{
		clientThread.invoke(() ->
		{
			long now = System.currentTimeMillis();
			reports.sendByHand(views.overlayFight(kind, now), kind, views.isSessionOverlay(), now);
		});
	}

	/** Reports the fight picked in the panel. */
	void reportPanelToParty(ReportKind kind)
	{
		DpsPanel p = panel;
		String selected = p == null ? null : p.getSelectedId();
		clientThread.invoke(() ->
		{
			long now = System.currentTimeMillis();
			reports.sendByHand(views.panelFight(selected, now), kind, DpsPanel.SESSION_CHOICE.equals(selected), now);
		});
	}

	void setMode(MeterMode mode)
	{
		configManager.setConfiguration(PartybaseConfig.GROUP, PartybaseConfig.MODE_KEY, mode);
	}

	void setBossOnly(boolean bossOnly)
	{
		configManager.setConfiguration(PartybaseConfig.GROUP, PartybaseConfig.BOSS_ONLY_KEY, bossOnly);
	}

	void setScope(PartybaseConfig.Scope scope)
	{
		configManager.setConfiguration(PartybaseConfig.GROUP, PartybaseConfig.SCOPE_KEY, scope);
	}

	void resetSession()
	{
		runAndRefresh(() ->
		{
			tracker.resetSession(System.currentTimeMillis());
			storage.save();
		});
	}

	void endFight()
	{
		runAndRefresh(() -> tracker.endNow(System.currentTimeMillis(), "Ended by you"));
	}

	void resetMonster(String name)
	{
		runAndRefresh(() -> storage.resetMonster(name));
	}

	void resetAllMonsters()
	{
		runAndRefresh(storage::resetAllMonsters);
	}

	void clearHistory()
	{
		runAndRefresh(() ->
		{
			tracker.clearHistory();
			storage.save();
		});
	}

	/** The panel sections that are shown or hidden against their default, comma separated. */
	String panelToggles()
	{
		String toggles = configManager.getConfiguration(PartybaseConfig.STATE_GROUP, PANEL_TOGGLES_KEY);
		return toggles == null ? "" : toggles;
	}

	void savePanelToggles(String toggles)
	{
		configManager.setConfiguration(PartybaseConfig.STATE_GROUP, PANEL_TOGGLES_KEY, toggles);
	}

	void requestPanelUpdate()
	{
		clientThread.invoke(this::updatePanel);
	}

	/** Runs a change to the fights on the client thread and shows the result. */
	private void runAndRefresh(Runnable action)
	{
		clientThread.invoke(() ->
		{
			action.run();
			refreshOverlay();
			updatePanel();
		});
	}

	private String localName()
	{
		return Names.localName(client);
	}
}
