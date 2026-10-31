package com.zaixiayesheng.ultrastacks.mixin.compat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * 兼容 mixin 的门：装了什么模组，才让对应的 mixin 生效。
 *
 * Gates the compat mixins: a mixin only loads when its mod is present.
 */
public final class CompatMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("UltraStacks/Compat");

    private static final String SOPHISTICATED = "sophisticatedcore";
    private static final String MEKANISM = "mekanism";
    private static final String MEKANICAL_CREATE = "mekanicalcreate";
    private static final String AE2 = "ae2";
    private static final String EXTENDED_AE = "extendedae";
    private static final String EXTENDED_AE_LEGACY = "expatternprovider";
    private static final String REFINED_STORAGE = "refinedstorage";
    private static final String SOPHISTICATED_INTERACTIONS = "sophisticatedinventoryinteractions";
    private static final String IRON_FURNACES = "ironfurnaces";
    private static final String CUCUMBER = "cucumber";
    private static final String FUNCTIONAL_STORAGE = "functionalstorage";
    private static final String FAST_WORKBENCH = "fastbench";
    private static final String IMMERSIVE_ENGINEERING = "immersiveengineering";
    private static final String CRAFTING_TWEAKS = "craftingtweaks";
    private static final String SUPERB_WARFARE = "superbwarfare";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String[] modIds = modIdsFor(mixinClassName);
        if (modIds == null || Arrays.stream(modIds).anyMatch(CompatMixinPlugin::isLoaded)) {
            return true;
        }
        LOGGER.debug("Skipping {}: none of {} is installed", mixinClassName, Arrays.toString(modIds));
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    /** 这个 mixin 要哪些模组在场：装任意一个就应用，null 表示不设门槛。 */
    private static String[] modIdsFor(String mixinClassName) {
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        if (simpleName.startsWith("SophisticatedInventoryInteractions")) {
            return new String[] { SOPHISTICATED_INTERACTIONS };
        }
        if (simpleName.startsWith("Sophisticated")) {
            return new String[] { SOPHISTICATED };
        }
        if (simpleName.startsWith("MekanicalCreate")) {
            return new String[] { MEKANICAL_CREATE };
        }
        if (simpleName.startsWith("Mekanism")) {
            return new String[] { MEKANISM };
        }
        if (simpleName.startsWith("Ae2OversizeInterface")) {
            return new String[] { EXTENDED_AE, EXTENDED_AE_LEGACY };
        }
        if (simpleName.startsWith("Ae2")) {
            return new String[] { AE2 };
        }
        if (simpleName.startsWith("IronFurnaces")) {
            return new String[] { IRON_FURNACES };
        }
        if (simpleName.startsWith("Cucumber")) {
            return new String[] { CUCUMBER };
        }
        if (simpleName.startsWith("FunctionalStorage")) {
            return new String[] { FUNCTIONAL_STORAGE };
        }
        if (simpleName.startsWith("FastWorkbench")) {
            return new String[] { FAST_WORKBENCH };
        }
        if (simpleName.startsWith("SuperbWarfare")) {
            return new String[] { SUPERB_WARFARE };
        }
        if (simpleName.startsWith("CraftingTweaks")) {
            return new String[] { CRAFTING_TWEAKS };
        }
        if (simpleName.startsWith("ImmersiveEngineering")) {
            return new String[] { IMMERSIVE_ENGINEERING };
        }
        if (simpleName.startsWith("RefinedStorage")) {
            return new String[] { REFINED_STORAGE };
        }
        return null;
    }

    /** 查模组在不在。类转换阶段 ModList 还没建好，只能读 LoadingModList。
    /** 模组装了没有。 */
    private static boolean isLoaded(String modId) {
        LoadingModList loadingModList = LoadingModList.get();
        return loadingModList != null && loadingModList.getMods().stream()
                .anyMatch(modInfo -> modId.equals(modInfo.getModId()));
    }
}
