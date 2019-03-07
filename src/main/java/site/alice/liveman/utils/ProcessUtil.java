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
import site.alice.liveman.model.ServerInfo;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.sun.jna.platform.win32.WinNT.*;

@Slf4j
public class ProcessUtil {

    private static final Map<Long, Process> processTargetMap = new ConcurrentHashMap<>();

    public static long createProcess(String... args) {
        return createProcess(args, null);
    }

    public static long createProcess(String cmdLine, String videoId) {
        String[] args = cmdLine.split("\t");
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("\"") && args[i].endsWith("\"")) {
                args[i] = args[i].substring(1, args[i].length() - 1);
            }
        }
        return createProcess(args, videoId);
    }

    public static long createProcess(String[] args, String videoId) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(args);
            if (videoId != null) {
                settingStdLog(videoId, processBuilder);
            }
            log.info("create process..." + processBuilder.command());
            Process process = processBuilder.start();
            long processHandle = getProcessHandle(process);
            processTargetMap.put(processHandle, process);
            return processHandle;
        } catch (IOException e) {
            log.error("createProcess failed", e);
            return 0;
        }
    }

    public static long createRemoteProcess(String cmdLine, ServerInfo remoteServer, boolean terminalMode, String videoId) {
        try {
            cmdLine = cmdLine.replaceAll("\t", " ");
            ProcessBuilder processBuilder = createRemoteProcessBuilder(remoteServer, cmdLine, terminalMode);
            if (videoId != null) {
                settingStdLog(videoId, processBuilder);
            }
            log.info("create process..." + processBuilder.command());
            Process process = processBuilder.start();
            long processHandle = getProcessHandle(process);
            if (!terminalMode) {
                process = new RemoteProcess(process, remoteServer, cmdLine);
            }
            processTargetMap.put(processHandle, process);
            return processHandle;
        } catch (IOException e) {
            log.error("createProcess failed", e);
            return 0;
        }
    }

    private static void settingStdLog(String logName, ProcessBuilder processBuilder) {
        File logFile = new File("logs/ffmpeg/" + logName + "/" + (System.currentTimeMillis() / 100000) + ".log");
        logFile.getParentFile().mkdirs();
        processBuilder.redirectOutput(logFile);
        processBuilder.redirectError(logFile);
    }

    public static boolean isProcessExist(long pid) {
        Process process = processTargetMap.get(pid);
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
        Process process = processTargetMap.get(pid);
        if (process != null) {
            process.destroy();
            waitProcess(pid);
        }
    }

    public static void waitProcess(long pid) {
        Process process = processTargetMap.get(pid);
        if (process != null) {
            try {
                process.waitFor();
                processTargetMap.remove(pid);
            } catch (InterruptedException ignore) {

            }
        }
    }

    public static Process getProcess(long pid) {
        return processTargetMap.get(pid);
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
        Process process = processTargetMap.get(pid);
        if (process != null) {
            try {
                boolean result = process.waitFor(dwMilliseconds, TimeUnit.MILLISECONDS);
                if (result) {
                    processTargetMap.remove(pid);
                }
                return result;
            } catch (InterruptedException ignore) {
                return false;
            }
        } else {
            return true;
        }
    }

    private static class RemoteProcess extends Process {

        private String     killRemoteProcessCmdLine = "ps -ef | grep -F \"%s\" | grep -v grep | awk -F ' ' '{print $2}'| xargs kill -9";
        private Process    sshProcess;
        private ServerInfo remoteServer;
        private String     cmdLine;

        public RemoteProcess(Process sshProcess, ServerInfo remoteServer, String cmdLine) {
            this.sshProcess = sshProcess;
            this.remoteServer = remoteServer;
            this.cmdLine = cmdLine;
        }

        public Process getSshProcess() {
            return sshProcess;
        }

        public void setSshProcess(Process sshProcess) {
            this.sshProcess = sshProcess;
        }

        public ServerInfo getRemoteServer() {
            return remoteServer;
        }

        public void setRemoteServer(ServerInfo remoteServer) {
            this.remoteServer = remoteServer;
        }

        public String getCmdLine() {
            return cmdLine;
        }

        public void setCmdLine(String cmdLine) {
            this.cmdLine = cmdLine;
        }

        @Override
        public OutputStream getOutputStream() {
            return sshProcess.getOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return sshProcess.getInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return sshProcess.getErrorStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            return sshProcess.waitFor();
        }

        @Override
        public int exitValue() {
            return sshProcess.exitValue();
        }

        @Override
        public void destroy() {
            ProcessBuilder processBuilder = createRemoteProcessBuilder(remoteServer, String.format(killRemoteProcessCmdLine, cmdLine.replaceAll("\"", "")), true);
            try {
                Process process = processBuilder.start();
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                log.error("destroy remote process failed\n" + processBuilder.command(), e);
            }
            sshProcess.destroy();
        }
    }

    private static ProcessBuilder createRemoteProcessBuilder(ServerInfo remoteServer, String cmdLine, boolean terminalMode) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("sshpass", "-p", remoteServer.getPassword(), "ssh", "-o", "StrictHostKeyChecking=no"));
        if (terminalMode) {
            args.add("-tt");
        }
        args.addAll(Arrays.asList("-p", String.valueOf(remoteServer.getPort()), remoteServer.getUsername() + "@" + remoteServer.getAddress(), cmdLine));
        processBuilder.command(args);
        return processBuilder;
    }
}
