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
package org.apache.syncope.types;

import ch.qos.logback.classic.Level;

public enum SyncopeLoggerLevel {

    OFF(Level.OFF),
    ERROR(Level.ERROR),
    WARN(Level.WARN),
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    TRACE(Level.TRACE),
    ALL(Level.ALL);

    private Level level;

    SyncopeLoggerLevel(final Level level) {
        this.level = level;
    }

    public Level getLevel() {
        return level;
    }

    public static SyncopeLoggerLevel fromLevel(final Level level) {
        SyncopeLoggerLevel result;
        if (level.equals(Level.OFF)) {
            result = OFF;
        } else if (level.equals(Level.ERROR)) {
            result = ERROR;
        } else if (level.equals(Level.WARN)) {
            result = WARN;
        } else if (level.equals(Level.INFO)) {
            result = INFO;
        } else if (level.equals(Level.DEBUG)) {
            result = DEBUG;
        } else if (level.equals(Level.TRACE)) {
            result = TRACE;
        } else if (level.equals(Level.ALL)) {
            result = ALL;
        } else {
            throw new IllegalArgumentException("Undefined Level " + level);
        }

        return result;
    }
}
