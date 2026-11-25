# Partybase

An all-in-one RuneLite party plugin. It combines three Plugin Hub plugins into one sidebar and one set of settings:

- [Party Panel](https://github.com/TheStonedTurtle/party-panel) by TheStonedTurtle: party member info.
- [GoldenDPS](https://github.com/tjeerocka/goldendps) by tjeerocka: damage, DPS and healing meter.
- [Better Party Defence](https://github.com/Falsary/better-party-defence) by Heisty: boss defence tracking.
- [Visual Metronome](https://github.com/vincent0955/Visual-metronome) by vincent0955: a tick metronome, here synced to the party leader.

It also shares tile packs from [Tile Packs](https://github.com/TrevorMDev/tile-packs) by TrevorMDev.

On top of those, Partybase adds a party timer, a party leader, and leader-marked targets.

Everyone in the party needs Partybase. It uses its own party message types, so it does not exchange data with people running the original plugins.

## Sidebar

The top of the sidebar shows **Party active 1:02:03** and the current leader. The timer counts from when the longest-standing current member joined the party. Below that are three tabs: Party info, DPS and Tiles.

### Party info

Each party member's banner shows HP, prayer, run energy, special attack, world and spellbook. Click a banner to expand it into inventory, equipment, skills and prayers. Your own banner is listed first and stays collapsed.

The party controls are at the top of this tab: start or leave a party, join one by passphrase, rejoin your previous party, and copy the passphrase.

### DPS

- **Meter:** the same bar meter as the game-screen overlay, at sidebar width. It shows the overall meter and the boss meter.
- **Marked targets:** the list of marked monsters with their icons (see below).
- **Fight controls:** a fight picker (live fight, session total or a saved fight), Boss only, End fight, New session, Clear, and Report buttons.
- **Fight details:** kills per hour, boss times in raids, defence drains, per-player details (accuracy, biggest hit, healing by source, overhealing, damage taken, breakdown per weapon, spec and spell) and damage per target.
- **Monster stats:** every monster, raid and Barrows run you killed, with personal bests and stats over all kills.

## DPS meter

- **Each client counts its own hits.** The game only marks your own hitsplats. Each client counts its own damage and healing and sends them to the party once per game tick. Players without Partybase are estimated from their attack animations and marked with `~`.
- **Overlay:** on the game screen, right-click the meter to switch between damage, DPS, healing and damage taken. The same menu toggles boss only, switches between fight and session, reports to party chat, and resets the session.
- **Overall meter and boss meter:** the boss meter shows only damage on the boss, over the boss time. While the boss lives it shows your personal best pace. Turn off **Overall meter** to show only the boss meter, on screen and in the sidebar.
- **Fight timing:** fights use active time, so the clock pauses after 10 seconds without hits. A fight ends when the boss dies or after 5 minutes without hits. Raids and Barrows runs are timed as a whole, and every boss inside them also gets its own fight.
- **Personal bests:** fastest time and best DPS per boss, raid and Barrows run, kept separately for solo and party kills.
- **Hit chance:** your chance to hit the monster you attack is shown above it. It uses your levels, boosts, prayers, gear, attack style and the monster's defence after specs.
- **Reports:** Report buttons send the top five to party chat. After a boss kill, the leader sends the boss report automatically (setting: Auto boss report).
- **Fun extras:** a crown over the top damage dealer, and a burger or fries over the top healer.
- **Party sync:** members only count while you are in the same instance or fighting the same boss (setting: Others only when together).

### Tiles

- **Party tiles:** the tiles the leader marked (see Party tiles). Untick to hide them, **Save** adds them to Tile Packs as one custom pack, and the leader can **Clear** them for everyone.
- **Shared with the party:** packs that party members shared. Tick a pack to draw it, **Save** adds it to Tile Packs as a custom pack, and **x** removes it.
- **Your Tile Packs:** every pack in your Tile Packs plugin, the ones you have turned on first, with a search box. **Share** sends one pack; **Share all turned on** sends every pack you have turned on.

## Tile pack sharing

- **Where packs come from:** Partybase reads the Tile Packs plugin's own data. Built-in packs come from the installed plugin, so they match your Tile Packs version. Edited and custom packs, and which packs are turned on, come from its settings. Without Tile Packs installed, only packs saved in its settings are listed.
- **Who can share:** anyone in the party.
- **What shows:** Partybase draws shared packs itself, so receivers don't need Tile Packs. Packs from the leader show straight away; packs from other members wait until you tick them. Your own shared packs are listed but start hidden, because Tile Packs already draws them.
- **How long they last:** shared packs stay until you leave or switch parties. **Save** keeps a pack for good by adding it to Tile Packs as a custom pack. Tile Packs reads its settings when it starts, so the pack appears there after Tile Packs or the client restarts.
- **Sending:** big packs are split into 12,000-character parts and sent two parts per game tick, so sharing many packs at once doesn't flood the party server. Sharing a pack again replaces the earlier copy.

## Party tiles

The leader can mark and label tiles for the whole party, like RuneLite's Ground Markers.

- **Marking:** shift right-click a tile and choose **Mark party tile**. Clicking it marks the tile yellow; its submenu picks one of eight colours.
- **Changing:** shift right-click a marked tile for **Unmark party tile**, **Label party tile** (type the label in the chatbox, up to 40 characters) and **Party tile colour**.
- **Who sees them:** every party member running Partybase, drawn with the shared packs. Members who join later get every party tile from the leader. Only the leader's changes are used; solo players can mark tiles for themselves.
- **Instances:** tiles are stored in instance template coordinates, as Ground Markers does, so they line up for everyone in the same raid or boss instance.
- **How long they last:** until you leave or switch parties. When the leader changes, the tiles stay and the new leader can change them. **Save** in the Tiles tab keeps them as a Tile Packs pack.
- **Limits:** up to 500 party tiles. They're sent 150 per message.

## Metronome

The Visual Metronome plugin is built in: a box, tile, number or mouse-following cue every game tick, with up to three cycles, colours and hotkeys. Turn it on with **Metronome > Show metronome**. If you use it here, turn off the standalone Visual Metronome plugin.

- **Following the leader:** with **Sync with party leader** on (the default), your metronome follows the leader's tick position, tick counts and number of colours. This replaces Visual Metronome's typed-in sync target.
- **The leader's side:** the leader's client sends its metronome every game tick while it shows one and the party has someone else in it. The leader's own metronome is never changed.
- **Servers:** sync goes through Partybase's party connection, so it works on RuneLite's party server and on a custom server.

## Party leader and marked targets

- **Leader:** the member who joined the party first leads. If the leader leaves, the earliest remaining member takes over.
- **Handing over:** the leader can right-click another member's banner in Party info and choose **Make party leader**. This only works for members who run Partybase.
- **Marking:** the leader shift right-clicks a monster and chooses **Mark target**. Clicking it uses the next free icon. Its submenu picks one of eight: Star, Circle, Diamond, Triangle, Moon, Square, Cross or Skull. You can mark several monsters at once. Solo players can mark targets too.
- **What a mark does:** each marked monster gets an outline in its icon colour and the icon above its head. Every member's Partybase treats it as a boss, so the boss meter follows it. When all marked monsters are dead, the fight ends and the leader sends the boss report.
- **Changing marks:** shift right-click a marked monster to change its icon or unmark it. The DPS tab has a remove button per mark and a Clear all button.
- **When marks go away:** a mark is removed when its monster dies or despawns. All marks are cleared when you leave or switch parties. Members who join later receive the current marks from the leader.

## Defence tracker

Partybase estimates the current Defence of supported bosses and NPCs.

- **Specs tracked:** Dragon warhammer, Elder maul, Bandos godsword, Arclight, Darklight, Emberlight, Barrelchest anchor, Bone dagger, Dorgeshuun crossbow, Accursed sceptre, Tonalztics of Ralos, Seercull and Eye of Ayak.
- **Other effects:** Vulnerability (including the Tome of Water bonus), and Shadow Barrage with the Shadow ancient sceptre.
- **Where it shows:**
  - a readout on the NPC (you choose position, format, font and colours)
  - a Defence info box, with spec history on hover
  - a Magic defence readout and info box
  - info boxes for weapon specs, Vulnerability, Shadow Barrage and the Red Keris timer in Tombs of Amascut
- **Sharing:** specs are shared with party members who run Partybase. Specs from members who use RuneLite's built-in Special Attack Counter are read too.
- **Encounter state:** the current state of each encounter is shared, which helps when someone joins late or dies. Separate raid instances are kept apart, and multi-target encounters keep a value per target (for example Olm's claws).
- **Supported bosses:** Chambers of Xeric, Theatre of Blood, Tombs of Amascut, The Fractured Archive, and many other bosses.

## Custom party server

By default Partybase uses RuneLite's party server. You can run your own instead: The separate `partybase-server` repo is a small AWS relay you deploy with CDK. See its README for deploying it and creating keys.

- **Turning it on:** in Partybase settings, open **Party server**. Put the server's `wss://` address in **Server URL**, and your personal key in **Server key**. Leave the key empty if the server needs none.
- **What changes:** all Partybase party traffic goes to that server, including member stats, inventory, gear and prayers. Partybase sends those itself, because RuneLite's party data only travels over RuneLite's server.
- **Joining:** you still join parties by passphrase, with the same Party info controls. Everyone in the party must use the same server.
- **Switching:** changing the URL or key moves you, with your current party passphrase, to the new server straight away.
- **Reconnects:** if the connection drops, Partybase reconnects with growing waits (2 seconds up to a minute). It keeps your member id, so the leader and party timer survive. A refused key is not retried. Partybase says so in the chatbox instead.
- **Security:** only `wss://` addresses are accepted, except `ws://localhost` for testing. The key is stored as a secret setting and sent only to that server.
- **Limits:** the avatar picture in Party info only exists on RuneLite's party. RuneLite's own plugins (such as the Party sidebar and Special Attack Counter) keep using RuneLite's party and don't see relay traffic. Partybase still shares your specs with the relay party itself.

## Settings

All settings are in the Partybase config panel, in these sections:

| Section | What it covers |
| --- | --- |
| Party server | Server URL and key for a custom relay server; empty uses RuneLite's party server |
| Party info | Sidebar icon, party controls, passphrase, expanding members, virtual levels, worlds, the not-in-a-party reminder |
| DPS: party | Sharing, counting others, raid party and own party, auto boss report, leave on logout, chat colour |
| DPS: overlay | Show overlay, compact mode, max bars, width, hide when empty, your bar colour, overall meter, boss meter, simple defence label, hit chance |
| DPS: fun | Crown on the top damage dealer, snack on the top healer, Max hit column |
| Metronome, Metronome: ... | Show metronome, sync with party leader, and Visual Metronome's display, colour, cycle and hotkey settings |
| Defence: general to Defence: effect info boxes | Defence tracker display, info boxes, text and icons, colours, magic defence, effect info boxes |

## How party data travels

All party data goes through a party server: RuneLite's, or your own relay server when one is set (see Custom party server). Clients never connect to each other directly. Each message reaches everyone in the party with the same passphrase.

| Message | What it carries |
| --- | --- |
| RuneLite's party data (`PartyData`) | Name, HP, prayer, energy, spec, skills, inventory, equipment, prayers, rune pouch, quiver. Sent by RuneLite on its party, and by Partybase on a relay server |
| `PartybaseBatchedChange` | Stamina, poison, disease, world, spellbook, available and unlocked prayers |
| `PartybaseDpsUpdate` | Your hits, healing and damage taken, once per game tick while fighting |
| `PartybaseDpsPartyMessage` | Join times, leader changes, kicks, and marks |
| `PartybaseDefence*` | Defence specs, encounter state, Red Keris timer |
| `PartybaseTilePack` | A shared tile pack, in parts |
| `PartybasePartyTiles` | The leader's party tiles: set, put, remove or clear |
| `PartybaseMetronomeSync` | The leader's metronome, every game tick while it shows one |
| `SpecialCounterUpdate` | Defence-draining specs, in RuneLite's Special Attack Counter format |
| `PartyChatMessage` | Fight and boss reports, shown as party chat |
| `UserSync` | Sent after joining, asking everyone to resend their full state |

On a relay server, each message is wrapped as `{"t":"<type>","d":{...}}`. The server forwards it unchanged.

RuneLite parties have no leader role. Each client works out the leader from Partybase's messages, and only accepts marks and kicks from that leader. Anyone with the passphrase can join, so share it only with your party.

## Data

Fight history, personal bests and monster stats are stored per account in `.runelite/plugin-data/partybase/`.

## Development

Requires JDK 11. On this machine that is `C:\Program Files\Eclipse Adoptium\jdk-11.0.19.7-hotspot`.

```
gradlew run     # RuneLite in developer mode with Partybase loaded
gradlew build   # compile, checkstyle and tests
```

`PartyLinkRelayLiveTest` runs two real party clients against a relay server. It is skipped unless `PARTYBASE_SERVER_URL` (and `PARTYBASE_SERVER_KEY` for a server with keys) are set.

- **Logging in:** use the Jagex account button in the client sidebar.
- **VS Code:** `.vscode/settings.json` points the Java extension at JDK 11, because Gradle 8.10 can't run on the bundled Java 25. Start the client from the Gradle sidebar: partybase > Tasks > other > run.
- **Git:** use Windows git (or IntelliJ/VS Code) in this folder. WSL git can't set file permissions on the C: drive.

### Code layout

- `com.matthewloller.partybase`: the plugin, its config and the sidebar panel. The `data` and `ui` packages hold the Party info code from Party Panel.
- `party`: `PartyLink`, the one party API every feature uses. It is backed by RuneLite's party or a relay server, and posts Partybase party events (`party.events`) in both cases. No other package uses RuneLite's `PartyService`, `WSClient` or party events.
- `dps`: the DPS meter, party leader and marked targets (`DpsModule`), from GoldenDPS.
- `defence`: the defence tracker (`DefenceModule`), from Better Party Defence.
- `metronome`: the tick metronome (`MetronomeModule`), from Visual Metronome.
- `tiles`: tile pack sharing and party tiles (`TileModule`, `PartyTiles`); `TilePacksSource` reads the Tile Packs plugin's data, and `SharedTilesOverlay` is ported from it.

Each feature is a `PartybaseModule`: a Guice singleton that `PartybasePlugin` starts and registers on the event bus, then stops in reverse order. `PartyLink` starts before the modules and stops after them. All settings live in `PartybaseConfig`. Party messages are named `Partybase*`.

### Code conventions

- Allman braces, tab indents, one extra tab for continuation lines. Checkstyle (`config/checkstyle/checkstyle.xml`) runs on every build.
- No unused imports. Static imports come first, then the rest in alphabetical order.
- LF line endings (see `.gitattributes`).
- Every source file starts with the BSD 2-Clause header naming its original author.
- Run `tools/format.sh` (needs `astyle`) before committing to apply the formatting and import rules. Running it twice changes nothing.
- Update this README with every feature change.

## Plugin Hub

Because Partybase can connect to a server RuneLite doesn't run, its Plugin Hub entry needs a `warning=` line, for example: "This plugin can send your party data (character name, stats, inventory, equipment and combat information) to a 3rd-party party server not controlled or verified by RuneLite developers, if you set one in its settings." The custom server is off unless a Server URL is set.

## Credits and licences

Partybase is BSD 2-Clause. It includes code from Party Panel (TheStonedTurtle), GoldenDPS (tjeerocka), Better Party Defence (Heisty), Visual Metronome (vincent0955) and Tile Packs (TrevorMDev), all under BSD 2-Clause. See `LICENSE`.

Better Party Defence includes code derived from OSParty; see `LICENSE-OSPARTY.txt` and `ATTRIBUTION-better-party-defence.md`. GoldenDPS's boss list comes from RuneLite's DPS Counter plugin. Its monster stats (`npc_stats.tsv`) come from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki) (CC BY-NC-SA 3.0).
