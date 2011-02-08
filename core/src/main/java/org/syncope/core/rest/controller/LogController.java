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
package org.syncope.core.rest.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.LoggerTO;

@Controller
@RequestMapping("/log")
public class LogController extends AbstractController {

    @PreAuthorize("hasRole('LOG_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/controller/list")
    public List<LoggerTO> getLoggers() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<LoggerTO> result =
                new ArrayList<LoggerTO>(lc.getLoggerList().size());
        LoggerTO loggerTO;
        for (Logger logger : lc.getLoggerList()) {
            if (logger.getLevel() != null) {
                loggerTO = new LoggerTO();
                loggerTO.setName(logger.getName());
                loggerTO.setLevel(logger.getLevel().toString());
                result.add(loggerTO);
            }
        }

        return result;
    }

    @PreAuthorize("hasRole('LOG_SET_LEVEL')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/controller/{name}/{level}")
    public LoggerTO setLoggerLevel(@PathVariable("name") final String name,
            @PathVariable("level") final Level level)
            throws NotFoundException {

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = lc.getLogger(name);
        if (logger == null) {
            throw new NotFoundException("Logger '" + name + "'");
        }

        logger.setLevel(level);

        LoggerTO result = new LoggerTO();
        result.setName(logger.getName());
        result.setLevel(logger.getLevel().toString());
        return result;
    }
}
