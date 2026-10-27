package in.gov.sih.sih26135.validation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for reflection-based property inspection and fail-fast configuration
 * validation for declarative cross-field constraints.
 */
public final class ValidationPropertyUtils {

  private ValidationPropertyUtils() {}

  /**
   * Resolved property accessor containing property type and reader handles.
   */
  public record PropertyAccessor(
      String propertyName,
      Class<?> propertyType,
      Method readMethod,
      Field field
  ) {
    public Object read(Object target) {
      if (target == null) {
        return null;
      }
      try {
        if (readMethod != null) {
          return readMethod.invoke(target);
        }
        if (field != null) {
          return field.get(target);
        }
      } catch (Exception ex) {
        throw new IllegalStateException("Failed to read property '" + propertyName + "' on " + target.getClass().getName(), ex);
      }
      throw new IllegalStateException("No accessible reader for property '" + propertyName + "' on " + target.getClass().getName());
    }
  }

  private static final Map<String, PropertyAccessor> ACCESSOR_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, Boolean> DATE_SEQUENCE_CONFIG_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, Boolean> NUMBER_RANGE_CONFIG_CACHE = new ConcurrentHashMap<>();

  /**
   * Clears internal reflection and configuration caches (useful during testing).
   */
  public static void clearCaches() {
    ACCESSOR_CACHE.clear();
    DATE_SEQUENCE_CONFIG_CACHE.clear();
    NUMBER_RANGE_CONFIG_CACHE.clear();
  }

  /**
   * Resolves a property accessor on the given class, or returns null if not found.
   */
  public static PropertyAccessor resolveProperty(Class<?> targetClass, String propertyName) {
    if (targetClass == null || propertyName == null || propertyName.isBlank()) {
      return null;
    }
    String key = targetClass.getName() + "#" + propertyName;
    PropertyAccessor accessor = ACCESSOR_CACHE.get(key);
    if (accessor != null) {
      return accessor;
    }

    accessor = findPropertyAccessor(targetClass, propertyName);
    if (accessor != null) {
      ACCESSOR_CACHE.put(key, accessor);
    }
    return accessor;
  }

  private static PropertyAccessor findPropertyAccessor(Class<?> targetClass, String propertyName) {
    String capitalized = propertyName.substring(0, 1).toUpperCase(Locale.ROOT)
        + (propertyName.length() > 1 ? propertyName.substring(1) : "");

    // 1. Check get<Property>()
    try {
      Method m = targetClass.getMethod("get" + capitalized);
      if (!void.class.equals(m.getReturnType()) && m.getParameterCount() == 0) {
        return new PropertyAccessor(propertyName, m.getReturnType(), m, null);
      }
    } catch (NoSuchMethodException ignored) {}

    // 2. Check is<Property>()
    try {
      Method m = targetClass.getMethod("is" + capitalized);
      if (!void.class.equals(m.getReturnType()) && m.getParameterCount() == 0) {
        return new PropertyAccessor(propertyName, m.getReturnType(), m, null);
      }
    } catch (NoSuchMethodException ignored) {}

    // 3. Check record-style / fluid accessor <property>()
    try {
      Method m = targetClass.getMethod(propertyName);
      if (!void.class.equals(m.getReturnType()) && m.getParameterCount() == 0) {
        return new PropertyAccessor(propertyName, m.getReturnType(), m, null);
      }
    } catch (NoSuchMethodException ignored) {}

    // 4. Fall back to declared field traversal
    Class<?> current = targetClass;
    while (current != null && current != Object.class) {
      try {
        Field f = current.getDeclaredField(propertyName);
        if (!Modifier.isStatic(f.getModifiers())) {
          f.setAccessible(true);
          return new PropertyAccessor(propertyName, f.getType(), null, f);
        }
      } catch (NoSuchFieldException ignored) {
      } catch (SecurityException ex) {
        throw new IllegalStateException("Security exception accessing field '" + propertyName + "' on " + current.getName(), ex);
      }
      current = current.getSuperclass();
    }

    return null;
  }

