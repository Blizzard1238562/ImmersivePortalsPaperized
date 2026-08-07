package org.envel.immersiveportalspaperized.bukkit.nms;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * NOTE on the ProtocolLib -&gt; PacketEvents migration:
 * <p>
 * This class used to also have {@code getActualDataWatcher(Entity)} and
 * {@code getRawEntitySpawnPacket(Entity)}, both removed here on purpose:
 * <p>
 * - {@code getRawEntitySpawnPacket} built a bare (id + entity type only) spawn packet that its
 *   one caller, {@code EntityPacketManipulator#showEntity}, filled in further afterwards.
 *   PacketEvents' {@code WrapperPlayServerSpawnEntity} takes every field through its
 *   constructor, so that two-step "build empty, mutate later" pattern doesn't apply anymore -
 *   {@code showEntity} now builds the full wrapper directly. See that method for the
 *   entity-type conversion and Hanging-direction rotation logic that used to live partly here.
 * <p>
 * - {@code getActualDataWatcher} used {@code WrappedDataWatcher.getEntityWatcher(entity)} to
 *   generically read the CURRENT metadata (glowing, on fire, sneaking, pose, ...) of an
 *   arbitrary live entity, via ProtocolLib's own NMS reflection. PacketEvents does not provide
 *   an equivalent - it's a packet library, not a live-entity-introspection library, so this
 *   capability genuinely has no 1:1 replacement here. See the comment on
 *   {@code EntityPacketManipulator#sendMetadata} for how this is currently handled (not yet
 *   resolved - deliberately left as an open decision rather than a guess) and the two realistic
 *   ways to close the gap.
 */
public class EntityUtil {
}


