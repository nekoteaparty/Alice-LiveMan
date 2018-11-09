/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package site.alice.liveman.utils;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.ptr.IntByReference;

import java.lang.reflect.Field;

import static com.sun.jna.platform.win32.WinNT.*;

public class ProcessUtil {

    public static long createProcess(String execPath, String cmdLine, boolean isVisible) {
        Kernel32 kernel = Kernel32.INSTANCE;
        PROCESS_INFORMATION process_information = new PROCESS_INFORMATION();
        DWORD dwCreationFlags = new DWORD(isVisible ? CREATE_NEW_CONSOLE : CREATE_NO_WINDOW);
        kernel.CreateProcess(execPath, cmdLine, null, null, false, dwCreationFlags, null, null, new WinBase.STARTUPINFO(), process_information);
        return process_information.dwProcessId.longValue();
    }

    public static boolean isProcessExist(long pid) {
        Kernel32 kernel = Kernel32.INSTANCE;
        IntByReference intByReference = new IntByReference();
        if (kernel.GetExitCodeProcess(getProcessHandle(pid), intByReference)) {
            return intByReference.getValue() == 259;
        }
        return false;
    }

    public static HANDLE getProcessHandle(long pid) {
        Kernel32 kernel = Kernel32.INSTANCE;
        return kernel.OpenProcess(PROCESS_ALL_ACCESS, false, (int) pid);
    }

    public static long getProcessPid(HANDLE pHandle) {
        Kernel32 kernel = Kernel32.INSTANCE;
        return kernel.GetProcessId(pHandle);
    }

    public static long getProcessPid(Process process) {
        try {
            Field handleField = process.getClass().getDeclaredField("handle");
            handleField.setAccessible(true);
            long pHandle = handleField.getLong(process);
            Kernel32 kernel = Kernel32.INSTANCE;
            HANDLE handle = new HANDLE();
            handle.setPointer(Pointer.createConstant(pHandle));
            return kernel.GetProcessId(handle);
        } catch (Throwable e) {
            return 0;
        }
    }

    public static void killProcess(long pid) {
        Kernel32 kernel = Kernel32.INSTANCE;
        HANDLE pHandle = getProcessHandle(pid);
        createProcess(System.getenv("SystemRoot") + "/system32/taskkill.exe", " /F /PID " + pid, false);
        kernel.WaitForSingleObject(pHandle, -1);
    }

    public static void waitProcess(long pid) {
        Kernel32 kernel = Kernel32.INSTANCE;
        HANDLE pHandle = getProcessHandle(pid);
        kernel.WaitForSingleObject(pHandle, -1);
    }

    public static int waitProcess(long pid, int dwMilliseconds) {
        Kernel32 kernel = Kernel32.INSTANCE;
        HANDLE pHandle = getProcessHandle(pid);
        return kernel.WaitForSingleObject(pHandle, dwMilliseconds);
    }
}
