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
 * String-keyed overloads that work everywhere without breaking its existing {@code NamespacedKey} API.
 *
 * <p>
 * Currently the legacy (pre-1.14) fallback covers {@code String} values &mdash; the item-identity path that
 * matters for legacy support. Other types default gracefully on legacy servers and use PDC normally on 1.14+.
 */
public final class VersionedPdc {

    private static final boolean PDC_SUPPORTED = classExists("org.bukkit.persistence.PersistentDataContainer");
    private static final Constructor<?> NBT_TAG_STRING_CTOR = resolveNbtTagStringConstructor();

    private VersionedPdc() {}

    public static void setString(@Nullable Object holder, String key, String value) {
        if (holder == null) {
            return;
        }

        if (PDC_SUPPORTED) {
            Object container = invoke(holder, "getPersistentDataContainer");
            Object type = pdcType("STRING");
            Object bukkitKey = toBukkitKey(key);

            if (container != null && type != null && bukkitKey != null) {
                invoke(container, "set", bukkitKey, type, value);
            }
        } else {
            nbtSetString(holder, key, value);
        }
    }

    @Nullable
    public static String getString(@Nullable Object holder, String key) {
        if (holder == null) {
            return null;
        }

        if (PDC_SUPPORTED) {
            Object container = invoke(holder, "getPersistentDataContainer");
            Object type = pdcType("STRING");
            Object bukkitKey = toBukkitKey(key);

            if (container == null || type == null || bukkitKey == null) {
                return null;
            }

            Object value = invoke(container, "get", bukkitKey, type);
            return value instanceof String ? (String) value : null;
        } else {
            return nbtGetString(holder, key);
        }
    }

    public static boolean has(@Nullable Object holder, String key) {
        if (holder == null) {
            return false;
        }

        if (PDC_SUPPORTED) {
            Object container = invoke(holder, "getPersistentDataContainer");
            Object type = pdcType("STRING");
            Object bukkitKey = toBukkitKey(key);

            if (container == null || type == null || bukkitKey == null) {
                return false;
            }

            return Boolean.TRUE.equals(invoke(container, "has", bukkitKey, type));
        } else {
            return nbtGetString(holder, key) != null;
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
            nbtRemove(holder, key);
        }
    }

    // --- PDC helpers (1.14+) -------------------------------------------------

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
            Method fromString = bukkitKey.getMethod("fromString", String.class);
            Object result = fromString.invoke(null, namespace + ":" + value);

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
    private static void nbtSetString(Object holder, String key, String value) {
        if (NBT_TAG_STRING_CTOR == null) {
            return;
        }

        try {
            Map<String, Object> tags = unhandledTags(holder);

            if (tags != null) {
                tags.put(key, NBT_TAG_STRING_CTOR.newInstance(value));
            }
        } catch (Throwable ignored) {
            // legacy holder without unhandledTags (e.g. a block state) - nothing we can do here
        }
    }

    @Nullable
    private static String nbtGetString(Object holder, String key) {
        try {
            Map<String, Object> tags = unhandledTags(holder);

            if (tags == null) {
                return null;
            }

            Object tag = tags.get(key);
            return tag == null ? null : extractNbtString(tag);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void nbtRemove(Object holder, String key) {
        try {
            Map<String, Object> tags = unhandledTags(holder);

            if (tags != null) {
                tags.remove(key);
            }
        } catch (Throwable ignored) {
            // nothing we can do
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

    @Nullable
    private static String extractNbtString(Object nbtTagString) throws IllegalAccessException {
        for (Field field : nbtTagString.getClass().getDeclaredFields()) {
            if (field.getType() == String.class) {
                field.setAccessible(true);
                return (String) field.get(nbtTagString);
            }
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
    private static Constructor<?> resolveNbtTagStringConstructor() {
        try {
            String cbPackage = Bukkit.getServer().getClass().getPackage().getName();
            String version = cbPackage.substring(cbPackage.lastIndexOf('.') + 1);
            Constructor<?> ctor = Class.forName("net.minecraft.server." + version + ".NBTTagString").getConstructor(String.class);
            ctor.setAccessible(true);
            return ctor;
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
