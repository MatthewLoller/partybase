/*
 * Copyright (c) 2026, Heisty <https://github.com/Falsary>
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
package com.matthewloller.partybase.defence;

import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.defence.DefenceTracker.DefenceState;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

/**
 * Owns status-bar info boxes and boss-effect presentation.
 */
final class DefenceDisplayController
{
	private final Client client;
	private final ClientThread clientThread;
	private final PartybaseConfig config;
	private final DefenceTracker tracker;
	private final InfoBoxManager infoBoxManager;
	private final ItemManager itemManager;
	private final SkillIconManager skillIconManager;
	private final InitialSyncDisplayGate initialSyncDisplayGate;

	private DefenceOverlayRenderer.IconSource iconSource;
	private DefenceModule owner;
	private DefenceInfoBox defenceBox;
	private DefenceInfoBox magicDefenceBox;
	private BossEffectTracker bossEffects;
	private BossEffectInfoBoxes bossEffectBoxes;
	private final Map<SpecialWeapon, SpecWeaponInfoBox> specWeaponBoxes = new LinkedHashMap<>();

	DefenceDisplayController(
		Client client,
		ClientThread clientThread,
		PartybaseConfig config,
		DefenceTracker tracker,
		InfoBoxManager infoBoxManager,
		ItemManager itemManager,
		SkillIconManager skillIconManager,
		InitialSyncDisplayGate initialSyncDisplayGate)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.tracker = tracker;
		this.infoBoxManager = infoBoxManager;
		this.itemManager = itemManager;
		this.skillIconManager = skillIconManager;
		this.initialSyncDisplayGate = initialSyncDisplayGate;
	}

	void start(DefenceModule plugin)
	{
		owner = plugin;
		bossEffects = new BossEffectTracker(client, clientThread, tracker);
		bossEffectBoxes = new BossEffectInfoBoxes(client, tracker);
		iconSource = new DefenceOverlayRenderer.IconSource(client, skillIconManager, config);
	}

	void shutdown()
	{
		reset();
		bossEffects = null;
		bossEffectBoxes = null;
		iconSource = null;
		owner = null;
	}

	DefenceOverlayRenderer.IconSource iconSource()
	{
		return iconSource;
	}

	void onSpecialEnergy(int energy)
	{
		if (bossEffects != null)
		{
			bossEffects.onSpecialEnergy(energy);
		}
	}

	void onHitsplatApplied(HitsplatApplied event)
	{
		if (bossEffects != null)
		{
			bossEffects.onHitsplatApplied(event);
		}
	}

	void onAnimationChanged(Actor actor)
	{
		if (bossEffects != null)
		{
			bossEffects.onLocalAnimation(actor);
		}
	}

	void onProjectileMoved(Projectile projectile)
	{
		if (bossEffects != null)
		{
			bossEffects.onProjectileMoved(projectile);
		}
	}

	void onGraphicChanged(GraphicChanged event)
	{
		if (bossEffects != null)
		{
			bossEffects.onGraphicChanged(event.getActor());
		}
	}

	void settleKeris()
	{
		if (bossEffects != null)
		{
			bossEffects.settleKeris(owner);
		}
	}

	void applyKerisTimer(NPC npc, int expiry, long hitAtMillis)
	{
		if (bossEffects != null)
		{
			bossEffects.applyKerisTimer(npc, expiry, hitAtMillis);
		}
	}

	void clearBoss(BossDefence boss)
	{
		if (bossEffects != null)
		{
			bossEffects.clearBoss(boss);
		}
	}

	boolean hasDefenceInfoBoxes()
	{
		return defenceBox != null || magicDefenceBox != null;
	}

	void update(boolean ocellusPhaseOneEnded)
	{
		DefenceState state = tracker.state();
		boolean live = state != null && trackedNpcIsLive(state, ocellusPhaseOneEnded);
		boolean awaitingInitialSync = state != null
			&& initialSyncDisplayGate.isWaiting(state, client.getTickCount());
		boolean showDefence = config.defenceInfoBox() && live && !awaitingInitialSync
			&& (tracker.hasDefenceSpecHistory() || config.defenceAlwaysShow());
		boolean showMagic = config.magicDefence() && config.magicDefenceInfoBox() && live
			&& !awaitingInitialSync
			&& DefenceReadout.magicDefenceVisible(state)
			&& (tracker.hasMagicDefenceSpecHistory() || config.defenceAlwaysShow());

		updateDefenceBox(showDefence);
		updateMagicDefenceBox(showMagic);
		updateSpecWeaponInfoBoxes(state, live && !awaitingInitialSync);
		if (bossEffects != null)
		{
			bossEffects.updateObservations();
		}
		if (bossEffectBoxes != null)
		{
			bossEffectBoxes.update(owner.getOwner(), infoBoxManager, itemManager, skillIconManager,
				config.showRedKeris(), config.showShadowBarrage(), config.showVulnerability());
		}
	}

	void removeAll()
	{
		if (bossEffectBoxes != null)
		{
			bossEffectBoxes.hide(infoBoxManager);
		}
		clearSpecWeaponInfoBoxes();
		removeBox(defenceBox);
		removeBox(magicDefenceBox);
		defenceBox = null;
		magicDefenceBox = null;
	}

	void reset()
	{
		if (bossEffects != null)
		{
			bossEffects.reset();
		}
		removeAll();
	}

	private void updateDefenceBox(boolean show)
	{
		if (show && defenceBox == null)
		{
			defenceBox = new DefenceInfoBox(infoBoxSkillIcon(Skill.DEFENCE), owner.getOwner(), tracker, config);
			infoBoxManager.addInfoBox(defenceBox);
		}
		else if (!show && defenceBox != null)
		{
			infoBoxManager.removeInfoBox(defenceBox);
			defenceBox = null;
		}
	}

	private void updateMagicDefenceBox(boolean show)
	{
		if (show && magicDefenceBox == null)
		{
			magicDefenceBox = new DefenceInfoBox(infoBoxSkillIcon(Skill.MAGIC), owner.getOwner(), tracker, config,
				DefenceInfoBox.Stat.MAGIC_DEFENCE);
			infoBoxManager.addInfoBox(magicDefenceBox);
		}
		else if (!show && magicDefenceBox != null)
		{
			infoBoxManager.removeInfoBox(magicDefenceBox);
			magicDefenceBox = null;
		}
	}

	private BufferedImage infoBoxSkillIcon(Skill skill)
	{
		if (config.defenceInfoBoxUseThemeSkillIcon() && iconSource != null)
		{
			return skill == Skill.MAGIC ? iconSource.magic(true) : iconSource.defence(true);
		}
		return skillIconManager.getSkillImage(skill);
	}

	private void updateSpecWeaponInfoBoxes(DefenceState state, boolean live)
	{
		if (!config.specWeaponInfoBoxes() || !live || state == null || !state.isDrained())
		{
			clearSpecWeaponInfoBoxes();
			return;
		}

		Map<SpecialWeapon, SpecWeaponSummary> summaries = SpecWeaponSummary.fromHistory(
				tracker.specHistoryForCurrentTarget(state.getNpcIndex()));
		for (SpecialWeapon existing : new ArrayList<>(specWeaponBoxes.keySet()))
		{
			if (!summaries.containsKey(existing))
			{
				infoBoxManager.removeInfoBox(specWeaponBoxes.remove(existing));
			}
		}
		for (Map.Entry<SpecialWeapon, SpecWeaponSummary> entry : summaries.entrySet())
		{
			SpecialWeapon weapon = entry.getKey();
			SpecWeaponInfoBox box = specWeaponBoxes.get(weapon);
			if (box == null)
			{
				int[] itemIds = weapon.getItemID();
				BufferedImage icon = itemIds != null && itemIds.length > 0
					? itemManager.getImage(itemIds[0])
					: null;
				if (icon == null)
				{
					icon = skillIconManager.getSkillImage(Skill.ATTACK);
				}
				box = new SpecWeaponInfoBox(icon, owner.getOwner(), weapon);
				specWeaponBoxes.put(weapon, box);
				infoBoxManager.addInfoBox(box);
			}
			box.update(entry.getValue());
		}
	}

	private void clearSpecWeaponInfoBoxes()
	{
		for (SpecWeaponInfoBox box : specWeaponBoxes.values())
		{
			infoBoxManager.removeInfoBox(box);
		}
		specWeaponBoxes.clear();
	}

	private boolean trackedNpcIsLive(DefenceState state, boolean ocellusPhaseOneEnded)
	{
		if (state.getNpcIndex() < 0)
		{
			return false;
		}
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null || worldView.npcs() == null)
		{
			return false;
		}

		NPC npc = worldView.npcs().byIndex(state.getNpcIndex());
		if (!BossDefence.isLiveNpc(npc))
		{
			return false;
		}
		if (state.isTfaBoss() && npc.getName() != null
			&& "Ocellus".equalsIgnoreCase(Text.removeTags(npc.getName()).trim()))
		{
			BossDefence actor = BossDefence.matchingNpc(npc);
			return actor != null
				&& (actor == BossDefence.TFA_OCELLUS_P1
					|| actor == BossDefence.TFA_OCELLUS_P2
					|| actor == BossDefence.TFA_OCELLUS_P3)
				&& actor.getBaseDef() == state.getBase()
				&& actor.getMinDef() == state.getMin()
				&& (actor != BossDefence.TFA_OCELLUS_P1 || !ocellusPhaseOneEnded);
		}
		return true;
	}

	private void removeBox(DefenceInfoBox box)
	{
		if (box != null)
		{
			infoBoxManager.removeInfoBox(box);
		}
	}
}
