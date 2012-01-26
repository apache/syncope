/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest;

import static org.junit.Assert.*;

import ch.qos.logback.classic.Level;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.syncope.client.to.LoggerTO;

public class LogTestITCase extends AbstractTest {

    @Test
    public void list() {
        List<LoggerTO> loggers = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "log/controller/list", LoggerTO[].class));
        assertNotNull(loggers);
        assertFalse(loggers.isEmpty());
        for (LoggerTO logger : loggers) {
            assertNotNull(logger);
        }
    }

    public void setLoggerLevel() {
        LoggerTO logger = restTemplate.postForObject(
                BASE_URL + "log/controller/{name}/{level}",
                null, LoggerTO.class, "org.syncope.core.monitor", "INFO");
        assertNotNull(logger);

        assertEquals(Level.INFO, logger.getLevel());
    }
}
