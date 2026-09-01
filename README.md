# ArenaBrawl - Project Architecture

Paper plugin (version 26.2) that implements a 2v2 combat minigame featuring abilities, persistent progression, and multiple match systems. This document explains how the code is organized and why, making it easy to maintain and extend.

---

## Table of Contents
- [General Design Principles](#general-design-principles)
- [Package Structure](#package-structure)
- [Match Lifecycle](#match-lifecycle)
- [Ability System](#ability-system)
    - [Components](#components)
    - [Why Category (Slot) and Cost Are Decoupled](#why-category-slot-and-cost-are-decoupled)
    - [AbilityTargeting](#abilitytargeting)
    - ["Breath" Abilities (Directional Cone)](#breath-abilities-directional-cone)
- [Combat System](#combat-system)
- [Debuff System](#debuff-system)
- [Summonable Structures](#summonable-structures)
- [Progression and Economy Systems](#progression-and-economy-systems)
- [Lobby and Matchmaking](#lobby-and-matchmaking)
- [Multi-Map System](#multi-map-system)
- [Entity Cleanup and Management](#entity-cleanup-and-management)
- [Intentionally Reused Patterns](#intentionally-reused-patterns)
- [How to Add New Features](#how-to-add-new-features)

---

## General Design Principles

* **One manager per responsibility.** Every system (energy, hunger, health, debuffs, shields, runes, hats...) has its own `XManager` class, which serves as the single source of truth for that data. State is never duplicated across multiple places.
* **Abilities are "stateless regarding matches" where possible.** An ability receives its dependencies (managers) via constructor and delegates to them actual state resides within the managers, not within the ability instance, barring certain exceptions.
* **Ability instances per match, not globally per player.** `AbilityRegistry.create(slot, id, deps)` builds a fresh instance of the ability every time a match starts, ensuring any internal state (excluding cooldowns, which are handled by `CooldownManager`) does not leak across different matches.
* **Domain-separated YAML persistence.** Each system with persistent data uses its own file (`stats.yml`, `ratings.yml`, `hats.yml`, `runes.yml`, `keys.yml`, `combat_upgrades.yml`, `abilities.yml`) instead of a single monolithic file.
* **All damage flows through `CombatService`.** It is the sole entry point aware of all mitigations (shields, damage reduction, buffs, and damage-related debuffs) no ability should subtract health directly without calling it (documented exception: *BroodMother's* poison, which intentionally bypasses shields and ignores damage buffs and/or debuffs).
* **Never rely on Bukkit's `ProjectileHitEvent` or physical collisions for PvP.** It proved unreliable with `Fireball` and launched living entities — the adopted solution is manual per-tick proximity tracking (`TrackedProjectileTask`, `TrackedLivingProjectileTask`).

---

## Package Structure

```text
org.latios.arenaBrawl
├── ArenaBrawlPlugin.java        # onEnable/onDisable, wiring for the entire plugin
├── abilities/                   # Ability system
│   ├── config/                  # Config files to edit ability values (damage,cooldown,duration..etc) without having to rebuild the plugin.
│   ├── cost/                    # Cost strategies (cooldown, energy, ultimate). All offensive abilities use energy, every other ability category has a time based cooldown, with every ultimate ability having a fixed 60 second cooldown and only being usable once.
│   ├── offensive/               # Offensive abilities.
│   ├── support/                 # Support abilities
│   ├── utility/                 # Utility abilities
│   ├── ultimate/                # Ultimate abilities
│   └── structures/               # Summonable structures (totems, trees, walls)
├── debuffs/                      # Negative effects system
├── general/                       # Cross-cutting services (combat, health, hunger, collision...)
├── game/                          # Match lifecycle, maps, arena
├── team/                          # Teams (RED/BLUE)
├── party/                         # Pre-match party system
├── queue/                         # Matchmaking queue
├── lobby/                         # Lobby state and UI
├── gui/                           # Selection menus (abilities, runes, hats)
├── powerups/                      # Health and Double Damage power-ups
├── runes/                         # Rune system (melee proc effects)
├── hats/                          # Cosmetics with chat phrases + Magic Chest
├── cosmetics/                     # Cosmetic armor based on rating tier
├── rating/                        # Elo system + leaderboard
├── stats/                         # Persistent kills/wins/losses/coins
└── upgrades/                      # Combat Upgrades (purchasable upgrades via coins)
```
## Match Lifecycle
    Lobby[Lobby / Queue] -->|ArenaManager.startMatch| Match[Match in Progress]
    Match -->|MatchManager.finishMatch| Cleanup[5s Delay & Teleport]
    Cleanup --> Lobby


`Match` is the core object representing an active match. It contains:

- The 4 players (`RED` / `BLUE` teams) and their `alive` state.
- The assigned `ArenaMap`.
- Its own `PowerupManager`, independent of other simultaneous matches.
- Individual per-player scoreboards, required for viewer-relative nametags.
- Start timestamp and the `doubleDamageActive` flag.

`MatchManager` does not handle a single match. It manages a list of all active simultaneous matches, one per map in use, indexed by player.

`ArenaMapManager` resolves the **4 physical maps, concurrent matches** requirement. Each map — *Bridge*, *Jungle*, *Highlink*, and *Zeus* — is a distinct Bukkit world loaded explicitly and registered in `config.yml`.

When starting a match, an available map is claimed through `claimAvailableMap()`. If all 4 maps are occupied, the queue waits without discarding players.
## Ability System

### Components

| Component | Responsibility |
|---|---|
| `Ability` | Interface defining `getName()`, `getCost()`, `activate(Player)`, plus optional default methods such as `getDescription()`, `getStats()`, and `onMatchStart(Player)`. |
| `AbilityCost` | Strategy interface that decouples what an ability does from how its usage is spent or restricted. |
| `CooldownCost` | Fixed-time cooldown, reduced by Combat Upgrades. |
| `EnergyCost` | Drains the player's energy. |
| `UltimateCost` | Allows the ability to be used once per match. |
| `AbilitySlot` | Categorizes abilities as `OFFENSIVE`, `UTILITY`, `SUPPORT`, or `ULTIMATE`. |
| `AbilityRegistry` | Central catalog and factory for abilities. |
| `AbilityDependencies` | Groups all managers required by abilities. |
| `AbilitySelectionManager` | Stores the ability IDs selected by each player per slot. |
| `AbilityManager` | Holds the concrete ability instances equipped by players during the current match. |

`AbilitySlot` is **category metadata, not behavior**. Any ability with any cost type can belong to any slot.

`AbilityRegistry` uses a string ID and a factory:

```text
AbilityDependencies -> Ability
```

Adding a new ability therefore never requires editing `AbilityManager`, `AbilitySelectionManager`, or the GUI.

`AbilityDependencies` is a record grouping all managers that abilities might need, including:

- Cooldowns
- Teams
- Energy
- Shields
- Debuffs
- Health
- Combat
- Upgrades
- And other future dependencies

When a new manager is introduced, it is added to `AbilityDependencies` once and becomes instantly accessible to all future abilities.

`AbilitySelectionManager` tracks which ability ID each player has selected per slot. Selections are persisted in `abilities.yml`, with defaults supplied if the player never opened the selector.

`AbilityManager` holds the concrete `Ability` instance equipped by each player during the current match. Instances are built through `AbilityRegistry.create()` at the start of every match using the saved selections.

### AbilityTargeting

`AbilityTargeting` is a shared utility for abilities requiring line-of-sight targeting on a player, such as:

- `ShadowStep`
- `Polymorph`
- `Consume`

It performs a raycast that:

- Passes through non-solid blocks such as slabs and stairs.
- Checks a synthetic bounding box around each candidate player.

This behavior is reimplemented rather than relying on Bukkit's native API.

## Combat System

`CombatService` is the **single entry point** for dealing ability damage to a player.

Resolution order inside `applyAbilityDamage`:

1. **Attacker damage multipliers**
    - `Double Damage` powerup
    - Global 5-minute double damage
    - Abilities such as nano boost

2. **Orbit Shield**
    - If the victim has an active Orbit Shield the hit is completely blocked.
    - A charge is consumed.
    - The shield effect is triggered instead of applying damage.
    - Effects may include healing and/or a guaranteed random debuff.

3. **Damage reduction**
    - Percentage reduction from certain abilities uses `ShieldManager`.

4. **Health subtraction**
    - Actual health is reduced through `PlayerHealthManager`.
    - Visual and audio feedback is triggered.
    - A damage-number hologram is positioned at the exact impact point:
        - Melee: raycast against the collision box.
        - Ranged: projectile location.

5. **Polymorph progress**
    - Damage contributes towards breaking `Polymorph`.

6. **Combat feedback**
    - Chat messages use colored arrow prefixes:
        - 🟢 Green = beneficial to you
        - 🔴 Red = detrimental to you

### Minion Damage

`applyMinionDamage` is a variant for damage inflicted by player-controlled entities, such as:

- `BroodMother` spiderlings

It applies the same multipliers and reductions as normal ability damage, but **breaks an Orbit Shield without being blocked**.

These damage sources therefore bypass the shield's protection.

### Projectile Impact Detection

Neither `Fireball` nor launched living entities such as `Chicken` reliably trigger physical collisions against players in PvP.

The solution is:

- `TrackedProjectileTask`
- `TrackedLivingProjectileTask`

These are `BukkitRunnable` implementations that calculate the actual distance between the projectile and each enemy player every tick.

Hits are therefore resolved by **proximity**, rather than relying on Bukkit collision events.

### Player Health

`PlayerHealthManager` decouples **real health** from Minecraft's vanilla heart system.

Players can have more than **2000 real health** with Combat Upgrades, while vanilla hearts remain fixed at 20.

The system therefore:

- Stores real health as an internal per-player value.
- Scales the visual heart display as a percentage.

---

## Debuff System

> [!IMPORTANT]
>
> ### Core Rule
>
> **Only one active debuff per player is allowed at a time.**
>
> `DebuffManager.tryApply()` silently rejects any attempt to apply another debuff while one is already active.

### Debuff Types

`DebuffType` is an enum containing:

- `IMMOBILIZE`
- `STUN`
- `POLYMORPH`
- `SLOW`
- `POISON`
- `SILENCE`
- `ANTIHEALING`

Each type carries an `isImmobilizing()` flag.

Other systems query this flag generically. For example, `ImmobilizeListener` blocks movement for any debuff marked as immobilizing, eliminating the need for individual listeners per debuff type.

### DebuffListener

`DebuffListener` follows the **Observer pattern** and exposes:

```java
onApplied();
onExpired();
```

Each mechanical effect registers independently, including:

- Blindness from `Stun`
- Disguise from `Polymorph` through LibsDisguises
- Slowness from `Slow`

Adding a new debuff therefore does not require modifications to `DebuffManager`.

### Defensive Cleanup

`tryApply()` forces a `clear()` on any previous entry that has expired by time but whose periodic cleanup task (`DebuffTickTask`) has not executed yet.

This prevents orphaned UI elements such as boss bars.

### Immobilization and Gravity

`ImmobilizeListener` intercepts `PlayerMoveEvent`.

While immobilized:

- Positive Y motion is cancelled, preventing jumping.
- Negative Y motion is allowed, preserving falling.
- X/Z lateral movement is locked.

This allows an immobilized player in mid-air to fall normally without being able to move laterally or jump.

### Poison

`Poison` is the only debuff that deals periodic damage.

The vanilla `POISON` potion effect is used **only for visual/HUD feedback**.

Actual damage is manually applied every second through:

```java
healthManager.damage(...)
```

This damage bypasses shields in accordance with the mechanic.

---

## Summonable Structures

`PlacedStructure` is the abstract foundation for structures summoned by abilities, such as:

- `Healing Totem`
- `Tree of Life`
- `Barricade`

### Structure Contract

Every structure implements:

| Method | Purpose |
|---|---|
| `tick()` | Executes periodic logic such as growth, healing, or expiration. Returns `true` when the structure should be removed. |
| `remove()` | Performs cleanup, including restoring original blocks and despawning entities. |
| `getOccupiedBlocks()` | Returns the physical blocks forming the structure. |

`getOccupiedBlocks()` is particularly important because it allows generalized interaction without repeated `instanceof` checks across consumers.

### StructureManager

`StructureManager` acts as the central registry.

A single periodic task, `StructureTickTask`, calls `tick()` on all active structures.

### StructureBlueprint

`StructureBlueprint` represents a list of block offsets relative to the invocation point:

```text
(dx, dy, dz, Material)
```

Static structures such as `Barricade` use a fixed blueprint.

Animated structures such as `Tree of Life` use an ordered list of blueprints, one for each growth phase.

Each phase appends only new blocks without duplicating blocks that already exist.

### `/capturestructure`

The development command:

```text
/capturestructure
```

automatically generates blueprint code by scanning hand-placed blocks in-game.

This eliminates the need to calculate block offsets manually.

### StructureDemolitionService

`StructureDemolitionService` extracts the logic for:

1. Finding a nearby structure.
2. Checking whether it is hostile.
3. Demolishing it with feedback.

The service was refactored from logic originally duplicated inside `BullChargeTask`.

It can now also be reused by `GolemFallAbility` and future abilities without duplicating the detection logic.

> [!NOTE]
>
> Structures **never overwrite pre-existing solid terrain**.
>
> Every structure block placement checks `isReplaceable()` before placing a block, ensuring that real map blocks remain intact.

---

## Progression and Economy Systems

| System | Persistence File | Description |
|---|---|---|
| `RatingManager` | `ratings.yml` | Individual Elo, starting at `1000`. Calculates delta against the enemy team average with clamps `[0.01, 32]`. |
| `StatsManager` | `stats.yml` | Tracks `wins`, `losses`, `kills`, `deaths`, `coins`, and `totalCoinsEarned`. |
| `CombatUpgradeManager` | `combat_upgrades.yml` | 4 categories × 9 tiers. Cost: `$880 × tier²`. Applies upgrades to max health, energy, base melee damage, and cooldown reduction. |
| `RuneSelectionManager` | `runes.yml` | One active rune at a time. Proc triggers on melee hits. |
| `HatSelectionManager` / `KeyManager` | `hats.yml` / `keys.yml` | Items unlocked through `Magic Chest`; keys are purchased with coins and rarity determines drop chance. |
| `ArmorTierManager` | Derived from rating | Purely cosmetic armor based on rating tier. Attributes are cleared. The top 10 players receive true glow using `setEnchantmentGlintOverride`. |



## Lobby and Matchmaking

### PartyManager

`PartyManager` supports parties of up to **2 players** with pending invitations.

Parties are locked while any member is queued to prevent inconsistent states between the queue and group composition.

### QueueManager

`QueueManager` implements a simple **FIFO queue**.

When the queue reaches 4 players, matchmaking is attempted while guaranteeing that party members are placed on the same team.

Map availability is checked before starting a match, preventing players from being kicked when all 4 maps are occupied.

### LobbyScoreboardManager

Each player receives an individual, unshared scoreboard displaying:

- Rating
- Wins
- Kills
- Coins

`losses` and `deaths` are deliberately hidden from the display despite being tracked internally.

## Multi-Map System

`ArenaMap` encapsulates everything specific to a given map:

- World reference
- 4 spawn points
- Power-up spawn locations

`ArenaMapManager` loads the map catalog from the `maps` section of `config.yml` during plugin startup.

Each referenced world is verified to be loaded by the server.

If a world is unloaded, the map is flagged as **unavailable** rather than crashing the plugin.

---

## Entity Cleanup and Management

`EntityCleanupUtils.markAsArenaEntity(entity)` tags every plugin-spawned entity using Bukkit's `PersistentDataContainer`.

Examples include:

- Power-ups
- Orbit shields
- Damage holograms
- `BroodMother` spiderlings
- Hats
- Other arena entities

When a match ends,:

```java
sweepArenaEntities(world);
```

sweeps the specific arena world and removes all tagged entities.

This acts as a safety net when a managing class loses its reference-

## How to Add New Features

### Adding a New Ability

1. Implement `Ability`, or extend existing abstract classes / reuse `AbilityTargeting` when applicable.
2. Register its factory in `AbilityRegistry`.
3. If it requires a new manager, add that manager to `AbilityDependencies` once.

### Adding a New Debuff

1. Add the enum constant to `DebuffType`.
2. Create a `DebuffListener` implementing `onApplied()` / `onExpired()`.
3. Register it through:

```java
debuffManager.registerListener(...)
```

### Adding a New Structure

1. Extend `PlacedStructure`.
2. Implement:
    - `tick()`
    - `remove()`
    - `getOccupiedBlocks()`
3. Build its blueprint manually or through:

```text
/capturestructure
```

4. Register the structure in `StructureManager` from the invoking ability.

Once registered, the structure automatically becomes compatible with `StructureDemolitionService`.

### Adding a New Map

1. Load the world through `bukkit.yml` or `WorldCreator`.
2. Add its configuration entry under `maps` in `config.yml`.
3. Restart the server.

`ArenaMapManager` will automatically detect and register the map during startup.