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
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.LoggerTO;
import org.syncope.core.persistence.beans.SyncopeLogger;
import org.syncope.core.persistence.dao.LoggerDAO;
import org.syncope.types.LoggerLevel;

@Controller
@RequestMapping("/logger")
public class LoggerController extends AbstractController {

    @Autowired
    private LoggerDAO loggerDAO;

    @PreAuthorize("hasRole('LOGGER_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    @Transactional(readOnly = true)
    public List<LoggerTO> list() {
        List<LoggerTO> result = new ArrayList<LoggerTO>();
        for (SyncopeLogger syncopeLogger : loggerDAO.findAll()) {
            LoggerTO loggerTO = new LoggerTO();
            BeanUtils.copyProperties(syncopeLogger, loggerTO);
            result.add(loggerTO);
        }

        return result;
    }

    @PreAuthorize("hasRole('LOGGER_SET_LEVEL')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/set/{name}/{level}")
    public LoggerTO setLevel(@PathVariable("name") final String name,
            @PathVariable("level") final Level level) {

        SyncopeLogger syncopeLogger = loggerDAO.find(name);
        if (syncopeLogger == null) {
            syncopeLogger = new SyncopeLogger();
            syncopeLogger.setName(name);
        }
        syncopeLogger.setLevel(LoggerLevel.fromLevel(level));
        syncopeLogger = loggerDAO.save(syncopeLogger);

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = lc.getLogger(name);
        logger.setLevel(level);

        LoggerTO result = new LoggerTO();
        BeanUtils.copyProperties(syncopeLogger, result);
        return result;
    }
}
