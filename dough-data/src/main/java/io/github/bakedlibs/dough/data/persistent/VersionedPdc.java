package io.github.bakedlibs.dough.data.persistent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;

/**
 * Universal-jar support: a fully reflective, version-safe persistent-data backend keyed by a plain
 * {@code "namespace:key"} string and operating on an opaque holder ({@code ItemMeta}, {@code TileState},
 * etc.).
 * <p>
 * On 1.14+ it uses the real {@code PersistentDataContainer}; on 1.8&ndash;1.13 (no PDC) it falls back to
 * real item NBT via CraftMetaItem's {@code unhandledTags} map. No 1.12+/1.14+ type is referenced in this
 * class's signatures, so it loads on every server version. This lets {@link PersistentDataAPI} expose
 * String-keyed overloads (String/int/long/byte/double) that work everywhere without breaking its existing
 * {@code NamespacedKey} API.
 */
public final class VersionedPdc {

    private static final boolean PDC_SUPPORTED = classExists("org.bukkit.persistence.PersistentDataContainer");
    private static final String NMS_PACKAGE = resolveNmsPackage();

    private VersionedPdc() {}

    // --- String --------------------------------------------------------------

    public static void setString(@Nullable Object holder, String key, String value) {
        if (PDC_SUPPORTED) {
            pdcSet(holder, key, "STRING", value);
        } else {
            nbtSet(holder, key, "NBTTagString", String.class, value);
        }
    }

    @Nullable
    public static String getString(@Nullable Object holder, String key) {
        if (PDC_SUPPORTED) {
            Object value = pdcGet(holder, key, "STRING");
            return value instanceof String ? (String) value : null;
        } else {
            Object tag = nbtGet(holder, key);
            return tag == null ? null : (String) extractField(tag, String.class);
        }
    }

    // --- int -----------------------------------------------------------------

    public static void setInt(@Nullable Object holder, String key, int value) {
        if (PDC_SUPPORTED) {
            pdcSet(holder, key, "INTEGER", value);
        } else {
            nbtSet(holder, key, "NBTTagInt", int.class, value);
        }
    }

    public static int getInt(@Nullable Object holder, String key, int defaultValue) {
        Number value = getNumber(holder, key, "INTEGER", int.class);
        return value != null ? value.intValue() : defaultValue;
    }

    // --- long ----------------------------------------------------------------

    public static void setLong(@Nullable Object holder, String key, long value) {
        if (PDC_SUPPORTED) {
            pdcSet(holder, key, "LONG", value);
        } else {
            nbtSet(holder, key, "NBTTagLong", long.class, value);
        }
    }

    public static long getLong(@Nullable Object holder, String key, long defaultValue) {
        Number value = getNumber(holder, key, "LONG", long.class);
        return value != null ? value.longValue() : defaultValue;
    }

    // --- double --------------------------------------------------------------

    public static void setDouble(@Nullable Object holder, String key, double value) {
        if (PDC_SUPPORTED) {
            pdcSet(holder, key, "DOUBLE", value);
        } else {
            nbtSet(holder, key, "NBTTagDouble", double.class, value);
        }
    }

    public static double getDouble(@Nullable Object holder, String key, double defaultValue) {
        Number value = getNumber(holder, key, "DOUBLE", double.class);
        return value != null ? value.doubleValue() : defaultValue;
    }

    // --- byte / boolean ------------------------------------------------------

    public static void setByte(@Nullable Object holder, String key, byte value) {
        if (PDC_SUPPORTED) {
            pdcSet(holder, key, "BYTE", value);
        } else {
            nbtSet(holder, key, "NBTTagByte", byte.class, value);
        }
    }

    public static byte getByte(@Nullable Object holder, String key, byte defaultValue) {
        Number value = getNumber(holder, key, "BYTE", byte.class);
        return value != null ? value.byteValue() : defaultValue;
    }

    // --- has / remove --------------------------------------------------------

