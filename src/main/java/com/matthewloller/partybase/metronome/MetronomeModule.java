/*
 * Copyright (c) 2020, vincent0955
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

import com.google.inject.Injector;
import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.PartybaseModule;
import com.matthewloller.partybase.PartybasePlugin;
import com.matthewloller.partybase.dps.DpsModule;
import com.matthewloller.partybase.metronome.fullresizableoverlay.Cycle2MetronomeOverlay;
import com.matthewloller.partybase.metronome.fullresizableoverlay.Cycle3MetronomeOverlay;
import com.matthewloller.partybase.metronome.fullresizableoverlay.DefaultCycleMetronomeOverlay;
import com.matthewloller.partybase.party.PartyLink;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * A visual cue every game tick (ported from Visual Metronome). Instead of syncing to a typed-in name, members
 * follow the party leader: the leader's client sends its metronome every tick (PartybaseMetronomeSync) while it
 * shows one, and followers with "Sync with party leader" copy it. Works on RuneLite's party and on a relay server.
 */
@Singleton
public class MetronomeModule implements PartybaseModule, KeyListener
{
	private static final String[] SYNCED_KEYS = {"tickCount", "tickCount2", "tickCount3", "colorCycle"};

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PartybaseConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PartyLink partyLink;

	@Inject
	private DpsModule dps;

	/** Makes the overlays in startUp: they need this module, so they can't be injected into it. */
	@Inject
	private Injector injector;

	private Overlay[] overlays = new Overlay[0];
	private boolean shown;

	protected int currentColorIndex = 0;
	protected int tickCounter = 0;
	protected int tickCounter2 = 0;
	protected int tickCounter3 = 0;
	protected Color currentColor = Color.WHITE;

	public int getCurrentColorIndex()
	{
		return currentColorIndex;
	}

	public int getTickCounter()
	{
		return tickCounter;
	}

	public int getTickCounter2()
	{
		return tickCounter2;
	}

	public int getTickCounter3()
	{
		return tickCounter3;
	}

	public Color getCurrentColor()
	{
		return currentColor;
	}

	@Override
	public void startUp(PartybasePlugin plugin)
	{
		DefaultCycleMetronomeOverlay defaultOverlay = injector.getInstance(DefaultCycleMetronomeOverlay.class);
		Cycle2MetronomeOverlay cycle2Overlay = injector.getInstance(Cycle2MetronomeOverlay.class);
		Cycle3MetronomeOverlay cycle3Overlay = injector.getInstance(Cycle3MetronomeOverlay.class);
		final Dimension defaultSize = new Dimension(config.boxWidth(), config.boxWidth());
		defaultOverlay.setPreferredSize(defaultSize);
		cycle2Overlay.setPreferredSize(defaultSize);
		cycle3Overlay.setPreferredSize(defaultSize);
		overlays = new Overlay[] {defaultOverlay, cycle2Overlay, cycle3Overlay,
			injector.getInstance(MetronomeTileOverlay.class), injector.getInstance(MetronomeNumberOverlay.class),
			injector.getInstance(MouseFollowingOverlay.class)
		};

		updateOverlaysShown();
		keyManager.registerKeyListener(this);
		partyLink.registerMessage(PartybaseMetronomeSync.class);
	}

	@Override
	public void shutDown()
	{
		setOverlaysShown(false);
		overlays = new Overlay[0];

		tickCounter = 0;
		tickCounter2 = 0;
		tickCounter3 = 0;
		currentColorIndex = 0;
		currentColor = config.getTickColor();

		keyManager.unregisterKeyListener(this);
		partyLink.unregisterMessage(PartybaseMetronomeSync.class);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (tickCounter % config.tickCount() == 0)
		{
			tickCounter = 0;
			if (currentColorIndex == config.colorCycle())
			{
				currentColorIndex = 0;
			}
			setCurrentColorByColorIndex(++currentColorIndex);
		}
		tickCounter++;
		if (config.enableCycle2())
		{
			if (tickCounter2 % config.tickCount2() == 0)
			{
				tickCounter2 = 0;
			}
			tickCounter2++;
		}
		if (config.enableCycle3())
		{
			if (tickCounter3 % config.tickCount3() == 0)
			{
				tickCounter3 = 0;
			}
			tickCounter3++;
		}

		// the leader shares its metronome while it shows one and someone could follow
		if (config.metronomeEnabled() && dps.youLead() && partyLink.getMembers().size() > 1)
		{
			partyLink.send(syncMessage());
		}
	}

	PartybaseMetronomeSync syncMessage()
	{
		PartybaseMetronomeSync msg = new PartybaseMetronomeSync();
		msg.tickCounter = tickCounter;
		msg.tickCounter2 = tickCounter2;
		msg.tickCounter3 = tickCounter3;
		msg.colorIndex = currentColorIndex;
		msg.colorCycle = config.colorCycle();
		msg.tickCount = config.tickCount();
		msg.tickCount2 = config.tickCount2();
		msg.tickCount3 = config.tickCount3();
		return msg;
	}

