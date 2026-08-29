package name.icpm;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * 卡死探测器（覆盖渲染线程与服务端线程）。
 *
 * 背景：玩家反馈「保存世界时游戏窗口卡死，但不产生 crash-xxxx.txt」。
 *
 * 重要修正（2026-08-17）：
 *   早期版本把「服务端线程栈签名连续多秒不变」直接判为卡死，但健康服务端在两次 tick 之间
 *   几乎 100% 时间停在 waitUntilNextTick（TIMED_WAITING/parked），栈签名永远稳定 ——
 *   这会产生海量假阳（每次保存都在刷屏，且完全掩盖真问题）。
 *   正确判据：
 *     - 服务端只有处于 RUNNABLE（真正在干活：保存/计算/死循环）且栈稳定时才算卡死；
 *       parked（WAITING/TIMED_WAITING/BLOCKED）一律视为正常，复位计数。
 *     - 渲染（主）线程：超过阈值未标记新的一帧（即客户端主循环整体停滞）才算卡死。
 *   另外 dump 去重：仅在「冻结点发生变化」时重新 dump，避免刷屏。
 *
 *   专用服务端（dedicated server）修正（2026-08-25）：
 *     服务端环境没有渲染线程，markClientTick 永不被调用，lastClientTickMs 停在启动时刻，
 *     渲染停滞值会无限增长 → 每帧都判为「渲染线程卡死」并逐秒 dump 全线程栈（刷屏）。
 *     因此在 EnvironmentType.SERVER 下直接跳过渲染停滞检查，仅保留服务端线程 RUNNABLE 卡死检查。
 */
