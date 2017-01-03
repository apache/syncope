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
package org.apache.syncope.core.logic;

import java.util.Queue;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.syncope.common.lib.log.LogStatementTO;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.core.provisioning.api.utils.ExceptionUtils2;

@Plugin(name = "Memory", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class MemoryAppender extends AbstractAppender {

    private final CircularFifoQueue<LogStatementTO> statements;

    protected MemoryAppender(
            final String name,
            final int size,
            final Filter filter,
            final boolean ignoreExceptions) {

        super(name, filter, null, ignoreExceptions);
        this.statements = new CircularFifoQueue<>(size);
    }

    @Override
    public void append(final LogEvent event) {
        LogStatementTO statement = new LogStatementTO();

        statement.setLevel(LoggerLevel.fromLevel(event.getLevel()));
        statement.setLoggerName(event.getLoggerName());

        Message msg = event.getMessage();
        statement.setMessage((msg instanceof ReusableMessage
                ? ((ReusableMessage) msg).memento()
                : msg).getFormattedMessage());

        statement.setTimeMillis(event.getTimeMillis());

        if (event.getThrown() != null) {
            statement.setStackTrace(ExceptionUtils2.getFullStackTrace(event.getThrown()));
        }

        statement.setThreadId(event.getThreadId());
        statement.setThreadName(event.getThreadName());
        statement.setThreadPriority(event.getThreadPriority());

        this.statements.add(statement);
    }

    public Queue<LogStatementTO> getStatements() {
        return statements;
    }

    @PluginFactory
    public static MemoryAppender createAppender(
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "size", defaultInt = 10) final int size,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {

        return new MemoryAppender(
                name,
                size,
                filter,
                ignoreExceptions);
    }

}
