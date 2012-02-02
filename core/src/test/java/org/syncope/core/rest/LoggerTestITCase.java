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

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.syncope.client.to.LoggerTO;
import org.syncope.types.LoggerLevel;

public class LoggerTestITCase extends AbstractTest {

    @Test
    public void list() {
        List<LoggerTO> loggers = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "logger/list", LoggerTO[].class));
        assertNotNull(loggers);
        assertFalse(loggers.isEmpty());
        for (LoggerTO logger : loggers) {
            assertNotNull(logger);
        }
    }

    @Test
    public void setLevel() {
        List<LoggerTO> loggers = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "logger/list", LoggerTO[].class));
        assertNotNull(loggers);
        int startSize = loggers.size();

        LoggerTO logger = restTemplate.postForObject(
                BASE_URL + "logger/set/{name}/{level}",
                null, LoggerTO.class, "TEST", "INFO");
        assertNotNull(logger);
        assertEquals(LoggerLevel.INFO, logger.getLevel());

        loggers = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "logger/list", LoggerTO[].class));
        assertNotNull(loggers);
        assertEquals(startSize + 1, loggers.size());
    }
}