    public static boolean has(@Nullable Object holder, String key, String pdcTypeName) {
        if (holder == null) {
            return false;
        }

        if (PDC_SUPPORTED) {
            Object container = invoke(holder, "getPersistentDataContainer");
            Object type = pdcType(pdcTypeName);
            Object bukkitKey = toBukkitKey(key);

            if (container == null || type == null || bukkitKey == null) {
                return false;
            }

            return Boolean.TRUE.equals(invoke(container, "has", bukkitKey, type));
        } else {
            return nbtGet(holder, key) != null;
        }
    }

    public static void remove(@Nullable Object holder, String key) {
        if (holder == null) {
            return;
        }

        if (PDC_SUPPORTED) {
            Object container = invoke(holder, "getPersistentDataContainer");
            Object bukkitKey = toBukkitKey(key);

            if (container != null && bukkitKey != null) {
                invoke(container, "remove", bukkitKey);
            }
        } else {
            try {
                Map<String, Object> tags = unhandledTags(holder);

                if (tags != null) {
                    tags.remove(key);
                }
            } catch (Throwable ignored) {
                // nothing we can do
            }
        }
    }

    // --- shared value path ---------------------------------------------------

    @Nullable
    private static Number getNumber(@Nullable Object holder, String key, String pdcTypeName, Class<?> primitive) {
        if (PDC_SUPPORTED) {
            Object value = pdcGet(holder, key, pdcTypeName);
            return value instanceof Number ? (Number) value : null;
        } else {
            Object tag = nbtGet(holder, key);
            return tag == null ? null : (Number) extractField(tag, primitive);
        }
    }

    // --- PDC path (1.14+) ----------------------------------------------------

    private static void pdcSet(@Nullable Object holder, String key, String pdcTypeName, Object value) {
        if (holder == null) {
            return;
        }

        Object container = invoke(holder, "getPersistentDataContainer");
        Object type = pdcType(pdcTypeName);
        Object bukkitKey = toBukkitKey(key);

        if (container != null && type != null && bukkitKey != null) {
            invoke(container, "set", bukkitKey, type, value);
        }
    }

    @Nullable
    private static Object pdcGet(@Nullable Object holder, String key, String pdcTypeName) {
        if (holder == null) {
            return null;
        }

        Object container = invoke(holder, "getPersistentDataContainer");
        Object type = pdcType(pdcTypeName);
        Object bukkitKey = toBukkitKey(key);

        if (container == null || type == null || bukkitKey == null) {
            return null;
        }

        return invoke(container, "get", bukkitKey, type);
    }

