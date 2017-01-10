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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.wicket.PageReference;

public class ConsoleLogPanel extends AbstractLogsPanel<LoggerTO> {

    private static final long serialVersionUID = -9165749229623482717L;

    private static final ConsoleLoggerController CONSOLE_LOGGER_CONTROLLER = new ConsoleLoggerController();

    public ConsoleLogPanel(final String id, final PageReference pageReference) {
        super(id, pageReference, CONSOLE_LOGGER_CONTROLLER.getLoggers());
    }

    @Override
    protected void update(final LoggerTO loggerTO) {
        CONSOLE_LOGGER_CONTROLLER.setLogLevel(loggerTO.getKey(), loggerTO.getLevel());
    }

    private static class ConsoleLoggerController implements Serializable {

        private static final long serialVersionUID = -1550459341476431714L;

        public List<LoggerTO> getLoggers() {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

            List<LoggerTO> result = new ArrayList<>();
            for (final LoggerConfig logger : ctx.getConfiguration().getLoggers().values()) {
                String loggerName = LogManager.ROOT_LOGGER_NAME.equals(logger.getName())
                        ? SyncopeConstants.ROOT_LOGGER : logger.getName();
                if (logger.getLevel() != null) {
                    LoggerTO loggerTO = new LoggerTO();
                    loggerTO.setKey(loggerName);
                    loggerTO.setLevel(LoggerLevel.fromLevel(logger.getLevel()));
                    result.add(loggerTO);
                }
            }
            Collections.sort(result, ComparatorUtils.transformedComparator(
                    ComparatorUtils.<String>naturalComparator(), new Transformer<LoggerTO, String>() {

                @Override
                public String transform(final LoggerTO input) {
                    return input.getKey();
                }
            }));

            return result;
        }

        public void setLogLevel(final String name, final LoggerLevel level) {
            final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            final LoggerConfig logConf = SyncopeConstants.ROOT_LOGGER.equals(name)
                    ? ctx.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME)
                    : ctx.getConfiguration().getLoggerConfig(name);
            logConf.setLevel(level.getLevel());
            ctx.updateLoggers();
        }
    }
}
