# ICPM — "I Can't Play MITE"

> **Disclaimer:** ICPM (I Can't Play MITE) is a fan-made derivative port of the classic hardcore mod **MITE** (Minecraft Is Too Easy). All package and internal names have been changed to avoid infringement. Feedback and corrections are always welcome.

**ICPM** is a hardcore survival overhaul mod for **Minecraft 1.21.11 (Fabric Loader)**. The name is a self-deprecating joke: the moment you step into its world, you'll realize you can no longer "play" the relaxed vanilla survival you were used to.

Built on MITE's design philosophy, ICPM rewrites vanilla's overly forgiving survival pace into a journey of planning, patience, and resource management — not by throwing monster hordes or absurd numbers at you, but by interlocking realistic systems where every decision carries a cost.

- **Environment:** Minecraft 1.21.11 + Fabric Loader + Fabric API + Fabric Language Kotlin
- **Target audience:** Solo/survival-oriented hardcore overhaul players who find vanilla "too easy".

---

## Core Gameplay

### 1. Realistic Satiation & Nutrition
- **Dual-slot system:** `satiation` (fullness) + `nutrition` jointly drive eating and health regeneration. Foods grant realistic values from the R196 table — no more "one bite fully heals".
- **Nutrition-driven regen:** Higher nutrition = faster natural healing; hitting zero nutrition triggers "starvation" and continuous health loss.
- **Level-scaled endurance:** Satiation/nutrition caps rise with level, forcing you to balance combat and supply.

### 2. Animals That "Live" (Wellness System)
- **Three wellness factors:** hunger, thirst, and crowding determine livestock state — neglected, thirsty, or cramped animals won't quietly wait to be harvested.
- **Active care:** provide water, feed, and open space; ignore them and both output and health decline.
- **Spook contagion:** attacking one animal spreads panic to nearby kin, scattering the whole group ("don't swing your blade in the sheep pen").
- **Breeding & inheritance:** offspring must reach a growth threshold to mature and **inherit their parents' wellness**; cows' health raised to 10 hearts.
- **Real output chain:** milking, egg-laying, feather drops, manure, trampling — animal behavior tangibly changes the world.

### 3. Weighty Tools & Mining
- **Custom mining rules:** obsidian, workbenches, metal anvils restored to vanilla-equivalent speeds; most blocks are noticeably slower, making every mining trip meaningful.
- **Tier correction:** tool tier directly affects mining progress; inferior tools barely move; **negative XP levels** further slow mining.

### 4. Experience & Enchanting That "Bite Back"
- **Negative XP levels:** XP can go negative (down to −40), acting as a real penalty (worse damage modifiers, slower mining) rather than "no bonus".
- **Metal Anvil:** real durability (wear persists across restarts/drops/variants), can **fuse two enchantment books**, smash books onto gear, and rename items.
- **Shield-mounted tools:** craft any tool/weapon with a shield at the ICPM workbench to get a "shielded" tool — right-click to block, **halving incoming damage** (R196 rules: only consumes tool durability, no arrow blocking, no knockback immunity).

### 5. Hardcore Crafting System
- **8-tier ICPM workbench:** flint workbench + 7 metal tiers (copper/silver/gold/iron/ancient-metal/mithril/adamantite), recipes faithfully aligned with R196.
- **Flint workbench polymorphic look:** single block + `wood` state, 11 log-derived appearances (including user-requested mangrove) without new block IDs; old-save compatible.
- **Craft quality:** quality set by level (lower bound) and XP (upper bound); cycle result in `[min,max]`, higher quality costs XP.
- **MITE shears (6):** copper/gold/silver/mithril/adamantine/ancient-metal, faithfully porting R196's slow, durability-hungry shearing rules.

### 6. The Underworld
- **Access:** rune gate / portal. Rune stones encode an "address" in 0–15 variants and teleport within the same dimension by world seed (mithril radius 5000 / adamantite radius 40000); landing does not auto-build a gate.
- **Dimension drop:** range **y −60 ~ 127** (188 layers); **y ≤ 0 is entirely deepslate**, matching modern versions; vanilla ores spawn here as deepslate variants.
- **Mantle + bedrock floor:** shifted down to **y −60 ~ −55** (mantle seals the bottom, bedrock at most 3 layers, basins where `bedrock_noise ≤ 0`); top bedrock ceiling remains at world top.
- **Random spawner cages:** negative layers randomly generate ancient-corpse / ancient-corpse-guard cages.
- **Rare Ancient City:** reuses vanilla jigsaw templates to very rarely spawn a Deep Dark structure underground (sculk series, candles, reinforced deepslate, etc.).
- **Negative-layer deepslate ores:** mineable vanilla deepslate coal, plus ICPM deepslate silver / mithril / adamantine.

### 7. Threats & Monsters
- **Blood-moon giant zombie:** on blood moons, surface zombies are replaced at 1/200 chance — 6× size, 50 attack.
- **Four new mobs (1.0.5):** Fire Elemental (only water/snowballs hurt), Hell Creeper (2× blast radius, drops hell shards), Dread Wolf (actively hunts on blue-moon nights), Grey Silverfish.
- **Earth Elemental:** damageable by tool category (non-wood variants: pickaxe/war-hammer melee + fall/void/explosion; wood variants: axe/battle-axe), fire behavior preserved.
- **Underworld-exclusive:** ancient corpse, ancient-corpse guard, ancient bone king, earth elemental, bat (bats spawn only in this dimension).
- **Spawn weights** aligned with R196 (ghoul/wight/shade/lurker/wraith, etc.).

### 8. World & Rhythm
- **Village preamble:** no villages generate for the first **60 days**, forcing you to open the frontier alone first.
- **End strongholds:** only **3** (innermost ring, 120° apart), sparser than vanilla.
- **Sleeping:** can sleep in daytime, sleep until 5:00 next day without forced wake, 8× health regen while sleeping.
- **Lava source conversion:** new worlds auto-enable `lava_source_conversion` so lava naturally forms source blocks.
- **Minerals:** ICPM silver / mithril / adamantine / edelman ores mineable in both Overworld and Underworld; gravel drops exactly 1 item per break (R196 drop table).

---

## Requirements
- Minecraft **1.21.11**
- Fabric Loader
- Fabric API
- Fabric Language Kotlin

## Installation
1. Install Fabric Loader for Minecraft 1.21.11 and the Fabric API + Fabric Language Kotlin.
2. Download the latest `[我不能玩MITE]ICPM-*.jar` from the releases.
3. Place the jar into your `mods/` folder.
4. Launch the game and start a new (or existing) world.

## Art Assets
Textures are sourced from **MITE Resource Pack 1.6.41** where a clear match exists; ICPM-original items (such as the metal spears) retain their **self-drawn textures**. If you plan to redistribute this mod publicly, please be aware that the MITE-derived textures may carry licensing restrictions from the original MITE project.

To replace any texture, drop a new PNG into the matching path under `src/main/resources/assets/icpm/` — model/blockstate/lang references do not need to change.

## License
Released into the public domain — see [LICENSE](LICENSE).
