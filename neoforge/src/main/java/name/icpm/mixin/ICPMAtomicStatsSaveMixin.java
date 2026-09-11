package name.icpm.mixin;

import net.minecraft.stats.ServerStatsCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * ServerStatsCounter（玩家统计 world/stats/<uuid>.json）存档原子化。
 *
 * 与 PlayerAdvancements 完全同源的问题（见 ICPMAtomicAdvancementSaveMixin 注释）：
 * vanilla save() 用 Files.newBufferedWriter(主文件) 直接覆盖 → 崩溃/强杀留下 0 字节空文件 →
 * 下次 load() parse 失败 → 玩家统计（行走/击杀/挖掘等，即用户所称"其他数据"）被整份清零。
 *
 * 修复与 advancement 相同：写 .tmp → 原子 move 覆盖；load 前清理历史 0 字节损坏文件。
 */
@Mixin(ServerStatsCounter.class)
public abstract class ICPMAtomicStatsSaveMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("ICPM-StatsSave");

    @Shadow
    @Final
    private Path file;

    /** save(): 写 .tmp 而非直接覆盖主文件。 */
    @Redirect(method = "save", at = @At(value = "INVOKE",
            target = "Ljava/nio/file/Files;newBufferedWriter(Ljava/nio/file/Path;Ljava/nio/charset/Charset;[Ljava/nio/file/OpenOption;)Ljava/io/BufferedWriter;"))
    private static BufferedWriter icpm$writeToTmp(Path path, Charset charset, OpenOption[] options) throws IOException {
        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        return Files.newBufferedWriter(tmp, charset, options);
    }

    /** save(): 正常写完 .tmp 后原子替换主文件。 */
    @Inject(method = "save", at = @At("TAIL"))
    private void icpm$commitTmpToReal(CallbackInfo ci) {
        Path tmp = this.file.resolveSibling(this.file.getFileName().toString() + ".tmp");
        try {
            Files.move(tmp, this.file,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException atomicNotSupported) {
            try {
                Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.error("[ICPM] stats save commit (fallback) failed for {}", this.file, e);
            }
        } catch (IOException e) {
            LOGGER.error("[ICPM] stats save commit failed for {}", this.file, e);
        }
    }

    /** 构造器（读档路径）末尾：删除历史遗留 0 字节损坏统计文件（此时 file 字段已赋值）。 */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void icpm$purgeZeroByteCorruptFile(CallbackInfo ci) {
        try {
            if (Files.isRegularFile(this.file) && Files.size(this.file) == 0L) {
                Files.delete(this.file);
                LOGGER.warn("[ICPM] purged 0-byte corrupt stats file: {}", this.file);
            }
        } catch (IOException e) {
            // 只读检查失败则跳过
        }
    }
}
