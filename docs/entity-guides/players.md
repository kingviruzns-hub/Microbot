# Player Gotchas

Rules for working with local-player and other-player lookups (`Rs2Player`, `Rs2PlayerModel`, `ActorModel`).

## 1. The local player can be null — treat it as "state not ready", never deref it

`Microbot.getClient().getLocalPlayer()` returns `null` during login, logout, world hops, and death/respawn
transitions. Every helper that wraps it must return `null` on that condition instead of throwing, and every
caller must guard the result *before* touching it — a null check inside the caller only works if the helper
is capable of producing null.

**Why this matters:** `Rs2Player.getLocalPlayer()` used to run
`getPlayers(p -> p.getId() == Microbot.getClient().getLocalPlayer().getId(), ...)` inside its filter, so it
**threw** an NPE whenever the local player was absent but other players were still in view. That made every
caller's `if (localPlayer == null) return;` guard dead code and crashed scripts mid-flow — e.g. ViruzFFABot
died exactly while walking back from a lure drag (`antiLure()`), where the tile is null across logout/respawn
boundaries.

**Pattern to follow:**

```java
// wrong — derefs the client's local player inside a predicate; throws instead of returning null
public static Rs2PlayerModel getLocalPlayer() {
    return getPlayers(p -> p.getId() == Microbot.getClient().getLocalPlayer().getId(), true)
            .findFirst().orElse(null);
}

// right — capture once, bail out on null, then filter by the captured id
public static Rs2PlayerModel getLocalPlayer() {
    Player local = Microbot.getClient().getLocalPlayer();
    if (local == null) return null;
    int localId = local.getId();
    return getPlayers(p -> p.getId() == localId, true).findFirst().orElse(null);
}
```

Also guard downstream nulls a null local player cascades into — a `WorldPoint` re-read can be null even when
the model is not, and `WorldArea.contains(null)` NPEs inside `distanceTo`:

```java
WorldPoint tile = localPlayer.getWorldLocation();
if (tile == null) return;          // WorldArea.contains(null) throws
```

**Where this applies:** `Rs2Player.getLocalPlayer()`, `Rs2Player.getWorldLocation_Internal()`,
`ActorModel.getWorldLocation()`, and all 29 `Rs2Player.getLocalPlayer()` call sites in
`ViruzFFABotScript` — plus any script loop that refreshes the local player between sleeps.

**Defensive check:** never call `.something()` on the result of `Rs2Player.getLocalPlayer()` in the same
expression that produced it; the null check must be its own statement.

## 2. Snapshot properties race the client thread — capture each read once

Model getters like `getInteracting()` and `getWorldLocation()` hop to the client thread on **every
invocation** (`runOnClientThreadOptional` / `invoke`), so two calls in one expression are two independent
reads that can disagree. Evaluating `x.getInteracting()` once for `instanceof` and again for `.getName()`
can NPE when the actor stops interacting between the calls.

**Why this matters:** ViruzFFABot logged
`Cannot invoke "net.runelite.api.Actor.getName()" because the return value of "Rs2PlayerModel.getInteracting()" is null`
from `findAttackerOfBluePhat` — the predicate called `getInteracting()` three times and the middle read was
already null by the time `.getName()` ran.

**Pattern to follow:**

```java
// wrong — three independent client-thread reads; any of them can flip between calls
&& p.getInteracting() instanceof Player
&& p.getInteracting().getName() != null
&& bluePhatNames.contains(p.getInteracting().getName().toLowerCase(Locale.ROOT).trim())

// right — one read, then use the local
Actor interacting = p.getInteracting();
if (!(interacting instanceof Player)) return false;
String targetName = interacting.getName();
return targetName != null && bluePhatNames.contains(targetName.toLowerCase(Locale.ROOT).trim());
```

The same applies to `WorldPoint` — capture `localPlayer.getWorldLocation()` into a local before a loop
condition or an `area.contains(...)` chain instead of re-reading it per clause.

**Where this applies:** predicates passed to `getPlayers(...)`, loop conditions in scripts, any comparator
over player locations (guard the `WorldPoint` against null there too — `distanceTo` will NPE).

**Defensive check:** count `getInteracting()` / `getWorldLocation()` occurrences per expression; more than
one means capture it in a local first.
