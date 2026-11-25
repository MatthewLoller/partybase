/*
 * Copyright (c) 2020, vincent0955 <https://github.com/vincent0955>
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
package com.matthewloller.partybase.metronome.fullresizableoverlay;

import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.PartybasePlugin;
import com.matthewloller.partybase.metronome.MetronomeModule;
import java.awt.Color;
import javax.inject.Inject;

public class Cycle3MetronomeOverlay extends FullResizableMetronomeOverlay
{
	@Inject
	public Cycle3MetronomeOverlay(PartybaseConfig config, MetronomeModule plugin, PartybasePlugin owner)
	{
		super(config, plugin, owner);
	}

	@Override
	protected boolean isVisible()
	{
		return config.enableCycle3() && config.showCycle3Overlay();
	}

	@Override
	protected String getTickText()
	{
		return String.valueOf(plugin.getTickCounter3());
	}

	@Override
	protected Color getTextColor()
	{
		return config.cycle3Color();
	}

	@Override
	protected boolean showBackgroundColor()
	{
		return false;
	}
}
