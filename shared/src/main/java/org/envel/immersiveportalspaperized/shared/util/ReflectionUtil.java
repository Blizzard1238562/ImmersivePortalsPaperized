package org.envel.immersiveportalspaperized.shared.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods for reflective access to classes, constructors, fields, and methods.
 * <p>
 * Used primarily for NMS version-agnostic module loading and late-bound optimizations.
 * </p>
 */
public class ReflectionUtil {
   public static Class<?> findClass(String name) {
      try {
         return Class.forName(name);
      } catch (ClassNotFoundException var2) {
         throw new ReflectionException(var2);
      }
   }

   public static Method findMethod(Class<?> klass, String name, Class<?>... paramTypes) {
      try {
         Method method = klass.getDeclaredMethod(name, paramTypes);
         method.setAccessible(true);
         return method;
      } catch (NoSuchMethodException var4) {
         throw new ReflectionException(var4);
      }
   }

   public static Field findField(Class<?> klass, String name) {
      return findField(klass, name, null);
   }

   public static Field findField(Class<?> klass, String name, @Nullable Class<?> verifyType) {
      try {
         Field field = klass.getDeclaredField(name);
         if (verifyType != null && !field.getType().equals(verifyType)) {
            throw new ReflectionException("Field with name " + name + " on class " + klass + " did not match expected type of " + verifyType);
         } else {
            field.setAccessible(true);
            return field;
         }
      } catch (NoSuchFieldException var4) {
         throw new ReflectionException(var4);
      }
   }

   public static Field findFieldByType(Class<?> klass, Class<?> type) {
      Field found = null;

      for (Field field : klass.getDeclaredFields()) {
         if (field.getType().equals(type)) {
            if (found != null) {
               throw new ReflectionException("Multiple instances of field with type " + type + " exist in class " + klass);
            }

            found = field;
         }
      }

      if (found == null) {
         throw new ReflectionException("No field with type " + type + " exists in class " + klass);
      } else {
         found.setAccessible(true);
         return found;
      }
   }

   public static Method findMethodByParamTypes(Class<?> klass, Class<?>... paramTypes) {
      return findMethodByTypes(klass, null, 0, 0, paramTypes);
   }

   public static Method findMethodByTypes(Class<?> klass, @Nullable Class<?> returnType, Class<?>... paramTypes) {
      return findMethodByTypes(klass, returnType, 0, 0, paramTypes);
   }

   public static Method findMethodByTypes(Class<?> klass, Class<?> returnType, int modifierMask, int modifiersValue) {
      return findMethodByTypes(klass, returnType, modifierMask, modifiersValue, new Class[0]);
   }

   public static Method findMethodByTypes(Class<?> klass, @Nullable Class<?> returnType, int modifierMask, int modifiersValue, Class<?>[] paramTypes) {
      Method found = null;

      for (Method method : klass.getDeclaredMethods()) {
         boolean matchesModifiers = (method.getModifiers() & modifierMask) == modifiersValue;
         if (matchesModifiers
            && (returnType == null || method.getReturnType().equals(returnType))
            && Arrays.equals((Object[])method.getParameterTypes(), (Object[])paramTypes)) {
            if (found != null) {
               throw new ReflectionException("Multiple instances of method existed with given types in " + klass);
            }

            found = method;
         }
      }

      if (found == null) {
         throw new ReflectionException("No method existed with given types in " + klass);
      } else {
         found.setAccessible(true);
         return found;
      }
   }

   public static Constructor<?> findConstructor(Class<?> klass, Class<?>... paramTypes) {
      try {
         Constructor<?> ctor = klass.getDeclaredConstructor(paramTypes);
         ctor.setAccessible(true);
         return ctor;
      } catch (NoSuchMethodException var3) {
         throw new ReflectionException(var3);
      }
   }

   public static Object invokeConstructor(Constructor<?> ctor, Object... args) {
      try {
         return ctor.newInstance(args);
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException var3) {
         throw new ReflectionException(var3);
      }
   }

   public static Object getField(@Nullable Object obj, Field field) {
      try {
         return field.get(obj);
      } catch (IllegalAccessException var3) {
         throw new ReflectionException(var3);
      }
   }

   public static void setField(@Nullable Object obj, Field field, Object value) {
      try {
         field.set(obj, value);
      } catch (IllegalAccessException var4) {
         throw new ReflectionException(var4);
      }
   }

   public static Object invokeMethod(@Nullable Object obj, Method method, Object... args) {
      try {
         return method.invoke(obj, args);
      } catch (IllegalAccessException | InvocationTargetException var4) {
         throw new ReflectionException(var4);
      }
   }
}
