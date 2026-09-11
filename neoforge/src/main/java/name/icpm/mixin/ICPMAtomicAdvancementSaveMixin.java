package name.icpm.mixin;

import net.minecraft.server.PlayerAdvancements;
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
 * PlayerAdvancements 存档原子化（根治"每次重进成就丢失"）。
 *
 * 问题根因：vanilla PlayerAdvancements.save() 用 Files.newBufferedWriter(主文件) 直接覆盖写——
 * 打开瞬间即把现有文件截断为 0 字节，之后才写内容。若进程在写入途中崩溃/被强杀（历史上有
 * 多次 client/server crash，用户随后强杀卡住的游戏进程），磁盘上留下 0 字节空文件；
 * 下次启动 load() 读空文件 → MalformedJsonException("line 1 column 1") → 整份成就状态被丢弃
 * （PlayerAdvancements 保持空）→ 进游戏 ICPM 成就触发器重新 grant → 表现为"每次重进成就丢失"。
 * 对比：playerdata 由 PlayerDataStorage 走 rename 备份（.dat_old），崩溃从不损坏主文件，
 * 这正是背包不丢而成就丢的原因。
 *
 * 修复：
 * 1. save() 的 newBufferedWriter 重定向到 <name>.json.tmp —— 崩溃只破坏 .tmp，
 *    主文件始终保持上一次完整状态；
 * 2. save() 正常返回后把 .tmp 原子 move 覆盖主文件；
 * 3. load() 前清理历史遗留的 0 字节损坏文件（删除后 vanilla 按"无玩家成就档案"静默处理，
 *    不再报 parse error / 不再整份清零可恢复的历史——历史在旧崩溃中已丢的部分无法挽回，
 *    但自此之后崩溃不再造成任何成就回退）。
 */
@Mixin(PlayerAdvancements.class)
public abstract class ICPMAtomicAdvancementSaveMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("ICPM-AdvancementSave");

    @Shadow
    @Final
    private Path playerSavePath;

    /** save(): 把目标文件从主路径改到同目录 .tmp，避免打开即截断主文件。 */
    @Redirect(method = "save", at = @At(value = "INVOKE",
            target = "Ljava/nio/file/Files;newBufferedWriter(Ljava/nio/file/Path;Ljava/nio/charset/Charset;[Ljava/nio/file/OpenOption;)Ljava/io/BufferedWriter;"))
    private static BufferedWriter icpm$writeToTmp(Path path, Charset charset, OpenOption[] options) throws IOException {
        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        return Files.newBufferedWriter(tmp, charset, options);
    }

    /** save(): 正常写完 .tmp 后，原子替换主文件（优先 ATOMIC_MOVE，不支持则普通 REPLACE）。 */
    @Inject(method = "save", at = @At("TAIL"))
    private void icpm$commitTmpToReal(CallbackInfo ci) {
        Path tmp = this.playerSavePath.resolveSibling(this.playerSavePath.getFileName().toString() + ".tmp");
        try {
            Files.move(tmp, this.playerSavePath,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException atomicNotSupported) {
            try {
                Files.move(tmp, this.playerSavePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.error("[ICPM] advancement save commit (fallback) failed for {}", this.playerSavePath, e);
            }
        } catch (IOException e) {
            LOGGER.error("[ICPM] advancement save commit failed for {}", this.playerSavePath, e);
        }
    }

    /** load(): 删除历史遗留的 0 字节损坏档案（崩溃截断产物），消除 parse 报错与成就整份清零。 */
    @Inject(method = "load", at = @At("HEAD"))
    private void icpm$purgeZeroByteCorruptFile(CallbackInfo ci) {
        try {
            if (Files.isRegularFile(this.playerSavePath) && Files.size(this.playerSavePath) == 0L) {
                Files.delete(this.playerSavePath);
                LOGGER.warn("[ICPM] purged 0-byte corrupt advancement file: {}", this.playerSavePath);
            }
        } catch (IOException e) {
            // 只读检查失败则跳过，交给 vanilla 原逻辑
        }
    }
}
