/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.msc.value.util;

/**
 * Class for test purposes.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class AnyService {

    public enum TaskType {MAIN, SECONDARY, NON_IMPORTANT, PRIVATE_DAEMON, PRIVATE};

    public static boolean disableAll;
    public byte[] allCounts;
    public int count;
    public byte byteCount;
    public long longCount;
    public short shortCount;
    public double doubleStatus;
    public float floatStatus;
    public char charStatus;

    public String description;
    @SuppressWarnings("unused")
    private int sum;
    private TaskType taskType = null;

    public void executeMainTask() {
        taskType = TaskType.MAIN;
    }

    public void executeSecondaryTask(String taskName) {
        taskType = TaskType.SECONDARY;
    }

    public void executeSecondaryTask(boolean notifyAll) {
        taskType = TaskType.SECONDARY;
    }

    public void executeSecondaryTask(int taskNumber, boolean notifyAll) {
        taskType = TaskType.SECONDARY;
    }

    public void executeNonImportantTask(String taskName) {
        taskType = TaskType.NON_IMPORTANT;
    }

    @SuppressWarnings("unused")
    private void executeTask(boolean daemon) {
        taskType = daemon? TaskType.PRIVATE_DAEMON: TaskType.PRIVATE;
    }

    public TaskType getExecutedTask() {
        return taskType;
    }
}