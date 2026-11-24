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

import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;

/** The plugin code uses none of the APIs the Plugin Hub review rejects. */
public class ForbiddenApiTest
{
	private static final List<String> FORBIDDEN = Arrays.asList(
			"import java.io.File;", "java.nio.file.Files", "java.nio.file.Paths", "RUNELITE_DIR",
			"Filepath.Unchecked", "new Gson(", "new OkHttpClient", "Thread.sleep", "java.lang.reflect",
			"Class.forName", "setAccessible(", "getDeclared", " native ", "System.exit", "Runtime.getRuntime",
			"ProcessBuilder", "java.awt.Desktop", "LinkBrowser", "menuAction(");

	/**
	 * The defence tracker's custom font picker reads a font file the user chose (as Better Party Defence does on the
	 * Plugin Hub); nothing else may touch files.
	 */
	private static boolean allowed(Path file, String api)
	{
		String name = file.getFileName().toString();
		return api.equals("import java.io.File;")
			&& (name.equals("CustomFontChooser.java") || name.equals("DefenceOverlayRenderer.java"));
	}

	@Test
	public void noForbiddenApis() throws IOException
	{
		List<String> found = new ArrayList<>();
		try (Stream<Path> files = Files.walk(Paths.get("src/main/java")))
		{
			for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator)
			{
				String code = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
				for (String api : FORBIDDEN)
				{
					if (code.contains(api) && !allowed(file, api))
					{
						found.add(file.getFileName() + ": " + api);
					}
				}
			}
		}
		assertTrue("not allowed on the Plugin Hub: " + found, found.isEmpty());
	}
}
