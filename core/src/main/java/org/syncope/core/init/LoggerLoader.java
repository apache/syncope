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
package org.syncope.core.init;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SyncopeLogger;
import org.syncope.core.persistence.dao.LoggerDAO;
import org.syncope.types.LoggerLevel;

@Component
public class LoggerLoader {

    @Autowired
    private LoggerDAO loggerDAO;

    @Transactional
    public void load() {
        Map<String, SyncopeLogger> syncopeLoggers =
                new HashMap<String, SyncopeLogger>();
        for (SyncopeLogger syncopeLogger : loggerDAO.findAll()) {
            syncopeLoggers.put(syncopeLogger.getName(), syncopeLogger);
        }

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        /*
         * Traverse all defined Logback loggers: if there is a matching
         * SyncopeLogger, set Logback level accordingly, otherwise create a
         * SyncopeLogger instance with given name and level.
         */
        for (Logger logger : lc.getLoggerList()) {
            if (logger.getLevel() != null) {
                if (syncopeLoggers.containsKey(logger.getName())) {
                    logger.setLevel(syncopeLoggers.get(logger.getName()).
                            getLevel().getLevel());
                    syncopeLoggers.remove(logger.getName());
                } else {
                    SyncopeLogger syncopeLogger = new SyncopeLogger();
                    syncopeLogger.setName(logger.getName());
                    syncopeLogger.setLevel(
                            LoggerLevel.fromLevel(logger.getLevel()));
                    loggerDAO.save(syncopeLogger);
                }
            }
        }

        /*
         * Foreach SyncopeLogger not found in Logback, create a new Logback
         * logger with given name and level.
         */
        for (SyncopeLogger syncopeLogger : syncopeLoggers.values()) {
            Logger logger = lc.getLogger(syncopeLogger.getName());
            logger.setLevel(syncopeLogger.getLevel().getLevel());
        }
    }
}