  // =========================================================================
  // Date Types Verification
  // =========================================================================

  public static boolean isSupportedDateType(Class<?> type) {
    if (type == null) {
      return false;
    }
    return Temporal.class.isAssignableFrom(type) || Date.class.isAssignableFrom(type);
  }

  public static boolean areCompatibleDateTypes(Class<?> typeA, Class<?> typeB) {
    if (typeA == null || typeB == null) {
      return false;
    }
    if (!isSupportedDateType(typeA) || !isSupportedDateType(typeB)) {
      return false;
    }
    if (typeA.equals(typeB)) {
      return true;
    }
    if (typeA.isAssignableFrom(typeB) || typeB.isAssignableFrom(typeA)) {
      return true;
    }
    if (java.time.chrono.ChronoLocalDate.class.isAssignableFrom(typeA)
        && java.time.chrono.ChronoLocalDate.class.isAssignableFrom(typeB)) {
      return true;
    }
    if (Date.class.isAssignableFrom(typeA) && Date.class.isAssignableFrom(typeB)) {
      return true;
    }
    return false;
  }

  /**
   * Validates @DateSequence configuration fail-fast against target class metadata.
   */
  public static void validateDateSequenceConfiguration(Class<?> clazz, DateSequence annotation) {
    if (clazz == null || annotation == null) {
      return;
    }
    String startField = annotation.startField();
    String endField = annotation.endField();
    String targetField = annotation.targetField().isBlank() ? endField : annotation.targetField();

    String cacheKey = clazz.getName() + "::" + startField + "->" + endField + "@" + targetField;
    if (DATE_SEQUENCE_CONFIG_CACHE.containsKey(cacheKey)) {
      return;
    }

    PropertyAccessor startAccessor = resolveProperty(clazz, startField);
    if (startAccessor == null) {
      throw new IllegalArgumentException(String.format(
          "Invalid @DateSequence configuration on %s: startField '%s' does not exist or is not accessible",
          clazz.getName(), startField));
    }

    PropertyAccessor endAccessor = resolveProperty(clazz, endField);
    if (endAccessor == null) {
      throw new IllegalArgumentException(String.format(
          "Invalid @DateSequence configuration on %s: endField '%s' does not exist or is not accessible",
          clazz.getName(), endField));
    }

    PropertyAccessor targetAccessor = resolveProperty(clazz, targetField);
    if (targetAccessor == null) {
      throw new IllegalArgumentException(String.format(
          "Invalid @DateSequence configuration on %s: targetField '%s' does not exist or is not accessible",
          clazz.getName(), targetField));
    }

    if (!isSupportedDateType(startAccessor.propertyType())) {
      throw new IllegalArgumentException(String.format(
          "Invalid @DateSequence configuration on %s: startField '%s' (%s) is not a supported date type",
          clazz.getName(), startField, startAccessor.propertyType().getName()));
    }

    if (!isSupportedDateType(endAccessor.propertyType())) {
      throw new IllegalArgumentException(String.format(
          "Invalid @DateSequence configuration on %s: endField '%s' (%s) is not a supported date type",
          clazz.getName(), endField, endAccessor.propertyType().getName()));
    }

    if (!areCompatibleDateTypes(startAccessor.propertyType(), endAccessor.propertyType())) {
      throw new IllegalArgumentException(String.format(
          "Invalid @DateSequence configuration on %s: startField '%s' (%s) and endField '%s' (%s) have incompatible date types",
          clazz.getName(), startField, startAccessor.propertyType().getSimpleName(),
          endField, endAccessor.propertyType().getSimpleName()));
    }

    DATE_SEQUENCE_CONFIG_CACHE.put(cacheKey, Boolean.TRUE);
  }

  // =========================================================================
  // Numeric Types Verification
  // =========================================================================

