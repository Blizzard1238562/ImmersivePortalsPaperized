package org.envel.immersiveportalspaperized.api;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown when attempting to remove a portal predicate that was never registered.
 */
public class UnknownPredicateException extends IllegalArgumentException {
   public UnknownPredicateException(@NotNull PortalPredicate predicate) {
      super("Attempted to remove predicate that wasn't added. Type: " + predicate.getClass().getName());
   }
}
