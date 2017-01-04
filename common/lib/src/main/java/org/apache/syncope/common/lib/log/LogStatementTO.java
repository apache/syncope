/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.lib.log;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.LoggerLevel;

@XmlRootElement(name = "logStatement")
@XmlType
public class LogStatementTO extends AbstractBaseBean {

    private static final long serialVersionUID = -2931205859104653385L;

    private LoggerLevel level;

    private String loggerName;

    private String message;

    private String stackTrace;

    private long timeMillis;

    private long threadId;

    private String threadName;

    private int threadPriority;

    public LoggerLevel getLevel() {
        return level;
    }

    public void setLevel(final LoggerLevel level) {
        this.level = level;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(final String loggerName) {
        this.loggerName = loggerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(final String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public void setTimeMillis(final long timeMillis) {
        this.timeMillis = timeMillis;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(final long threadId) {
        this.threadId = threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(final String threadName) {
        this.threadName = threadName;
    }

    public int getThreadPriority() {
        return threadPriority;
    }

    public void setThreadPriority(final int threadPriority) {
        this.threadPriority = threadPriority;
    }

}
