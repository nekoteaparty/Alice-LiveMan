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


import java.util.concurrent.*;

public class ThreadPoolUtil {

    private static final ThreadPoolExecutor          cachedThreadPool    = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private static final ScheduledThreadPoolExecutor scheduledThreadPool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(50);

    public static void execute(Runnable runnable) {
        cachedThreadPool.execute(runnable);
    }

    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduledThreadPool.schedule(command, delay, unit);
    }
}
