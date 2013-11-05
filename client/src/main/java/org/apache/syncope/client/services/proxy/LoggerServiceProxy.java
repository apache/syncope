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
package org.apache.syncope.client.services.proxy;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.syncope.common.services.LoggerService;
import org.apache.syncope.common.to.EventCategoryTO;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.types.LoggerType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.springframework.web.client.RestTemplate;

public class LoggerServiceProxy extends SpringServiceProxy implements LoggerService {

    public LoggerServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public List<LoggerTO> list(final LoggerType type) {
        switch (type) {
            case NORMAL:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + "logger/log/list", LoggerTO[].class));

            case AUDIT:
                List<AuditLoggerName> auditNames = Arrays.asList(getRestTemplate().getForObject(
                        baseUrl + "logger/audit/list", AuditLoggerName[].class));
                return CollectionWrapper.unwrapLogger(auditNames);

            default:
                throw new BadRequestException();
        }
    }

    @Override
    public LoggerTO read(final LoggerType type, final String name) {
        List<LoggerTO> logger = list(type);
        for (LoggerTO l : logger) {
            if (l.getName().equals(name)) {
                return l;
            }
        }
        throw new NotFoundException();
    }

    @Override
    public void update(final LoggerType type, final String name, final LoggerTO logger) {
        switch (type) {
            case NORMAL:
                getRestTemplate().postForObject(baseUrl + "logger/log/{name}/{level}", null, LoggerTO.class, name,
                        logger.getLevel());
                break;

            case AUDIT:
                try {
                    getRestTemplate().put(baseUrl + "logger/audit/enable", AuditLoggerName.fromLoggerName(name));
                } catch (Exception e) {
                    throw new BadRequestException(e);
                }
                break;

            default:
                throw new BadRequestException();
        }
    }

    @Override
    public void delete(final LoggerType type, final String name) {
        switch (type) {
            case NORMAL:
                getRestTemplate().getForObject(baseUrl + "logger/log/delete/{name}", LoggerTO.class, name);
                break;
            case AUDIT:
                try {
                    getRestTemplate().put(baseUrl + "logger/audit/disable", AuditLoggerName.fromLoggerName(name));
                } catch (Exception e) {
                    throw new BadRequestException(e);
                }
                break;

            default:
                throw new BadRequestException();
        }

    }

    @Override
    public List<EventCategoryTO> events() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "logger/events", EventCategoryTO[].class));
    }
}
