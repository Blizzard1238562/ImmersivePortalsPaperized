package org.envel.immersiveportalspaperized.bukkit.util;

import java.util.Arrays;

/**
 * ArrayUtil.
 */
public class ArrayUtil {
   public static <T> T[] removeFirstElement(T[] array) {
      if (array.length == 0) {
         throw new IllegalArgumentException("Cannot remove element of empty array");
      } else {
         return (T[])Arrays.copyOfRange(array, 1, array.length);
      }
   }

   public static <T> T[] removeLastElement(T[] array) {
      if (array.length == 0) {
         throw new IllegalArgumentException("Cannot remove element of empty array");
      } else {
         return (T[])Arrays.copyOf(array, array.length - 1);
      }
   }
}


