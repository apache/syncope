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
package org.apache.syncope.client.cli.commands.logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.client.cli.view.Table;
import org.apache.syncope.common.lib.log.LogStatementTO;
import org.apache.syncope.common.lib.log.LoggerTO;

public class LoggerResultManager extends CommonsResultManager {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void fromListMemoryAppenders(final List<String> memoryAppenders) {
        final Table.TableBuilder tableBuilder = new Table.TableBuilder("list memory appenders").header("appender");
        for (final String appender : memoryAppenders) {
            tableBuilder.rowValues(Collections.singletonList(appender));
        }
        tableBuilder.build().print();
    }

    public void fromGetLastLogStatements(final List<LogStatementTO> statements) throws JsonProcessingException {
        final Table.TableBuilder tableBuilder = new Table.TableBuilder("last statements").header("statement");
        for (final LogStatementTO statement : statements) {
            tableBuilder.rowValues(Collections.singletonList(
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(statement)));
        }
        tableBuilder.build().print();
    }

    public void fromList(final List<LoggerTO> loggerTOs) {
        fromCommandToView("list loggers", "level", loggerTOs);
    }

    public void fromRead(final List<LoggerTO> loggerTOs) {
        fromCommandToView("read loggers", "level", loggerTOs);
    }

    public void fromCreate(final List<LoggerTO> loggerTOs) {
        fromCommandToView("created loggers", "level", loggerTOs);
    }

    public void fromUpdate(final List<LoggerTO> loggerTOs) {
        fromCommandToView("updated loggers", "new level", loggerTOs);
    }

    public void fromDelete(final List<LoggerTO> loggerTOs) {
        fromCommandToView("deleted loggers", "new level", loggerTOs);
    }

    private void fromCommandToView(
            final String title,
            final String secondHeader,
            final List<LoggerTO> loggerTOs) {

        final Table.TableBuilder tableBuilder = new Table.TableBuilder(title).header("logger").header(secondHeader);
        for (final LoggerTO loggerTO : loggerTOs) {
            tableBuilder.rowValues(Arrays.asList(loggerTO.getKey(), loggerTO.getLevel().getLevel().name()));
        }
        tableBuilder.build().print();
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("loggers details", details);
    }
}
