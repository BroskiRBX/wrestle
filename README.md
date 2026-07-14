# Wrestling Mania

A Fabric mod for Minecraft 1.20.1 that turns your world into a wrestling promotion. Spawn a full indoor arena with one item, put opponents through breakable tables, and hit animated finishers with cooldowns on a custom HUD.

Built for Fabric Loader 0.15.x, Fabric API 0.91.x, GeckoLib 4.x, Java 17. Works in singleplayer and on dedicated servers.

## Setup

You need JDK 17 and Gradle 8.4 or newer (Loom 1.5 requires Gradle 8.x).

Option A, IDE (easiest):
1. Open the project folder in IntelliJ IDEA. It will import the Gradle project automatically.
2. Wait for the sync to finish, then run the `runClient` Gradle task.

Option B, command line:
```
gradle wrapper --gradle-version 8.4
./gradlew genSources
./gradlew runClient
```

To build the jar:
```
./gradlew build
```
The finished mod jar lands in `build/libs/`.

Heads up: this project ships without the Gradle wrapper jar, which is why you generate it with the `gradle wrapper` command above (or just let your IDE handle everything).

## What's in the mod

### 1. Arena Ticket
Right click the ground and it builds a 30x20x15 indoor arena rotated to face away from you: gray concrete floor, smooth stone walls with a black stripe, shroomlight ceiling grid, a raised 12x12 ring with a white mat and quartz apron, iron corner posts with lanterns, chain ropes at two heights, two stepped rows of stair seating along each side, an entrance ramp with red carpet, and a banner wall at the back.

If the space is not flat and clear you get an actionbar warning instead. Everything is placed manually in code (`ArenaTicketItem.java`), no structure files, so it is all hackable.

### 2. Breakable Table
A placeable table block with 3 hit points. Each punch (left click) plays crack particles and a wood hit sound. The third hit shatters it: loud break, splinter burst, and it drops planks (count is configurable). A fast moving entity flying through the open space under the tabletop breaks it instantly, and landing hard on top of it does too. Suplex someone onto a table. You know you want to.

### 3. Finisher Moves
Hold the Wrestling Gloves in your main hand:

| Key | Move | Effect | Cooldown |
|-----|------|--------|----------|
| G | Suplex | Launches the target up, Slowness + Mining Fatigue 2s | 8s |
| H | DDT | Lifts the target then spikes them down, Nausea 3s | 12s |
| J | Clothesline | Heavy horizontal knockback, Blindness 1.5s | 6s |

All moves are server authoritative: the client only sends "I pressed the button", and the server checks the gloves, the cooldown, and finds the target itself. Cooldowns show on a HUD panel on the left side of the screen (only while holding the gloves). Each move triggers a GeckoLib animation on the gloves that syncs to everyone nearby.

Keybinds are rebindable in Options > Controls under the Wrestling Mania category.

### 4. Crowd Cheers
Arenas have ambient crowd noise. The Arena Ticket hides a 2x2 patch of Arena Core blocks under the ring. Every few seconds the mod scans around each player for blocks in the `#wrestling_mania:arena_marker` tag and plays a positional cheer whose volume scales with how close you are (20 block radius). Because it scans by tag instead of remembering positions, it survives server restarts, and you can make any build an "arena" by hiding an Arena Core block in it yourself.

## Configuration

A config file is written to `config/wrestling_mania.json` on first launch:

```json
{
  "tablePlankDrops": 4,
  "tableBreakVelocity": 0.6,
  "crowdIntervalTicks": 100,
  "crowdBaseVolume": 1.0
}
```

- `tablePlankDrops`: planks dropped when a table shatters
- `tableBreakVelocity`: how fast an entity must be moving (blocks per tick) to smash through a table
- `crowdIntervalTicks`: how often the crowd system runs (100 = 5 seconds, minimum 20)
- `crowdBaseVolume`: crowd volume at point blank range

## Swapping in a real crowd sound

The `crowd_cheer` sound event currently layers two vanilla sounds (villager celebrate + level up) as placeholders. To use a real crowd recording:
1. Drop your file at `src/main/resources/assets/wrestling_mania/sounds/crowd_cheer.ogg` (mono OGG recommended for positional audio).
2. Change `sounds.json` to:
```json
{
  "crowd_cheer": {
    "subtitle": "subtitles.wrestling_mania.crowd_cheer",
    "sounds": ["wrestling_mania:crowd_cheer"]
  }
}
```

## Known limitations

- GeckoLib 4.x moves fast between minor versions. This project targets 4.4.4 for 1.20.1. If the item renderer or triggerAnim signatures do not line up after a version bump, check the GeckoLib wiki for the matching pattern and adjust `WrestlingGlovesItem`.
- The gloves geo model and all textures are simple programmer-art placeholders. Swap in Blockbench exports whenever you want.
- The "thrown through the table" check uses the open space in the table's collision shape, so an entity gliding exactly along the tabletop surface will not always trigger it. Landing on top with fall distance does.
- The Arena Core block has no loot table, so it drops nothing when mined. Intentional, it is a hidden marker.

## Project layout

```
src/main/java/com/example/wrestlingmania/
  WrestlingMania.java          main entrypoint, table punch event
  WrestlingManiaClient.java    keybinds, HUD, S2C packet receiver
  item/ArenaTicketItem.java    arena generation
  item/WrestlingGlovesItem.java GeckoLib item with triggerable anims
  block/BreakableTable*.java   3 HP table + block entity
  combat/FinisherMove*.java    move data + server side move logic
  network/ModPackets.java      C2S move request, S2C cooldown sync
  sound/CrowdCheerManager.java tag scan + per player positional audio
  client/                      HUD overlay, cooldown mirror, geo renderer
  registry/                    items, blocks, block entities, sounds, tags
  config/WrestlingConfig.java  JSON config
```

MIT licensed. Have fun. BAH GAWD.
