/*
 * Copyright (c) 2026, tjeerocka <https://github.com/tjeerocka>
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
package com.matthewloller.partybase.dps.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a short report of a fight, one chat line per player:
 * <pre>
 * Boss damage - Vorkath 1:23 - total 27.4k (330.1/s)
 * 1. Kurt Yoghurt 12.3k 148.2/s 45%
 * 2. Zezima 9.1k 109.6/s 33%
 * </pre>
 * Every line fits in one RuneLite party chat message.
 */
public final class ReportFormatter
{
	/** RuneLite party chat cuts messages at 150 characters; stay below that. */
	public static final int MAX_LINE = 140;
	public static final int DEFAULT_TOP = 5;

	private ReportFormatter()
	{
	}

	/**
	 * @param report the fight to report
	 * @param mode   what to report (damage, DPS, healing, damage taken)
	 * @param title  what the fight is called ("Vorkath", "Session")
	 * @param top    how many players
	 * @return the lines to send, header first; only the header when there is nothing to report
	 */
	public static List<String> lines(FightReport report, MeterMode mode, String title, int top)
	{
		return lines(report, mode, mode.getLabel(), title, top);
	}

	/** @param label what the header calls the report ("Boss damage", "Healing") */
	public static List<String> lines(FightReport report, MeterMode mode, String label, String title, int top)
	{
		return lines(report, mode, label, title, top, false);
	}

	/**
	 * @param withKills the header gives the kills instead of the DPS: over many monsters
	 *                  ("Overall damage") the kills say more than a DPS with walking in it
	 */
	public static List<String> lines(FightReport report, MeterMode mode, String label, String title, int top,
		boolean withKills)
	{
		List<String> out = new ArrayList<>();
		List<FightReport.PlayerRow> rows = report.sorted(mode);
		double total = report.total(mode);

		StringBuilder header = new StringBuilder(label);
		if (title != null && !title.isEmpty())
		{
			header.append(" - ").append(title);
		}
		header.append(' ').append(Format.preciseDuration(report.getDurationMs()));
		if (!rows.isEmpty())
		{
			header.append(" - total ").append(withKills ? killsText(report, mode, total) : totalText(report, mode, total));
		}
		out.add(cut(header.toString()));
		if (report.isSolo())
		{
			// the header already has your total and DPS
			return out;
		}

		int n = Math.min(Math.max(1, top), rows.size());
		for (int i = 0; i < n; i++)
		{
			FightReport.PlayerRow row = rows.get(i);
			String name = (row.isEstimate() ? "~" : "") + row.name;
			out.add(cut((i + 1) + ". " + name + " " + valueText(row, mode, total)));
		}
		return out;
	}

	private static String killsText(FightReport report, MeterMode mode, double total)
	{
		int kills = report.getKills();
		String amount = Format.amount(mode == MeterMode.DPS ? report.getTotalDamage() : total);
		return kills == 0 ? amount : amount + " - " + kills + (kills == 1 ? " kill" : " kills");
	}

	private static String totalText(FightReport report, MeterMode mode, double total)
	{
		switch (mode)
		{
			case DAMAGE:
			case DPS:
				return Format.amount(report.getTotalDamage()) + " (" + Format.dps(report.getGroupDps()) + "/s)";
			case HEALING:
				double oh = report.groupOverhealPercent();
				return Format.amount(total) + (oh > 0 ? " OH " + Format.percent(oh) : "");
			default:
				return Format.amount(total);
		}
	}

	private static String valueText(FightReport.PlayerRow row, MeterMode mode, double total)
	{
		double v = FightReport.value(row, mode);
		String share = Format.percent(total <= 0 ? 0 : 100.0 * v / total);
		switch (mode)
		{
			case DAMAGE:
				return Format.amount(row.damage) + " " + Format.dps(row.dps) + "/s " + Format.percent(row.percent);
			case DPS:
				return Format.dps(row.dps) + "/s " + Format.amount(row.damage) + " " + Format.percent(row.percent);
			case HEALING:
				return Format.amount(v) + (row.overheal > 0 ? " OH " + Format.percent(row.overhealPercent()) : "") + " " + share;
			default:
				return Format.amount(v) + " " + share;
		}
	}

	private static String cut(String s)
	{
		return s.length() <= MAX_LINE ? s : s.substring(0, MAX_LINE);
	}
}
