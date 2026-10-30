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
 * 兼容补丁的开关：目标模组不在场就不应用对应的 mixin，免得在别人的整合包里因为找不到类而起不来。
 *
 * <p>兼容配置声明的是 {@code required = true}，{@code @Mixin(targets = ...)} 找不到目标类会直接抛异常，所以必须先按 modId 挡掉没装的模组。目标装了但方法被改名的情况，靠注入点上的 {@code require = 0} 静默跳过——本包里多数注入点都写了。
 *
 * <p>This plugin decides whether a compat mixin applies: when the target mod is absent the mixin is
 * skipped, so a modpack without that mod does not fail on a missing class. The compat config is
 * declared {@code required = true}, and {@code @Mixin(targets = ...)} throws when the target class is
 * missing, so the modId check has to run first. A mod that is installed but has renamed a method is
 * covered by {@code require = 0} on the injector, which most injectors in this package declare.
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
    private static final String IRON_FURNACES = "ironfurnaces";
    private static final String CUCUMBER = "cucumber";
    private static final String FUNCTIONAL_STORAGE = "functionalstorage";
    private static final String FAST_WORKBENCH = "fastbench";
    private static final String IMMERSIVE_ENGINEERING = "immersiveengineering";

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

    /**
     * 这个 mixin 需要哪些模组在场：任意一个装上就应用。
     *
     * <p>{@code null} 表示不设门槛，目前包里每个类都能匹配到一条规则，{@code null} 只是兜底。ExtendedAE 换过 modId，旧的 {@code expatternprovider} 与新的 {@code extendedae} 并列，装哪个版本都算命中。
     *
     * <p>Which mods this mixin needs: it applies when any one of them is installed.
     *
     * <p>A {@code null} result means no gate; every class in this package currently matches one rule, so
     * that branch is only a fallback. ExtendedAE changed its modId, so the old {@code expatternprovider}
     * and the newer {@code extendedae} are listed together and either version matches.
     */
    private static String[] modIdsFor(String mixinClassName) {
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
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
        if (simpleName.startsWith("ImmersiveEngineering")) {
            return new String[] { IMMERSIVE_ENGINEERING };
        }
        if (simpleName.startsWith("RefinedStorage")) {
            return new String[] { REFINED_STORAGE };
        }
        return null;
    }

    /**
     * 查加载期的模组列表。
     *
     * <p>类转换阶段 {@code ModList} 还没建好，只能读 {@link LoadingModList}；拿不到列表时按「没装」处理。
     *
     * <p>Looks a mod up in the loading-time mod list.
     *
     * <p>{@code ModList} does not exist yet while classes are being transformed, so {@link LoadingModList}
     * is the only available source. A missing list counts as not installed.
     */
    private static boolean isLoaded(String modId) {
        LoadingModList loadingModList = LoadingModList.get();
        return loadingModList != null && loadingModList.getMods().stream()
                .anyMatch(modInfo -> modId.equals(modInfo.getModId()));
    }
}