public final class FreezeDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger("ICPM-FreezeDetector");

    /** 渲染线程停滞阈值（ms）。 */
    private static final long CLIENT_THRESHOLD_MS = 8_000L;
    /** 服务端线程真正卡死（RUNNABLE 且栈稳定）阈值（ms）。 */
    private static final long SERVER_THRESHOLD_MS = 10_000L;
    /** 服务端线程名（Minecraft 集成服务端专用线程）。 */
    private static final String SERVER_THREAD_NAME = "Server thread";

    private static volatile long lastClientTickMs = System.currentTimeMillis();
    private static volatile boolean started = false;
    /** 是否专用服务端（无渲染线程，markClientTick 永不被调用）。 */
    private static boolean dedicatedServer = false;
    /** 上一次 dump 的冻结签名，用于去重。 */
    private static String lastDumpSig = "";

    // 服务端线程栈稳定性跟踪（仅 RUNNABLE 时累计）
    private static String lastServerSig = "";
    private static int serverStuckSeconds = 0;

    private FreezeDetector() {
    }

    /** 由客户端每帧调用（渲染线程存活）。 */
    public static void markClientTick() {
        lastClientTickMs = System.currentTimeMillis();
    }

    /** 幂等启动监控守护线程。 */
    public static synchronized void ensureStarted() {
        if (started) {
            return;
        }
        started = true;
        dedicatedServer = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
        Thread t = new Thread(FreezeDetector::monitor, "ICPM-FreezeDetector");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        LOGGER.info("ICPM 卡死探测器已启动（渲染阈值 {}ms / 服务端 RUNNABLE 阈值 {}ms）",
                CLIENT_THRESHOLD_MS, SERVER_THRESHOLD_MS);
    }

    private static void monitor() {
        while (true) {
            try {
                Thread.sleep(1_000L);
            } catch (InterruptedException e) {
                return;
            }
            long now = System.currentTimeMillis();
            long clientStalled = now - lastClientTickMs;
            ServerStuck server = checkServerStuck();

            // 专用服务端没有渲染线程，markClientTick 永不被调用，渲染停滞检查必为假阳 → 直接跳过。
            boolean clientFreeze = !dedicatedServer && clientStalled > CLIENT_THRESHOLD_MS;
            boolean serverFreeze = server.stuckMs >= SERVER_THRESHOLD_MS;
            if (clientFreeze || serverFreeze) {
                String sig = buildFreezeSig(clientFreeze, clientStalled, server);
                // 仅在冻结点变化（或首次）时 dump，避免刷屏
                if (!sig.equals(lastDumpSig)) {
                    lastDumpSig = sig;
                    dumpThreads(clientStalled, server.stuckMs, server.topFrame);
                }
            } else {
                // 已恢复，复位去重
                lastDumpSig = "";
            }
        }
    }

    private static String buildFreezeSig(boolean clientFreeze, long clientStalled, ServerStuck server) {
        StringBuilder sb = new StringBuilder();
        if (clientFreeze) {
            // 按 5s 分桶：真卡死时每 5s 仅 dump 一次，避免逐秒刷屏
            sb.append("C").append(clientStalled / 5_000L).append('|');
        }
        if (server.stuckMs >= SERVER_THRESHOLD_MS) {
            sb.append("S:").append(server.topFrame).append('|');
        }
        return sb.toString();
    }

    /**
     * 检查服务端线程是否真正卡死。
     * 仅当服务端线程状态为 RUNNABLE（正在执行代码）且栈签名连续稳定时才累计；
     * 若处于 WAITING/TIMED_WAITING/BLOCKED（挂起/等待锁，属正常空闲）则复位计数。
     */
    private static ServerStuck checkServerStuck() {
        ServerStuck result = new ServerStuck();
        try {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            long[] ids = bean.getAllThreadIds();
            ThreadInfo[] infos = bean.getThreadInfo(ids, 30);
            for (ThreadInfo info : infos) {
                if (info == null) {
                    continue;
                }
                if (SERVER_THREAD_NAME.equals(info.getThreadName())) {
                    if (info.getThreadState() != Thread.State.RUNNABLE) {
                        // 挂起/等待：健康状态，复位
                        serverStuckSeconds = 0;
                        lastServerSig = "";
                        return result;
                    }
                    String sig = stackSignature(info);
                    if (sig.equals(lastServerSig)) {
                        serverStuckSeconds++;
                    } else {
                        serverStuckSeconds = 0;
                        lastServerSig = sig;
                    }
                    result.stuckMs = serverStuckSeconds * 1000L;
                    StackTraceElement[] st = info.getStackTrace();
                    if (st != null && st.length > 0) {
                        result.topFrame = st[0].toString();
                    }
                    return result;
                }
            }
        } catch (Throwable t) {
            // 忽略监控自身异常
        }
        // 服务端线程未找到（已正常退出）→ 复位
        serverStuckSeconds = 0;
        lastServerSig = "";
        return result;
    }

    private static String stackSignature(ThreadInfo info) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement ste : info.getStackTrace()) {
            sb.append(ste.getClassName()).append('.').append(ste.getMethodName())
              .append('@').append(ste.getLineNumber()).append(';');
            if (sb.length() > 600) {
                break;
            }
        }
        return sb.toString();
    }

    private static void dumpThreads(long clientStalled, long serverStuckMs, String serverTopFrame) {
        try {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] infos;
            try {
                if (bean.isObjectMonitorUsageSupported() && bean.isSynchronizerUsageSupported()) {
                    infos = bean.dumpAllThreads(true, true);
                } else {
                    infos = bean.dumpAllThreads(false, false);
                }
            } catch (Throwable t) {
                infos = bean.getThreadInfo(bean.getAllThreadIds(), 200);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\n########## ICPM FREEZE DETECTED ##########\n")
              .append("## 渲染线程停滞: ").append(clientStalled).append("ms | 服务端 RUNNABLE 卡死: ")
              .append(serverStuckMs).append("ms")
              .append(serverTopFrame != null ? " (top=" + serverTopFrame + ")" : "").append(" ##\n")
              .append("## 以下为全部线程栈，定位卡死点（重点关注 \"Server thread\" 或 \"Render thread\"）##\n");
            for (ThreadInfo info : infos) {
                if (info == null) {
                    continue;
                }
                sb.append("Thread ").append(info.getThreadId()).append(" \"")
                  .append(info.getThreadName()).append("\" state=").append(info.getThreadState());
                if (info.getLockName() != null) {
                    sb.append(" lock=").append(info.getLockName());
                }
                if (info.getLockOwnerName() != null) {
                    sb.append(" owner=").append(info.getLockOwnerName());
                }
                sb.append('\n');
                for (StackTraceElement ste : info.getStackTrace()) {
                    sb.append("    at ").append(ste).append('\n');
                }
                sb.append('\n');
            }
            LOGGER.error(sb.toString());
        } catch (Throwable t) {
            LOGGER.error("ICPM 卡死探测器自身异常", t);
        }
    }

    /** 服务端卡死检查结果。 */
    private static final class ServerStuck {
        long stuckMs = 0L;
        String topFrame = null;
    }
}
