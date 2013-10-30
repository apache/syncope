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
package org.apache.syncope.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.AbstractWrappable;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.types.LoggerLevel;

public final class CollectionWrapper {

    private CollectionWrapper() {
        // empty constructor for static utility class
    }

    public static <T extends AbstractWrappable> List<T> wrap(
            final String element, final Class<T> reference) {

        return Collections.singletonList(AbstractWrappable.getInstance(reference, element));
    }

    public static <T extends AbstractWrappable> List<T> wrap(
            final Collection<String> collection, final Class<T> reference) {

        List<T> response = new ArrayList<T>();
        for (String element : collection) {
            response.add(AbstractWrappable.getInstance(reference, element));
        }
        return response;
    }

    public static <T extends AbstractWrappable> List<String> unwrap(final Collection<T> collection) {
        List<String> response = new ArrayList<String>();
        for (T e : collection) {
            response.add(e.getName());
        }
        return response;
    }

    public static List<AuditLoggerName> wrapLogger(final Collection<LoggerTO> logger) {
        List<AuditLoggerName> respons = new ArrayList<AuditLoggerName>();
        for (LoggerTO l : logger) {
            try {
                respons.add(AuditLoggerName.fromLoggerName(l.getName()));
            } catch (Exception e) {
                //TODO log event
            }
        }
        return respons;
    }

    public static List<LoggerTO> unwrapLogger(final Collection<AuditLoggerName> auditNames) {
        List<LoggerTO> respons = new ArrayList<LoggerTO>();
        for (AuditLoggerName l : auditNames) {
            LoggerTO loggerTO = new LoggerTO();
            loggerTO.setName(l.toLoggerName());
            loggerTO.setLevel(LoggerLevel.DEBUG);
            respons.add(loggerTO);
        }
        return respons;
    }
}
