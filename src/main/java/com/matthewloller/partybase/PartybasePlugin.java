/*
 * Copyright (c) 2020, TheStonedTurtle <https://github.com/TheStonedTurtle>
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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.matthewloller.partybase.data.PartyPlayer;
import com.matthewloller.partybase.data.PrayerData;
import com.matthewloller.partybase.data.Prayers;
import com.matthewloller.partybase.data.events.PartyMiscChange;
import com.matthewloller.partybase.data.events.PartybaseBatchedChange;
import com.matthewloller.partybase.defence.DefenceModule;
import com.matthewloller.partybase.dps.DpsModule;
import com.matthewloller.partybase.metronome.MetronomeModule;
import com.matthewloller.partybase.party.PartyLink;
import com.matthewloller.partybase.party.PartybaseMember;
import com.matthewloller.partybase.party.events.PartybaseMemberAvatar;
import com.matthewloller.partybase.party.events.PartybaseMemberData;
import com.matthewloller.partybase.party.events.PartybasePartyChanged;
import com.matthewloller.partybase.party.events.PartybaseUserPart;
import com.matthewloller.partybase.party.events.PartybaseUserSync;
import com.matthewloller.partybase.tiles.TileModule;
import com.matthewloller.partybase.ui.PlayerPanel;
import com.matthewloller.partybase.ui.prayer.PrayerSprites;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.party.PartyDataService;
import net.runelite.client.party.PartyDataType;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.slayer.SlayerPlugin;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Partybase",
	description = "Party side panel with member info and a party DPS meter",
	tags = {"party", "group", "dps", "damage", "meter", "boss", "pvm", "raid"},
	// names the data folder: .runelite/plugin-data/partybase
	internalName = "partybase"
)
// the DPS hit chance counts the slayer helmet on the task that RuneLite's Slayer plugin knows
@PluginDependency(SlayerPlugin.class)
public class PartybasePlugin extends Plugin
{
	private static final BufferedImage ICON = ImageUtil.loadImageResource(PartybasePlugin.class, "icon.png");
	public static final Set<Integer> RUNEPOUCH_ITEM_IDS = ImmutableSet.of(
			ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER, ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER
		);
	public static final Set<Integer> DIZANAS_QUIVER_IDS = ImmutableSet.<Integer>builder()
		.addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.DIZANAS_QUIVER_CHARGED)))
		.addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.DIZANAS_QUIVER_INFINITE)))
		.addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.SKILLCAPE_MAX_DIZANAS)))
		.build();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Getter
	@Inject
	private PartybaseConfig config;

	@Inject
	private PartyLink partyLink;

	@Inject
	private PartyDataService partyDataService;

	@Inject
	SpriteManager spriteManager;

	@Inject
	ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PartyReminderOverlay partyReminderOverlay;

	@Inject
	private EventBus eventBus;

	/** The DPS meter (ported from GoldenDPS); its panel is the DPS tab. */
	@Getter
	@Inject
	private DpsModule dps;

	/** The defence tracker (ported from Better Party Defence). */
	@Inject
	private DefenceModule defence;

	/** The tick metronome (ported from Visual Metronome), synced to the party leader. */
	@Getter
	@Inject
	private MetronomeModule metronome;

	/** Tile pack sharing (reads packs from the Tile Packs plugin). */
	@Getter
	@Inject
	private TileModule tiles;

	/** The started modules, in start order. */
	private List<PartybaseModule> modules = Collections.emptyList();

	/** The plugin's data folder, .runelite/plugin-data/partybase. */
	public Filepath getDataDirectory() throws IOException
	{
		return getPluginDirectory();
	}

	@Provides
	PartybaseConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PartybaseConfig.class);
	}

	@Getter
	private final Map<Long, PartyPlayer> partyMembers = new HashMap<>();

	@Getter
	private PartyPlayer myPlayer = null;

	private NavigationButton navButton;
	private boolean addedButton = false;

	private PartybasePanel panel;
	private Instant lastLogout;

	// All events should be deferred to the next game tick
	private PartybaseBatchedChange currentChange = new PartybaseBatchedChange();

	@Override
	protected void startUp() throws Exception
	{
		// the party first: the modules register their messages with it
		partyLink.startUp();
		modules = Arrays.asList(dps, defence, metronome, tiles);
		for (PartybaseModule module : modules)
		{
			module.startUp(this);
			eventBus.register(module);
		}

		panel = new PartybasePanel(this);
		navButton = NavigationButton.builder()
			.tooltip("Partybase")
			.icon(ICON)
			.priority(7)
			.panel(panel)
			.build();

		partyLink.registerMessage(PartybaseBatchedChange.class);
		partyDataService.subscribe(this,
			PartyDataType.CHARACTER_NAME,
			PartyDataType.ENERGY,
			PartyDataType.SPEC,
			PartyDataType.INV,
			PartyDataType.WORN,
			PartyDataType.ACTIVE_PRAYERS,
			PartyDataType.RUNE_POUCH,
			PartyDataType.QUIVER
		);
		partyDataService.subscribe(this, Skill.values());

		if (isInParty() || config.alwaysShowIcon())
		{
			clientToolbar.addNavigation(navButton);
			addedButton = true;
		}

		if (isInParty())
		{
			clientThread.invokeLater(() ->
			{
				myPlayer = new PartyPlayer(partyLink.getLocalMember(), client, itemManager, clientThread);
				partyLink.send(new UserSync());
				partyLink.send(partyPlayerAsBatchedChange());
			});
		}

		lastLogout = Instant.now();
		overlayManager.add(partyReminderOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		if (isInParty())
		{
			final PartybaseBatchedChange cleanUserInfo = partyPlayerAsBatchedChange();
			cleanUserInfo.setM(Collections.emptySet());
			partyLink.send(cleanUserInfo);
		}

		clientToolbar.removeNavigation(navButton);
		addedButton = false;
		partyMembers.clear();
		partyLink.unregisterMessage(PartybaseBatchedChange.class);
		partyDataService.unsubscribe(this, PartyDataType.values());
		partyDataService.unsubscribe(this, Skill.values());
		currentChange = new PartybaseBatchedChange();
		panel.getPlayerPanelMap().clear();
		lastLogout = null;
		overlayManager.remove(partyReminderOverlay);

		for (int i = modules.size() - 1; i >= 0; i--)
		{
			eventBus.unregister(modules.get(i));
			modules.get(i).shutDown();
		}
		modules = Collections.emptyList();
		partyLink.shutDown();
	}

	@Subscribe
	protected void onConfigChanged(final ConfigChanged c)
	{
		if (!c.getGroup().equals("partybase"))
		{
			return;
		}

		if (config.alwaysShowIcon())
		{
			if (!addedButton)
			{
				clientToolbar.addNavigation(navButton);
				addedButton = true;
			}
		}
		else if (addedButton && !isInParty())
		{
			clientToolbar.removeNavigation(navButton);
			addedButton = false;
		}
		addedButton = config.alwaysShowIcon();

		switch (c.getKey())
		{
			case "autoExpandMembers":
				panel.updatePartyMembersExpand(config.autoExpandMembers());
				break;
			case "showPartyControls":
				panel.updatePartyControls();
				break;
			case "showPartyPassphrase":
				panel.syncPartyPassphraseVisibility();
				break;
			case "displayVirtualLevels":
				panel.updateDisplayVirtualLevels();
				break;
			case "displayPlayerWorlds":
				panel.updateDisplayPlayerWorlds();
				break;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged c)
	{
		if (!isInParty())
		{
			return;
		}

		if (c.getGameState() == GameState.LOGIN_SCREEN)
		{
			lastLogout = Instant.now();
		}

		if (myPlayer == null)
		{
			myPlayer = new PartyPlayer(partyLink.getLocalMember(), client, itemManager, clientThread);
			final PartybaseBatchedChange ce = partyPlayerAsBatchedChange();
			partyLink.send(ce);
			return;
		}

		if (c.getGameState() == GameState.LOGGED_IN)
		{
			PartyMiscChange e = new PartyMiscChange(PartyMiscChange.PartyMisc.W, client.getWorld());
			if (myPlayer.getWorld() == e.getV())
			{
				return;
			}

			myPlayer.setWorld(e.getV());
			currentChange.getM().add(e);
		}

		if (c.getGameState() == GameState.LOGIN_SCREEN)
		{
			if (myPlayer.getWorld() == 0)
			{
				return;
			}

			myPlayer.setWorld(0);
			currentChange.getM().add(new PartyMiscChange(PartyMiscChange.PartyMisc.W, 0));

			partyLink.send(currentChange);
			currentChange = new PartybaseBatchedChange();
		}
	}

	public boolean isInParty()
	{
		return partyLink.isInParty();
	}

	public boolean isLocalPlayer(long id)
	{
		return partyLink.getLocalMember() != null && partyLink.getLocalMember().getMemberId() == id;
	}

	@Subscribe
	public void onPartybaseUserPart(final PartybaseUserPart event)
	{
		final PartyPlayer removed = partyMembers.remove(event.getMemberId());
		if (removed != null)
		{
			SwingUtilities.invokeLater(() -> panel.removePartyPlayer(removed));
		}

		if (addedButton && (!isInParty() || partyLink.getMembers().isEmpty()) && !config.alwaysShowIcon())
		{
			clientToolbar.removeNavigation(navButton);
			addedButton = false;
		}
	}

	@Subscribe
	public void onPartybaseUserSync(final PartybaseUserSync event)
	{
		if (!addedButton)
		{
			clientToolbar.addNavigation(navButton);
			addedButton = true;
		}

		if (myPlayer != null)
		{
			final PartybaseBatchedChange c = partyPlayerAsBatchedChange();
			if (c.isValid())
			{
				partyLink.send(c);
			}
			return;
		}

		clientThread.invoke(() ->
		{
			myPlayer = new PartyPlayer(partyLink.getLocalMember(), client, itemManager, clientThread);
			final PartybaseBatchedChange c = partyPlayerAsBatchedChange();
			if (c.isValid())
			{
				partyLink.send(c);
			}
		});
	}

	@Subscribe
	public void onPartybasePartyChanged(final PartybasePartyChanged event)
	{
		partyMembers.clear();
		SwingUtilities.invokeLater(() ->
		{
			panel.clearSidebar();
			panel.renderSidebar();
		});
		myPlayer = null;

		panel.updateParty();

		if (!isInParty())
		{
			if (!config.alwaysShowIcon())
			{
				clientToolbar.removeNavigation(navButton);
				addedButton = false;
			}

			panel.getPlayerPanelMap().clear();
			return;
		}
		else if (!addedButton)
		{
			clientToolbar.addNavigation(navButton);
			addedButton = true;
		}

		config.setPreviousPartyId(event.getPassphrase());
	}

	@Subscribe
	public void onGameTick(final GameTick tick)
	{
		if (!isInParty() || client.getLocalPlayer() == null || partyLink.getLocalMember() == null)
		{
			return;
		}

		// To reduce server load we should only process changes every X ticks
		if (client.getTickCount() % messageFreq(partyLink.getMembers().size()) != 0)
		{
			return;
		}

		// First time logging in, or they changed accounts so resend the entire player object
		if (myPlayer == null || !Objects.equals(client.getLocalPlayer().getName(), myPlayer.getUsername()))
		{
			myPlayer = new PartyPlayer(partyLink.getLocalMember(), client, itemManager, clientThread);
			final PartybaseBatchedChange c = partyPlayerAsBatchedChange();
			partyLink.send(c);
			return;
		}

		if (myPlayer.getPrayers() == null)
		{
			myPlayer.setPrayers(new Prayers(client));
			includeAvailableAndUnlockedPrayersInBatchedChange(currentChange);
		}
		else
		{
			// TODO: Find a better way to do this that isn't every game tick since we no longer track enabled prayers
			final Collection<Prayer> available = new ArrayList<>();
			final Collection<Prayer> unlocked = new ArrayList<>();
			boolean change = false;
			for (final PrayerSprites p : PrayerSprites.values())
			{
				change = myPlayer.getPrayers().updatePrayerState(p, client) || change;

				// Store the data for this prayer regardless of if it changes since any update
				// will assume all prayers are not available & disabled
				final PrayerData data = myPlayer.getPrayers().getPrayerData().get(p.getPrayer());
				if (data.isAvailable())
				{
					available.add(p.getPrayer());
				}

				if (data.isUnlocked())
				{
					unlocked.add(p.getPrayer());
				}
			}

			// Send both arrays as bit-packed ints whenever any prayer has changed.
			if (change)
			{
				currentChange.setAp(PartybaseBatchedChange.pack(available));
				currentChange.setUp(PartybaseBatchedChange.pack(unlocked));
			}
		}


		if (myPlayer.getSpellbook() == -1)
		{
			updateSpellbook();
		}

		if (currentChange.isValid())
		{
			currentChange.setMemberId(partyLink.getLocalMember().getMemberId()); // Add member ID before sending
			currentChange.removeDefaults();
			partyLink.send(currentChange);

			currentChange = new PartybaseBatchedChange();
		}
	}

	@Subscribe
	public void onPartybaseMemberData(final PartybaseMemberData event)
	{
		// Member data (stats, inventory, gear, prayers) comes from RuneLite's PartyDataService or, on a relay
		// server, from Partybase's own sender. Your own data is shown too, in your collapsed banner.
		final PartybaseMember newData = partyLink.getMemberById(event.getMemberId());
		if (newData == null)
		{
			return;
		}
		final PartyPlayer partyPlayer = partyMembers.computeIfAbsent(
				event.getMemberId(), k -> new PartyPlayer());

		clientThread.invoke(() ->
		{
			final boolean breakingChange = partyPlayer.updateMemberInfo(newData, itemManager);

			handlePlayerDataUpdate(event.getMemberId(), breakingChange);
		});
	}

	private void handlePlayerDataUpdate(long memberId, boolean breakingBannerChange)
	{
		// you are shown too (collapsed, see PartybasePanel), from the data PartyDataService keeps for you
		final PartyPlayer player = partyMembers.get(memberId);
		if (player == null)
		{
			log.info("UNKNOWN MEMBER UPDATE: {}", memberId);
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			panel.drawPlayerPanel(player, breakingBannerChange);
		});
	}

	@Subscribe
	public void onVarbitChanged(final VarbitChanged event)
	{
		if (myPlayer == null || myPlayer.getStats() == null || !isInParty())
		{
			return;
		}

		// Cap at 1 so we only transmit when stamina changes states (active vs expires)
		final boolean staminaActive = client.getVarbitValue(VarbitID.STAMINA_DURATION) > 0;
		if (staminaActive != myPlayer.isStaminaActive())
		{
			myPlayer.setStaminaActive(staminaActive);
			currentChange.getM().add(new PartyMiscChange(PartyMiscChange.PartyMisc.ST, staminaActive));
		}

		final int poison = client.getVarpValue(VarPlayerID.POISON);
		if (poison != myPlayer.getPoison())
		{
			myPlayer.setPoison(poison);
			currentChange.getM().add(new PartyMiscChange(PartyMiscChange.PartyMisc.P, poison));
		}

		final int disease = client.getVarpValue(VarPlayerID.DISEASE);
		if (disease != myPlayer.getDisease())
		{
			myPlayer.setDisease(disease);
			currentChange.getM().add(new PartyMiscChange(PartyMiscChange.PartyMisc.D, disease));
		}

		if (event.getVarbitId() == VarbitID.SPELLBOOK)
		{
			updateSpellbook();
		}
	}

	@Subscribe
	public void onPartybaseBatchedChange(PartybaseBatchedChange e)
	{
		// your own changes come back from the party server too; they keep your banner up to date
		// create new PartyPlayer for this member if they don't already exist
		final PartyPlayer player = partyMembers.computeIfAbsent(
				e.getMemberId(),
				k -> new PartyPlayer(partyLink.getMemberById(e.getMemberId()), itemManager));

		// Create placeholder prayer object
		if (player.getPrayers() == null && (e.getAp() != null || e.getUp() != null))
		{
			player.setPrayers(new Prayers());
		}

		clientThread.invoke(() ->
		{
			e.process(player, itemManager);

			SwingUtilities.invokeLater(() ->
			{
				panel.drawPlayerPanel(player, e.hasBreakingBannerChange());
			});
		});
	}

	@Subscribe
	public void onPartybaseMemberAvatar(PartybaseMemberAvatar e)
	{
		if (partyMembers.get(e.getMemberId()) == null)
		{
			return;
		}

		final PartyPlayer player = partyMembers.get(e.getMemberId());
		player.setMember(partyLink.getMemberById(e.getMemberId()));
		SwingUtilities.invokeLater(() ->
		{
			final PlayerPanel p = panel.getPlayerPanelMap().get(e.getMemberId());
			if (p != null)
			{
				p.getBanner().refreshStats();
			}
		});
	}

	public void changeParty(String passphrase)
	{
		passphrase = passphrase.replace(" ", "-").trim();
		if (passphrase.isEmpty())
		{
			return;
		}

		for (int i = 0; i < passphrase.length(); ++i)
		{
			char ch = passphrase.charAt(i);
			if (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '-')
			{
				JOptionPane.showMessageDialog(panel.getControlsPanel(),
					"Party passphrase must be a combination of alphanumeric or hyphen characters.",
					"Invalid party passphrase",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		partyLink.changeParty(passphrase);
		panel.updateParty();
	}

	public void createParty()
	{
		// Create party
		clientThread.invokeLater(() -> changeParty(partyLink.generatePassphrase()));
	}

	public String getPartyPassphrase()
	{
		return partyLink.getPartyPassphrase();
	}

	public void leaveParty()
	{
		partyLink.changeParty(null);
		panel.updateParty();
	}

	private void includeAvailableAndUnlockedPrayersInBatchedChange(final PartybaseBatchedChange change)
	{
		final Collection<Prayer> available = new ArrayList<>();
		final Collection<Prayer> unlocked = new ArrayList<>();
		for (final PrayerSprites p : PrayerSprites.values())
		{
			final PrayerData data = myPlayer.getPrayers().getPrayerData().get(p.getPrayer());
			if (data.isAvailable())
			{
				available.add(p.getPrayer());
			}

			if (data.isUnlocked())
			{
				unlocked.add(p.getPrayer());
			}
		}

		change.setAp(PartybaseBatchedChange.pack(available));
		change.setUp(PartybaseBatchedChange.pack(unlocked));
	}

	public PartybaseBatchedChange partyPlayerAsBatchedChange()
	{
		final PartybaseBatchedChange c = new PartybaseBatchedChange();
		if (myPlayer == null)
		{
			return c;
		}

		// A lot of this data is now handled by the PartyDataService

		// Misc
		c.getM().add(new PartyMiscChange(PartyMiscChange.PartyMisc.ST, myPlayer.isStaminaActive()));
		c.getM().add(new PartyMiscChange(PartyMiscChange.PartyMisc.P, myPlayer.getPoison()));
		c.getM().add(new PartyMiscChange(PartyMiscChange.PartyMisc.D, myPlayer.getDisease()));
		c.getM().add(new PartyMiscChange(PartyMiscChange.PartyMisc.W, myPlayer.getWorld()));
		c.getM().add(new PartyMiscChange(PartyMiscChange.PartyMisc.SP, myPlayer.getSpellbook()));

		if (myPlayer.getPrayers() != null)
		{
			includeAvailableAndUnlockedPrayersInBatchedChange(c);
		}

		c.setMemberId(partyLink.getLocalMember().getMemberId()); // Add member ID before sending
		c.removeDefaults();

		return c;
	}

	@Schedule(
		period = 10,
		unit = ChronoUnit.SECONDS
	)
	public void checkIdle()
	{
		if (client.getGameState() != GameState.LOGIN_SCREEN)
		{
			return;
		}

		if (lastLogout != null && lastLogout.isBefore(Instant.now().minus(30, ChronoUnit.MINUTES))
			&& partyLink.isInParty())
		{
			log.info("Leaving party due to inactivity");
			partyLink.changeParty(null);
		}
	}

	private static int messageFreq(int partySize)
	{
		// introduce a tick delay for each member >6
		// Default the message frequency to every 2 ticks since this plugin sends a lot of data
		return Math.max(2, partySize - 6);
	}

	private void updateSpellbook()
	{
		int spellbook = client.getVarbitValue(VarbitID.SPELLBOOK);
		myPlayer.setSpellbook(spellbook);
		currentChange.getM().add(new PartyMiscChange(PartyMiscChange.PartyMisc.SP, spellbook));
	}
}