	@Subscribe
	public void onPartybaseMetronomeSync(PartybaseMetronomeSync msg)
	{
		// arrives on the websocket thread; the leader is known on the client thread
		clientThread.invokeLater(() -> applySync(msg));
	}

	void applySync(PartybaseMetronomeSync msg)
	{
		if (!config.metronomeSyncLeader() || dps.youLead() || msg.getMemberId() != dps.leaderId())
		{
			return;
		}
		tickCounter = msg.tickCounter;
		tickCounter2 = msg.tickCounter2;
		tickCounter3 = msg.tickCounter3;
		currentColorIndex = msg.colorIndex;
		setCurrentColorByColorIndex(currentColorIndex);

		// copy the leader's counts so the overlays and settings match; only write what changed
		int[] values = {msg.tickCount, msg.tickCount2, msg.tickCount3, msg.colorCycle};
		int[] current = {config.tickCount(), config.tickCount2(), config.tickCount3(), config.colorCycle()};
		for (int i = 0; i < SYNCED_KEYS.length; i++)
		{
			if (values[i] > 0 && values[i] != current[i])
			{
				configManager.setConfiguration(PartybaseConfig.GROUP, SYNCED_KEYS[i], values[i]);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!PartybaseConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if ("metronomeEnabled".equals(event.getKey()) || "metronomeHidden".equals(event.getKey()))
		{
			updateOverlaysShown();
		}
		if (currentColorIndex > config.colorCycle())
		{
			currentColorIndex = 0;
		}

		if (tickCounter > config.tickCount())
		{
			tickCounter = 0;
		}
		if (tickCounter2 > config.tickCount2())
		{
			tickCounter2 = 0;
		}
		if (tickCounter3 > config.tickCount3())
		{
			tickCounter3 = 0;
		}
	}

	//hotkey settings
	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (!config.metronomeEnabled())
		{
			return;
		}
		if (config.showHideHotkey().matches(e))
		{
			boolean hidden = !config.metronomeHidden();
			configManager.setConfiguration(PartybaseConfig.GROUP, "metronomeHidden", hidden);
		}

		if (config.increaseTickCountHotkey().matches(e))
		{
			configManager.setConfiguration(PartybaseConfig.GROUP, "tickCount", config.tickCount() + 1);
		}

		if (config.decreaseTickCountHotkey().matches(e))
		{
			configManager.setConfiguration(PartybaseConfig.GROUP, "tickCount", Math.max(1, config.tickCount() - 1));
		}

		if (config.tickResetHotkey().matches(e))
		{
			int resetValue;

			// Reset Cycle 1
			if (config.tickCount() > 1)
			{
				// Prevent out of bounds by setting to 0 if reset start is above tick count
				resetValue = (config.tickResetStartTick() >= config.tickCount()) ? 0 : config.tickResetStartTick();
				// If resetting to 0, set color index to 0 as well so that the color is set to the first color next
				// onGameTick
				currentColorIndex = resetValue == 0 ? 0 : 1;
			}
			else
			{
				resetValue = (config.tickResetStartTick() >= config.colorCycle()) ? 0 : config.tickResetStartTick();
				currentColorIndex = resetValue;
			}
			tickCounter = resetValue;
			setCurrentColorByColorIndex(currentColorIndex);

			tickCounter2 = (config.tickResetStartTick() >= config.tickCount2()) ? 0 : config.tickResetStartTick();
			tickCounter3 = (config.tickResetStartTick() >= config.tickCount3()) ? 0 : config.tickResetStartTick();
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	private void updateOverlaysShown()
	{
		setOverlaysShown(config.metronomeEnabled() && !config.metronomeHidden());
	}

	private void setOverlaysShown(boolean show)
	{
		if (show == shown)
		{
			return;
		}
		shown = show;
		for (Overlay overlay : overlays)
		{
			if (show)
			{
				overlayManager.add(overlay);
			}
			else
			{
				overlayManager.remove(overlay);
			}
		}
	}

	private void setCurrentColorByColorIndex(int currentColorIndex)
	{
		switch (currentColorIndex)
		{
			case 1:
				currentColor = config.getTickColor();
				break;
			case 2:
				currentColor = config.getTockColor();
				break;
			case 3:
				currentColor = config.getTick3Color();
				break;
			case 4:
				currentColor = config.getTick4Color();
				break;
			case 5:
				currentColor = config.getTick5Color();
				break;
			case 6:
				currentColor = config.getTick6Color();
				break;
			case 7:
				currentColor = config.getTick7Color();
				break;
			case 8:
				currentColor = config.getTick8Color();
				break;
			case 9:
				currentColor = config.getTick9Color();
				break;
			case 10:
				currentColor = config.getTick10Color();
				break;
			default:
				break;
		}
	}
}
