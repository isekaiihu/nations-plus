# NationsPlus

A professional-grade Towns & Nations plugin for Paper servers, with chunk-based land claiming and protection, Dynmap territory visualization, nation warfare, and a full player marriage system.

**Author:** ISekai

## Features
- **Towns** - create, rename, invite/kick members, assign roles, claim/unclaim chunks, town bank (deposit/withdraw), set spawn with a teleport countdown, configurable town laws (PvP, block break/place, interact, explosions, mob spawning), and a custom map color per town
- **Nations** - create, rename, invite/kick towns, nation bank, spawn teleport, neutrality toggle
- **Land protection** - blocks break/place, interaction, PvP, mob spawning, and explosions are governed by each town's laws and claim ownership
- **Dynmap integration** - live-rendered town and nation borders, custom fill/border colors per town, war-zone highlighting, and spawn icons
- **Vault economy integration** - configurable costs for town/nation creation, renaming, claiming, war declarations, and divorce
- **Nation wars** - declare war, grace period before hostilities, surrender, war status, and configurable post-war tribute/occupation settings
- **Marriage system** - propose, accept/deny, divorce, gender selection, and spouse actions (hug, kiss, reproduce for a claim-block bonus)
- **PlaceholderAPI expansion** - placeholders for town/nation stats, roles, marriage status, and active wars (for use in TAB, scoreboards, etc.)
- **Admin tools** - force-delete towns/nations, grant/remove bonus claim blocks, and reload configuration
- **Towny migration** - one-time `/migrate towny` command that imports towns, nations, residents, claims, and bank balances from an existing Towny installation

## Commands
| Command | Description |
|---|---|
| `/town <subcommand>` | Town management (create, delete, gui, invite, kick, claim, spawn, color, rename, laws, ...) |
| `/nation <subcommand>` | Nation management (create, delete, gui, invite, kick, spawn, rename, ...) |
| `/war <declare\|surrender\|status> [nation]` | Declare or manage wars |
| `/marry <player>` | Propose marriage to a player |
| `/divorce [player]` | Divorce your spouse |
| `/marriage <accept\|deny\|info>` | Marriage proposal management |
| `/gender <male\|female\|other\|prefer-not>` | Set your gender |
| `/hug` / `/kiss` / `/reproduce` | Spouse actions |
| `/earthadmin` | Open the admin management GUI |
| `/tnc reload` / `/tnc forcedelete <town\|nation> <name>` | Admin commands |
| `/claim <give\|remove> <player> <amount>` | Manage a player's bonus claim blocks |
| `/migrate <plugin>` | Migrate data from another plugin (currently Towny) |

## Permissions
| Permission | Default | Description |
|---|---|---|
| `nationsplus.admin` | op | Full admin access |
| `nationsplus.admin.claims` | op | Manage claim blocks |
| `nationsplus.town.*` | true | Town create/delete/gui/invite/kick/claim/spawn/setspawn/bank/settings |
| `nationsplus.nation.*` | true | Nation create/delete/gui/invite/kick/spawn/setspawn |
| `nationsplus.war.declare` / `nationsplus.war.surrender` | true | War actions |
| `nationsplus.marry` / `nationsplus.gender` | true | Marriage/gender commands |
| `nationsplus.spouse.hug` / `.kiss` / `.reproduce` | true | Spouse actions |

See [`plugin.yml`](src/main/resources/plugin.yml) for the complete permission list.

## Dependencies
- [Paper API](https://papermc.io/) 1.21.4-R0.1-SNAPSHOT (provided)
- Adventure text serializers (legacy, plain)
- [Vault](https://www.spigotmc.org/resources/vault.34315/) (soft dependency - economy features)
- [Dynmap](https://www.spigotmc.org/resources/dynmap%C2%AE.274/) (soft dependency - map integration)
- [Towny](https://www.spigotmc.org/resources/towny-advanced.72694/) (soft dependency - migration only)
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (soft dependency - placeholders)

## Installation
1. Download the jar from the Releases page of this repository.
2. Drop it into your server's `plugins/` folder.
3. Restart or reload the server.

## Building from source
```bash
mvn clean package
```
Compiled jar lands in `target/`.

## License
See [LICENSE](LICENSE). All rights reserved — see terms above.
