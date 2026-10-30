package com.zaixiayesheng.ultrastacks.mixin.compat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * 兼容 mixin 的开关：目标模组没装就跳过这一条，别让补丁在别人机器上炸掉启动。
 *
 * <p>Mixin 找不到目标类时只打一条 WARN；真正会崩的是「模组装了、但方法改名或签名变了」。所以这里先按
 * modId 过滤，包内每个注入也都写了 {@code require = 0}，最坏结果都只是兼容静默失效。
 *
 * <p>Gates the compatibility mixins: a mixin is skipped when its target mod is not installed, so a patch
 * never breaks startup on someone else's machine. A missing target class only logs a warning; what would
 * really fail is a present mod whose method was renamed or resignatured. The filtering by modId here plus
 * {@code require = 0} on every injector in this package keeps the worst case at "the patch silently does
 * nothing".
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
    private static final String FAST_WORKBENCH = "fastbench";
    private static final String FUNCTIONAL_STORAGE = "functionalstorage";
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
     * 这条 mixin 需要哪个模组在场；返回 {@code null} 表示不设门槛。
     *
     * <p>判断依据是类名的前缀，所以新加兼容补丁时类名要跟这里的分支对上。ExtendedAE 的 modId 有两个取值
     * （{@code expatternprovider} 与 {@code extendedae}），命中任意一个都算装了。
     *
     * <p>Which mod a mixin needs; {@code null} means no gate. The decision is made on the class name prefix,
     * so a new compat patch has to match one of the branches here. ExtendedAE has been published under two
     * modIds, {@code expatternprovider} and {@code extendedae}, and either one counts as installed.
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
        if (simpleName.startsWith("FastWorkbench")) {
            return new String[] { FAST_WORKBENCH };
        }
        if (simpleName.startsWith("FunctionalStorage")) {
            return new String[] { FUNCTIONAL_STORAGE };
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
     * 在类还在被转换的阶段查模组是否装好。
     *
     * <p>此时 {@code ModList} 还没建起来，能用的只有加载期的 {@code LoadingModList}，所以这里读的是它。
     *
     * <p>Checks whether a mod is installed while classes are still being transformed. {@code ModList} does not
     * exist yet at that point, so the loading-time {@code LoadingModList} is the only source available here.
     */
    private static boolean isLoaded(String modId) {
        LoadingModList loadingModList = LoadingModList.get();
        return loadingModList != null && loadingModList.getMods().stream()
                .anyMatch(modInfo -> modId.equals(modInfo.getModId()));
    }
}
