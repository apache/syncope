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
package org.syncope.core.connid;

import org.identityconnectors.common.logging.Log.Level;
import org.identityconnectors.common.logging.LogSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4J logger for IdentityConnectors framework.
 */
public class SLF4JLogger implements LogSpi {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SLF4JLogger.class);

    @Override
    public final void log(final Class<?> clazz,
            final String method,
            final Level level,
            final String message,
            final Throwable ex) {

        final StringBuilder logMessage = new StringBuilder();
        logMessage.append(clazz.getName());
        logMessage.append('.');
        logMessage.append(method);
        logMessage.append(' ');
        logMessage.append(message);

        switch (level) {
            case ERROR:
                if (ex != null) {
                    LOG.error(logMessage.toString(), ex);
                } else {
                    LOG.error(logMessage.toString());
                }
                break;

            case INFO:
                if (ex != null) {
                    LOG.info(logMessage.toString(), ex);
                } else {
                    LOG.info(logMessage.toString());
                }
                break;

            case OK:
                if (ex != null) {
                    LOG.debug(logMessage.toString(), ex);
                } else {
                    LOG.debug(logMessage.toString());
                }
                break;

            case WARN:
                if (ex != null) {
                    LOG.warn(logMessage.toString(), ex);
                } else {
                    LOG.warn(logMessage.toString());
                }
                break;

            default:
        }
    }

    @Override
    public final boolean isLoggable(final Class<?> clazz,
            final Level level) {

        boolean loggable;

        switch (level) {
            case ERROR:
                loggable = LOG.isErrorEnabled();
                break;

            case INFO:
                loggable = LOG.isInfoEnabled();
                break;

            case OK:
                loggable = LOG.isDebugEnabled();
                break;

            case WARN:
                loggable = LOG.isWarnEnabled();
                break;

            default:
                loggable = false;
        }

        return loggable;
    }
}
