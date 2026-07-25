# Restoration Notes (v6)

## The real problem behind the "Object cannot be converted to X" flood
The decompiled source that was already sitting in NetherViewReloaded-master.zip
(`ImmersivePortalsPaperized-1.1.0-all/`) had lost generic type information across the
`bukkit` module — fields like `Map<Location, Set<IPortal>> portals` were decompiled as raw
`Map portals`, `Cache<String, YamlConfiguration>` as raw `Cache`, etc. This wasn't a build
config problem at all — `api` and `shared` compiled fine because their generics happened to
survive that decompile; `bukkit`'s didn't.

## Fix
Re-decompiled `ImmersivePortalsPaperized-1.1.0-all.jar` from scratch using Vineflower 1.11.1
(`-dgs=1 -rsy=1` — generic signature recovery + synthetic renaming), which correctly recovers
the `Signature` attribute javac embeds for every generic field/method. Verified spot checks
(`Cache<String, YamlConfiguration>`, `Map<Location, Set<IPortal>>`) came back fully typed
this time. Re-ran the exact same module-splitting + shaded-import-fix pipeline as before on
this new decompile.

## Build.gradle fixes (unchanged from v5, carried over)
- `api/build.gradle`: added `compileOnly "io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT"`
- `bukkit/build.gradle`: recreated from scratch — paper-api, ProtocolLib, VaultAPI (via
  JitPack coordinates `com.github.MilkBowl:VaultAPI:1.7`, with `exclude group: "org.bukkit"`
  to avoid its stale transitive `bukkit:1.13.1` colliding with Paper's own capability),
  Caffeine
- `velocity/build.gradle`: added `com.moandjiezana.toml:toml4j:0.7.2` for parsing
  `velocityconfig.toml`

## Verified clean
- No leftover `org.envel.betterportals` references anywhere
- Spot-checked generic-heavy fields recovered properly across bukkit
- All 33 resource files present in the right module resource dirs

## Still worth double-checking
- This was re-decompiled fresh, so re-verify the full build end to end — there may be other
  spots Vineflower phrases slightly differently than the original decompile (switch
  expressions, var usage, etc.) that could need minor syntax tweaks, though none showed up
  in spot checks.
- Dependency versions are still my best guesses from imports, not necessarily your original
  exact versions.
- Comments/Javadoc are still gone (decompiled source only, this was never going to change).
- No `.git` history for `ImmersivePortalsPaperized` itself.
