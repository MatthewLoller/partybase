# Partybase

An all-in-one RuneLite party plugin. The sidebar shows how long your party has been active and who leads it, with two tabs:

- **Party info**: your party members' stats, inventory, equipment and prayers (from [Party Panel](https://github.com/TheStonedTurtle/party-panel) by TheStonedTurtle).
- **DPS**: a live damage meter plus fight details, personal bests and monster stats (from [GoldenDPS](https://github.com/tjeerocka/goldendps) by tjeerocka). The same meter also shows as an overlay on the game screen.

## Party leader and marked targets

- The member who joined the party first leads. If the leader leaves, the next earliest member takes over.
- The leader can right-click another member in Party info and choose **Make party leader**.
- The leader shift right-clicks a monster and chooses **Mark target**. Everyone's Partybase then tracks it as a boss fight: the boss meter follows it, it gets a gold outline, and when it dies the fight ends and the leader sends the boss report to party chat (setting: Auto boss report). Solo players can mark targets too.

Partybase uses its own party message types, so it does not exchange data with Party Panel or GoldenDPS users. Everyone in the party needs Partybase installed.

## Code layout

- `com.matthewloller.partybase`: the plugin, its config and the sidebar panel, plus the Party info code (`data`, `ui`) from Party Panel.
- `dps`: the DPS meter, party leader and marked targets (`DpsModule`), from GoldenDPS.
- `defence`: the defence tracker (`DefenceModule`), from Better Party Defence.

Each feature is a `PartybaseModule`: a Guice singleton that `PartybasePlugin` starts, registers on the event bus, and stops in reverse order. All settings live in `PartybaseConfig`, grouped into Party info, DPS and Defence sections. Party messages are named `Partybase*` so they never mix with the original plugins.

## Code conventions

- Allman braces, tab indents, one extra tab for continuation lines. Checkstyle (`config/checkstyle/checkstyle.xml`) runs on every build.
- Imports: no unused ones; static imports first, then the rest in alphabetical order.
- LF line endings (see `.gitattributes`).
- Every source file starts with the BSD 2-Clause header naming its original author.

Run `tools/format.sh` (needs `astyle`) before committing to apply the formatting and import rules.

## Development

Requires JDK 11.

```
gradlew run    # RuneLite in developer mode with Partybase loaded
gradlew test
```

Log in through the Jagex account panel in the client sidebar. Fight data is stored in `.runelite/plugin-data/partybase/`.
