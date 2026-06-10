package io.github.bakedlibs.dough.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public final class CustomItemStack {

    /**
     * Java-8 universal port: a {@link Material} passed here may be {@code null} on a legacy server
     * (XMaterial#parseMaterial returns null for a material that version doesn't have). Substitute a
     * harmless placeholder so menu/icon items still build instead of throwing {@code new ItemStack(null)}.
     * {@code PAPER} exists on every supported Minecraft version.
     */
    private static Material safe(@Nullable Material material) {
        return material != null ? material : Material.PAPER;
    }

    private CustomItemStack() {
        throw new IllegalStateException("Cannot instantiate CustomItemStack");
    }

    public static ItemStack create(ItemStack itemStack, Consumer<ItemMeta> metaConsumer) {
        return new ItemStackEditor(itemStack).andMetaConsumer(metaConsumer).create();
    }

    public static ItemStack create(@Nullable Material material, Consumer<ItemMeta> metaConsumer) {
        return new ItemStackEditor(safe(material)).andMetaConsumer(metaConsumer).create();
    }

    public static ItemStack create(ItemStack item, @Nullable String name, String... lore) {
        return new ItemStackEditor(item)
                .setDisplayName(name)
                .setLore(lore)
                .create();
    }

    public static ItemStack create(@Nullable Material material, @Nullable String name, String... lore) {
        return create(new ItemStack(safe(material)), name, lore);
    }

    public static ItemStack create(@Nullable Material type, @Nullable String name, List<String> lore) {
        return create(new ItemStack(safe(type)), name, lore.toArray(String[]::new));
    }


    public static ItemStack create(ItemStack item, List<String> list) {
        return create(new ItemStack(item), list.get(0), list.subList(1, list.size()).toArray(String[]::new));
    }

    public static ItemStack create(@Nullable Material type, List<String> list) {
        return create(new ItemStack(safe(type)), list);
    }

    public static ItemStack create(ItemStack item, int amount) {
        return new ItemStackEditor(item).setAmount(amount).create();
    }

    /**
     * Clones the item stack and sets its type
     *
     * @param itemStack The item
     * @param type      The new type
     * @return Returns the item with a new type
     * @deprecated Setting the type via {@link ItemStack#setType(Material)} will not be supported soon.
     */
    @Deprecated(forRemoval = true)
    public static ItemStack create(ItemStack itemStack, @Nullable Material type) {
        return new ItemStackEditor(itemStack).andStackConsumer(item -> item.setType(safe(type))).create();
    }

}
