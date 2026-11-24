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
import com.matthewloller.partybase.PartybaseConfig.TrackerFont;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.config.ConfigManager;

// Selects and stores a local font without blocking the game thread.
final class CustomFontChooser
{
	private final PartybaseConfig config;
	private final ConfigManager configManager;

	CustomFontChooser(PartybaseConfig config, ConfigManager configManager)
	{
		this.config = config;
		this.configManager = configManager;
	}

	void choose()
	{
		SwingUtilities.invokeLater(() ->
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Choose a font for the Partybase defence tracker");
			chooser.setFileFilter(new FileNameExtensionFilter("Font files (*.ttf, *.otf)", "ttf", "otf"));

			String current = config.customFontPath();
			if (current != null && !current.trim().isEmpty())
			{
				File file = new File(current);
				if (file.getParentFile() != null)
				{
					chooser.setCurrentDirectory(file.getParentFile());
				}
			}

			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				setConfiguration(PartybaseConfig.KEY_CUSTOM_FONT_PATH,
					chooser.getSelectedFile().getAbsolutePath());
				setConfiguration(PartybaseConfig.KEY_DEFENCE_FONT, TrackerFont.CUSTOM);
			}
			else
			{
				TrackerFont fallback = current == null || current.trim().isEmpty()
					? TrackerFont.RUNESCAPE
					: TrackerFont.CUSTOM;
				setConfiguration(PartybaseConfig.KEY_DEFENCE_FONT, fallback);
			}
		});
	}

	private void setConfiguration(String key, Object value)
	{
		configManager.setConfiguration(PartybaseConfig.GROUP, key, value);
	}
}