    @Nullable
    private static Object pdcType(String typeName) {
        try {
            return Class.forName("org.bukkit.persistence.PersistentDataType").getField(typeName).get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Object toBukkitKey(String key) {
        Class<?> bukkitKey;

        try {
            bukkitKey = Class.forName("org.bukkit.NamespacedKey");
        } catch (Throwable ignored) {
            return null;
        }

        int index = key.indexOf(':');
        String namespace = index < 0 ? "minecraft" : key.substring(0, index);
        String value = index < 0 ? key : key.substring(index + 1);

        try {
            Object result = bukkitKey.getMethod("fromString", String.class).invoke(null, namespace + ":" + value);

            if (result != null) {
                return result;
            }
        } catch (Throwable ignored) {
            // fall through to the constructor
        }

        try {
            Constructor<?> ctor = bukkitKey.getDeclaredConstructor(String.class, String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(namespace, value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    // --- NBT fallback (1.8-1.13, ItemMeta holders) ---------------------------

    @SuppressWarnings("unchecked")
    private static void nbtSet(@Nullable Object holder, String key, String nbtTagSimpleName, Class<?> ctorParam, Object value) {
        if (holder == null) {
            return;
        }

        Constructor<?> ctor = nbtConstructor(nbtTagSimpleName, ctorParam);

        if (ctor == null) {
            return;
        }

        try {
            Map<String, Object> tags = unhandledTags(holder);

            if (tags != null) {
                tags.put(key, ctor.newInstance(value));
            }
        } catch (Throwable ignored) {
            // legacy holder without unhandledTags (e.g. a block state) - nothing we can do
        }
    }

    @Nullable
    private static Object nbtGet(@Nullable Object holder, String key) {
        if (holder == null) {
            return null;
        }

        try {
            Map<String, Object> tags = unhandledTags(holder);
            return tags == null ? null : tags.get(key);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Constructor<?> nbtConstructor(String nbtTagSimpleName, Class<?> ctorParam) {
        if (NMS_PACKAGE == null) {
            return null;
        }

        try {
            Constructor<?> ctor = Class.forName(NMS_PACKAGE + "." + nbtTagSimpleName).getConstructor(ctorParam);
            ctor.setAccessible(true);
            return ctor;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Map<String, Object> unhandledTags(Object holder) throws IllegalAccessException {
        Field field = findField(holder.getClass(), "unhandledTags");

        if (field == null) {
            return null;
        }

        field.setAccessible(true);
        return (Map<String, Object>) field.get(holder);
    }

    /**
     * Reads the single wrapped value out of an NBT tag (e.g. NBTTagString's String, NBTTagInt's int) by
     * locating the field of the matching type - the field names are obfuscated and vary by version.
     */
    @Nullable
    private static Object extractField(Object nbtTag, Class<?> primitive) {
        Class<?> boxed = box(primitive);

        try {
            for (Field field : nbtTag.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                Class<?> fieldType = field.getType();

                if (fieldType == primitive || fieldType == boxed) {
                    field.setAccessible(true);
                    return field.get(nbtTag);
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }

        return null;
    }

    // --- Reflection plumbing -------------------------------------------------

    /**
     * Invokes a method by name + assignable arg types, re-resolving against a public supertype when the
     * declaring class is a non-public CraftBukkit implementation (otherwise IllegalAccessException).
     */
    @Nullable
    private static Object invoke(@Nullable Object target, String name, Object... args) {
        if (target == null) {
            return null;
        }

        try {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
                    continue;
                }

                Class<?>[] paramTypes = method.getParameterTypes();
                boolean matches = true;

                for (int i = 0; i < args.length; i++) {
                    if (args[i] != null && !box(paramTypes[i]).isAssignableFrom(args[i].getClass())) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    return invocable(method, paramTypes).invoke(target, args);
                }
            }
        } catch (Throwable ignored) {
            // method missing on this version or invocation failed
        }

        return null;
    }

    private static Method invocable(Method method, Class<?>[] paramTypes) {
        if (Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            return method;
        }

        Method publicMethod = searchPublic(method.getDeclaringClass(), method.getName(), paramTypes);

        if (publicMethod != null) {
            return publicMethod;
        }

        try {
            method.setAccessible(true);
        } catch (Throwable ignored) {
            // strong encapsulation may forbid this
        }

        return method;
    }

    @Nullable
    private static Method searchPublic(Class<?> type, String name, Class<?>[] paramTypes) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (Modifier.isPublic(current.getModifiers())) {
                try {
                    return current.getMethod(name, paramTypes);
                } catch (NoSuchMethodException ignored) {
                    // keep walking
                }
            }

            for (Class<?> iface : current.getInterfaces()) {
                Method candidate = searchPublic(iface, name, paramTypes);

                if (candidate != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    @Nullable
    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }

        return null;
    }

    @Nullable
    private static String resolveNmsPackage() {
        try {
            String cbPackage = Bukkit.getServer().getClass().getPackage().getName();
            return "net.minecraft.server." + cbPackage.substring(cbPackage.lastIndexOf('.') + 1);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }

        if (type == int.class) {
            return Integer.class;
        } else if (type == boolean.class) {
            return Boolean.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == double.class) {
            return Double.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == byte.class) {
            return Byte.class;
        } else if (type == char.class) {
            return Character.class;
        }

        return type;
    }
}