  public static Class<?> boxType(Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    if (type == int.class) return Integer.class;
    if (type == long.class) return Long.class;
    if (type == double.class) return Double.class;
    if (type == float.class) return Float.class;
    if (type == short.class) return Short.class;
    if (type == byte.class) return Byte.class;
    if (type == boolean.class) return Boolean.class;
    if (type == char.class) return Character.class;
    return type;
  }

  public static boolean isSupportedNumericType(Class<?> type) {
    if (type == null) {
      return false;
    }
    Class<?> boxed = boxType(type);
    return Number.class.isAssignableFrom(boxed) && Comparable.class.isAssignableFrom(boxed);
  }

  public static boolean areCompatibleNumericTypes(Class<?> typeA, Class<?> typeB) {
    if (typeA == null || typeB == null) {
      return false;
    }
    if (!isSupportedNumericType(typeA) || !isSupportedNumericType(typeB)) {
      return false;
    }
    Class<?> boxedA = boxType(typeA);
    Class<?> boxedB = boxType(typeB);
    if (boxedA.equals(boxedB)) {
      return true;
    }
    if (boxedA.isAssignableFrom(boxedB) || boxedB.isAssignableFrom(boxedA)) {
      return true;
    }
    return false;
  }

  /**
   * Validates @NumberRange configuration fail-fast against target class metadata.
   */
  public static void validateNumberRangeConfiguration(Class<?> clazz, NumberRange annotation) {
    if (clazz == null || annotation == null) {
      return;
    }
    String minField = annotation.minField();
    String maxField = annotation.maxField();
    String targetField = annotation.targetField().isBlank() ? maxField : annotation.targetField();

    String cacheKey = clazz.getName() + "::" + minField + "->" + maxField + "@" + targetField;
    if (NUMBER_RANGE_CONFIG_CACHE.containsKey(cacheKey)) {
      return;
    }

    PropertyAccessor minAccessor = resolveProperty(clazz, minField);
    if (minAccessor == null) {
      throw new IllegalArgumentException(String.format(
          "Invalid @NumberRange configuration on %s: minField '%s' does not exist or is not accessible",
          clazz.getName(), minField));
    }

    PropertyAccessor maxAccessor = resolveProperty(clazz, maxField);
    if (maxAccessor == null) {
      throw new IllegalArgumentException(String.format(
          "Invalid @NumberRange configuration on %s: maxField '%s' does not exist or is not accessible",
          clazz.getName(), maxField));
    }

    PropertyAccessor targetAccessor = resolveProperty(clazz, targetField);
    if (targetAccessor == null) {
      throw new IllegalArgumentException(String.format(
          "Invalid @NumberRange configuration on %s: targetField '%s' does not exist or is not accessible",
          clazz.getName(), targetField));
    }

    if (!isSupportedNumericType(minAccessor.propertyType())) {
      throw new IllegalArgumentException(String.format(
          "Invalid @NumberRange configuration on %s: minField '%s' (%s) is not a supported numeric type",
          clazz.getName(), minField, minAccessor.propertyType().getName()));
    }

    if (!isSupportedNumericType(maxAccessor.propertyType())) {
      throw new IllegalArgumentException(String.format(
          "Invalid @NumberRange configuration on %s: maxField '%s' (%s) is not a supported numeric type",
          clazz.getName(), maxField, maxAccessor.propertyType().getName()));
    }

    if (!areCompatibleNumericTypes(minAccessor.propertyType(), maxAccessor.propertyType())) {
      throw new IllegalArgumentException(String.format(
          "Invalid @NumberRange configuration on %s: minField '%s' (%s) and maxField '%s' (%s) have incompatible numeric types",
          clazz.getName(), minField, minAccessor.propertyType().getSimpleName(),
          maxField, maxAccessor.propertyType().getSimpleName()));
    }

    NUMBER_RANGE_CONFIG_CACHE.put(cacheKey, Boolean.TRUE);
  }
}
