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
package org.apache.syncope.core.starter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;

class ProductionModeCheckerTest {

    private static final String WARNING = "not running in Production Mode";

    private static final class CapturingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private CapturingAppender() {
            super("ProductionModeCheckerTest", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(final LogEvent event) {
            events.add(event.toImmutable());
        }

        private List<LogEvent> getEvents() {
            return events;
        }
    }

    private List<LogEvent> check(final boolean productionMode) {
        ProductionModeChecker checker = new ProductionModeChecker();
        checker.productionMode = productionMode;

        @SuppressWarnings("unchecked")
        Logger logger = (Logger) LogManager.getLogger(ProductionModeChecker.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.WARN);

        CapturingAppender appender = new CapturingAppender();
        appender.start();
        logger.addAppender(appender);
        try {
            checker.check();
        } finally {
            logger.removeAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
        }

        return appender.getEvents();
    }

    @Test
    void doesNotWarnInProductionMode() {
        List<LogEvent> events = check(true);

        assertFalse(events.stream().anyMatch(
                event -> event.getMessage().getFormattedMessage().contains(WARNING)));
    }

    @Test
    void warnsWhenNotInProductionMode() {
        List<LogEvent> events = check(false);

        assertTrue(events.stream().anyMatch(
                event -> event.getMessage().getFormattedMessage().contains(WARNING)));
    }
}
