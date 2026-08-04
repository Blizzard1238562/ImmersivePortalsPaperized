package org.envel.immersiveportalspaperized.bukkit.util;

/**
 * Minimal generic tuple type.
 * <p>
 * Replaces {@code com.comphenix.protocol.wrappers.Pair}, which was used in a couple of places
 * in this codebase purely as a convenient generic tuple - unrelated to packets, and not something
 * that should depend on a packet library. Introduced as part of the ProtocolLib -> PacketEvents
 * migration to remove that incidental dependency.
 */
public record Pair<A, B>(A first, B second) {
}
