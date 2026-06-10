package io.github.bakedlibs.dough.items.nms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.reflection.ReflectionUtils;
import io.github.bakedlibs.dough.versions.UnknownServerVersionException;

class ItemNameAdapterBefore17 implements ItemNameAdapter {

    private final Method getCopy;
    private final Method getName;
    private final Method toString;

    ItemNameAdapterBefore17() throws NoSuchMethodException, SecurityException, ClassNotFoundException, UnknownServerVersionException {
        super();

        getCopy = ReflectionUtils.getOBCClass("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);
        getName = ReflectionUtils.getMethod(ReflectionUtils.getNMSClass("ItemStack"), "getName");
        toString = ReflectionUtils.getMethod(ReflectionUtils.getNMSClass("IChatBaseComponent"), "getString");
    }

    @Override
    @ParametersAreNonnullByDefault
    public String getName(ItemStack item) throws IllegalAccessException, InvocationTargetException {
        Object instance = getCopy.invoke(null, item);

        if (instance == null) {
            return item.getType().name();
        }

        Object name = getName.invoke(instance);

        if (name == null) {
            // Can happen for some items on legacy servers; fall back to the material name.
            return item.getType().name();
        }

        // On 1.8-1.12 NMS ItemStack#getName() already returns a String; only from 1.13 does it return
        // an IChatBaseComponent that needs #getString(). Handle both so this adapter works on all <1.17.
        if (name instanceof String) {
            return (String) name;
        }

        Object result = toString.invoke(name);
        return result != null ? (String) result : item.getType().name();
    }

}
