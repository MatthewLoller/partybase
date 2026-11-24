/*
 * Copyright (c) 2026, Heisty <https://github.com/Falsary>
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
import com.matthewloller.partybase.PartybaseConfig.DefenceOverlayPosition;
import com.matthewloller.partybase.defence.DefenceTracker.DefenceState;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class NpcDefenceOverlay extends Overlay
{
	private final Client client;
	private final DefenceTracker tracker;
	private final PartybaseConfig config;
	private final DefenceOverlayRenderer renderer;
	private final InitialSyncDisplayGate initialSyncDisplayGate;

	public NpcDefenceOverlay(Client client, DefenceTracker tracker, PartybaseConfig config,
		DefenceOverlayRenderer.IconSource skillIcons, InitialSyncDisplayGate initialSyncDisplayGate)
	{
		this.client = client;
		this.tracker = tracker;
		this.config = config;
		this.renderer = new DefenceOverlayRenderer(config, skillIcons);
		this.initialSyncDisplayGate = initialSyncDisplayGate;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.defenceHpBar())
		{
			return null;
		}

		graphics.setFont(renderer.font());
		DefenceOverlayPosition position = config.defenceHpBarPosition();
		int yOffset = config.defenceHpBarYOffset();
		for (DefenceState state : tracker.states())
		{
			if (!isDisplayable(state))
			{
				continue;
			}
			NPC npc = liveNpc(state.getNpcIndex());
			if (npc != null)
			{
				renderAtNpc(graphics, state, npc, position, yOffset);
			}
		}
		return null;
	}

	private void renderAtNpc(Graphics2D graphics, DefenceState state, NPC npc,
		DefenceOverlayPosition position, int yOffset)
	{
		// Ocellus P2 uses the model midpoint as its display height.
		int zOffset;
		if (BossDefence.isAttackableOcellusPhaseTwo(npc))
		{
			zOffset = (int) (npc.getLogicalHeight() * 0.5 * position.getHeightFactor());
		}
		else
		{
			zOffset = (int) (npc.getLogicalHeight() * position.getHeightFactor()) + position.getHeightOffset();
		}
		Point anchor = npc.getCanvasTextLocation(graphics, "", zOffset);
		if (anchor == null)
		{
			return;
		}
		int centreX = anchor.getX() + position.getXNudge();
		int baseline = anchor.getY() - yOffset;
		renderer.renderAt(graphics, state, centreX, baseline);
	}

	private boolean isDisplayable(DefenceState state)
	{
		// Config controls previews only; drained state is always shown.
		return state != null && (state.isDrained() || config.defenceAlwaysShow())
			&& state.getNpcIndex() >= 0
			&& !initialSyncDisplayGate.isWaiting(state, client.getTickCount());
	}

	private NPC liveNpc(int index)
	{
		if (index < 0 || client.getTopLevelWorldView() == null || client.getTopLevelWorldView().npcs() == null)
		{
			return null;
		}
		NPC npc = client.getTopLevelWorldView().npcs().byIndex(index);
		return BossDefence.isLiveNpc(npc) ? npc : null;
	}
}
