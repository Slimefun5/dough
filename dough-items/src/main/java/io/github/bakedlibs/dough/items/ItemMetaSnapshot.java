package io.github.bakedlibs.dough.items;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

/**
 * This is an immutable version of ItemMeta.
 * Use this class to optimize your ItemStack#getItemMeta() calls by returning
 * a field of this immutable copy.
 * <p>
 * This does not support {@link PersistentDataContainer} at the moment.
 * 
 * @author TheBusyBiscuit
 *
 */
public class ItemMetaSnapshot {

    private final Optional<String> displayName;
    private final Optional<List<String>> lore;
    private final OptionalInt customModelData;

    private final Set<ItemFlag> itemFlags;
    private final Map<Enchantment, Integer> enchantments;

    public ItemMetaSnapshot(@Nonnull ItemStack item) {
        this(item.getItemMeta());
    }

    public ItemMetaSnapshot(@Nonnull Supplier<ItemMeta> supplier) {
        this(supplier.get());
    }

    public ItemMetaSnapshot(@Nonnull ItemMeta meta) {
        this.displayName = meta.hasDisplayName() ? Optional.of(meta.getDisplayName()) : Optional.empty();
        this.lore = meta.hasLore() ? Optional.of(Collections.unmodifiableList(meta.getLore())) : Optional.empty();
        this.customModelData = readCustomModelData(meta);

        this.itemFlags = meta.getItemFlags();
        this.enchantments = meta.getEnchants();
    }

    /**
     * Reads the custom model data reflectively. {@code ItemMeta#hasCustomModelData()} and
     * {@code #getCustomModelData()} only exist from Minecraft 1.14 onwards; on the universal
     * Java-8 jar this method must not hard-link them or it would {@link NoSuchMethodError} on
     * legacy servers. Returns {@link OptionalInt#empty()} on any version that lacks the API.
     */
    @Nonnull
    private static OptionalInt readCustomModelData(@Nonnull ItemMeta meta) {
        try {
            Boolean has = (Boolean) ItemMeta.class.getMethod("hasCustomModelData").invoke(meta);
            if (Boolean.TRUE.equals(has)) {
                return OptionalInt.of((Integer) ItemMeta.class.getMethod("getCustomModelData").invoke(meta));
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // pre-1.14 server: custom model data is unsupported (NoSuchMethodError extends LinkageError)
        }

        return OptionalInt.empty();
    }

    public @Nonnull Optional<String> getDisplayName() {
        return displayName;
    }

    public @Nonnull Optional<List<String>> getLore() {
        return lore;
    }

    public @Nonnull OptionalInt getCustomModelData() {
        return customModelData;
    }

    public @Nonnull Set<ItemFlag> getItemFlags() {
        return itemFlags;
    }

    public @Nonnull Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public boolean isSimilar(@Nonnull ItemMetaSnapshot snapshot) {
        if (snapshot.displayName.isPresent() != displayName.isPresent()) {
            return false;
        } else if (snapshot.displayName.isPresent() && displayName.isPresent() && !snapshot.displayName.get().equals(displayName.get())) {
            return false;
        } else if (snapshot.lore.isPresent() && lore.isPresent()) {
            return lore.get().equals(snapshot.lore.get());
        } else {
            return !snapshot.lore.isPresent() && !lore.isPresent();
        }
    }

    public boolean isSimilar(@Nonnull ItemMeta meta) {
        boolean hasDisplayName = meta.hasDisplayName();

        if (hasDisplayName != displayName.isPresent()) {
            return false;
        } else if (hasDisplayName && displayName.isPresent() && !meta.getDisplayName().equals(displayName.get())) {
            return false;
        } else {
            boolean hasLore = meta.hasLore();

            if (hasLore && lore.isPresent()) {
                return lore.get().equals(meta.getLore());
            } else {
                return !hasLore && !lore.isPresent();
            }
        }
    }

}
