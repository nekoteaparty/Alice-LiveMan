/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package site.alice.liveman.utils;

import com.alibaba.fastjson.JSON;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.sun.jna.platform.win32.WinNT.*;

@Slf4j
public class ProcessUtil {

    private static final Map<Long, Object> processTargetMap = new ConcurrentHashMap<>();

    public static long createProcess(String cmdLine, String videoId, boolean isVisible) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String[] args = cmdLine.split("\t");
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("\"") && args[i].endsWith("\"")) {
                    args[i] = args[i].substring(1, args[i].length() - 1);
                }
            }
            log.info(JSON.toJSONString(args));
            processBuilder.command(args);
            if (videoId != null) {
                File logFile = new File("logs/ffmpeg/" + videoId + ".log");
                logFile.getParentFile().mkdirs();
                processBuilder.redirectOutput(logFile);
                processBuilder.redirectError(logFile);
            }
            Process process = processBuilder.start();
            long processHandle = getProcessHandle(process);
            processTargetMap.put(processHandle, process);
            return processHandle;
        } catch (IOException e) {
            log.error("createProcess failed", e);
            return 0;
        }
    }

    public static boolean isProcessExist(long pid) {
        Process process = (Process) processTargetMap.get(pid);
        if (process != null) {
            return process.isAlive();
        }
        return false;
    }

    public static HANDLE getProcessHandle(long pid) {
        Kernel32 kernel = Kernel32.INSTANCE;
        return kernel.OpenProcess(PROCESS_ALL_ACCESS, false, (int) pid);
    }

    public static long getProcessHandle(Process process) {
        try {
            Field handleField;
            if (Platform.isWindows()) {
                handleField = process.getClass().getDeclaredField("handle");
            } else {
                handleField = process.getClass().getDeclaredField("pid");
            }
            handleField.setAccessible(true);
            return handleField.getLong(process);
        } catch (Throwable e) {
            log.error("getProcessHandle failed", e);
            return 0;
        }
    }

    public static void killProcess(long pid) {
        Process process = (Process) processTargetMap.get(pid);
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException ignore) {

            }
        }
    }

    public static void waitProcess(long pid) {
        Process process = (Process) processTargetMap.get(pid);
        if (process != null) {
            try {
                process.waitFor();
            } catch (InterruptedException ignore) {

            }
        }
    }

    /**
     * @param pid
     * @param dwMilliseconds
     * @return 如果在等待期间进程退出返回true，否则返回false
     */
    public static boolean waitProcess(long pid, int dwMilliseconds) {
        if (pid == 0) {
            return true;
        }
        Process process = (Process) processTargetMap.get(pid);
        if (process != null) {
            try {
                return process.waitFor(dwMilliseconds, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignore) {
                return false;
            }
        } else {
            return true;
        }
    }
}
