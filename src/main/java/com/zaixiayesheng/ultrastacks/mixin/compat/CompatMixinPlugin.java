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
 * 兼容 mixin 的总开关：目标模组没装就跳过对应的补丁。
 *
 * <p>目标类缺失时 Mixin 只打一条 WARN，真正会崩的是「模组装了、但方法改名或改签名」：注入点找不到又要求
 * 必须成功，就会直接抛异常终止启动。所以这里先按 modId 过滤，包内每个注入点也都写了 {@code require = 0}，
 * 两种情况下最坏结果都只是「兼容静默失效」，游戏照常起来。
 *
 * <p>The overall switch for the compat mixins: a patch is skipped when its target mod is not installed.
 *
 * <p>A missing target class only makes Mixin log a warning, while what actually breaks startup is a mod that
 * is installed but has renamed or redesigned a method: an injector that cannot find its target and is
 * required to apply throws and aborts the launch. The plugin therefore filters by modId first, and every
 * injector in this package also declares {@code require = 0}, so in either case the worst outcome is a patch
 * that quietly does nothing and a game that still starts.
 */
public final class CompatMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("UltraStacks/Compat");

    private static final String SOPHISTICATED = "sophisticatedcore";
    private static final String AE2 = "ae2";
    private static final String EXTENDED_AE = "extendedae";
    private static final String EXTENDED_AE_LEGACY = "expatternprovider";
    private static final String REFINED_STORAGE = "refinedstorage";
    private static final String IRON_FURNACES = "ironfurnaces";
    private static final String CUCUMBER = "cucumber";

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
     * 这个 mixin 需要哪些 modId 在场，{@code null} 表示不按模组过滤。
     *
     * <p>ExtendedAE 有两个 modId：{@code extendedae} 与旧的 {@code expatternprovider}，任一在场就应用。判断
     * 只看类名前缀，所以按同样前缀命名的兼容 mixin 会自动归到对应的模组上。
     *
     * <p>Which modIds this mixin needs; {@code null} means no mod gate.
     *
     * <p>ExtendedAE has two modIds, {@code extendedae} and the older {@code expatternprovider}, and the mixin
     * applies when either one is present. Only the simple class name prefix is inspected, so a new compat
     * mixin that follows the naming scheme is classified automatically.
     */
    private static String[] modIdsFor(String mixinClassName) {
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        if (simpleName.startsWith("Sophisticated")) {
            return new String[] { SOPHISTICATED };
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
        if (simpleName.startsWith("RefinedStorage")) {
            return new String[] { REFINED_STORAGE };
        }
        return null;
    }

    /**
     * 判断某个 modId 是否已加载。
     *
     * <p>这里只能读 {@code LoadingModList}：类转换阶段 {@code ModList} 还没建好，用它拿不到已加载模组，取不
     * 到列表时按「没装」处理。
     *
     * <p>Whether the given modId is loaded.
     *
     * <p>Only {@code LoadingModList} can be read here: {@code ModList} is not built yet while classes are
     * being transformed and cannot report the loaded mods, and a null list counts as "not installed".
     */
    private static boolean isLoaded(String modId) {
        LoadingModList loadingModList = LoadingModList.get();
        return loadingModList != null && loadingModList.getMods().stream()
                .anyMatch(modInfo -> modId.equals(modInfo.getModId()));
    }
}
