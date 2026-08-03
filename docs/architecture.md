# Architecture Documentation

## Module Dependency Graph

```
api
 └── shared
      ├── bukkit
      │    ├── block
      │    ├── entity
      │    ├── player
      │    ├── portal
      │    └── net
      ├── bungee
      ├── velocity
      └── proxy
           └── (shared net, encryption)
final
 └── (shadows all modules above into one JAR)
```

### Module Responsibilities

- **`api`**: Pure Java interfaces and DTOs with no server API dependencies. Defines `ImmersivePortalsPaperizedAPI`, `IPortal`, `PortalPosition`, `PortalPredicate`, `IntVector`, and `PortalDirection`.
- **`shared`**: Platform-agnostic code including networking (`shared.net`), encryption (`shared.net.encryption`), logging (`shared.logging`), and reflection utilities (`shared.util`).
- **`bukkit`**: Paper/Spigot server implementation. Contains all portal logic, block view rendering, entity tracking, player data, selection wand, commands, GUI, and the Guice DI wiring (`MainModule`).
- **`bungee`**: BungeeCord proxy plugin implementation. Handles server switching and cross-server portal connections on Bungee networks.
- **`velocity`**: Velocity proxy plugin implementation. Mirrors Bungee functionality for Velocity networks.
- **`proxy`**: Cross-server networking layer shared by Bungee and Velocity. Implements encrypted client/server communication, request routing, and handshake protocol.
- **`final`**: Shadow JAR assembly module. Uses `com.gradleup.shadow` to produce `ImmersivePortalsPaperized-all-<version>.jar` with relocated dependencies.

## Dependency Injection (Guice)

The plugin uses Google Guice for DI. The root module is `MainModule` in the `bukkit` package. It binds:
- Core plugin classes (`JavaPlugin`, `ImmersivePortalsPaperized`)
- Logger (`OverrideLogger` wrapping the server logger)
- Default implementations for interfaces (`IChunkLoader` → `ModernChunkLoader`, etc.)
- Eager singletons for tasks that must start immediately (`SelectionVisualizer`, `PortalEffectsTask`, `PortalAdminGUI`, `EconomyManager`)
- Sub-modules for feature areas (`EventsModule`, `CommandsModule`, `PortalModule`, `BlockModule`, `NetworkModule`, `PlayerModule`, `EntityModule`)

Factory bindings use `FactoryModuleBuilder` with assisted inject for objects that require runtime parameters (e.g., `PlayerBlockView` created per-player per-portal).

## Scheduling Model

The plugin supports both standard Bukkit and Folia scheduling:
- **Standard Bukkit:** Tasks run on the main server thread via `Bukkit.getScheduler().runTask()`.
- **Folia:** Uses `SchedulerUtil.runForEntity(entity, runnable)` to schedule work on the entity's thread, and `SchedulerUtil.runAtLocation(location, runnable)` for location-bound work.

The main tick loop is `MainUpdate`, which runs every tick and coordinates:
1. Player view updates (`playerDataManager.onUpdate()`)
2. Entity tracking updates (`entityTrackingManager.update()`)
3. Portal activity bookkeeping (`activityManager.postUpdate()`)
4. Pending network request handling (`requestHandler.handlePendingRequests()`)
5. External block watcher updates (`blockWatcherManager.update()`)

## Cross-Server Request Lifecycle

1. Player enters a cross-server portal on server A.
2. `PortalClient` on server A sends a `TeleportRequest` to the proxy.
3. `ProxyRequestHandler` routes the request to the destination server B.
4. Server B processes the request, stores the player's destination selection, and responds.
5. Server A receives the response, finalizes the teleport, and updates the player's view.

Block data synchronization between servers uses `GetBlockDataChangesRequest` to transfer only changed blocks, minimizing bandwidth.

## Block View Rendering Pipeline

1. `PlayerBlockView` is created per player per portal when the player looks through a portal.
2. On each update, `PlayerBlockView.finishUpdate()`:
   - Computes which blocks are visible through the portal plane (`PlaneIntersectionChecker`).
   - Compares visible blocks against the player's current `PlayerBlockStates`.
   - Queues changed blocks in an `IMultiBlockChangeManager`.
   - Sends batched block change packets via ProtocolLib.
3. `BlockUpdateFinisher` schedules and deduplicates updates. A background thread (`ThreadedBlockUpdateFinisher`) processes the queue to avoid blocking the main thread.

## Portal Storage

Portals are persisted to `portals.yml` via `YamlPortalStorage`. Legacy formats are supported through `LegacyPortalLoader`. Portal data is loaded on plugin enable and saved on disable/reload.
