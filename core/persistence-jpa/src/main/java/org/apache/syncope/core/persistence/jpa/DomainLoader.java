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
package org.apache.syncope.core.persistence.jpa;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.DomainCR;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DomainLoader implements SyncopeCoreLoader {

    private static final Logger LOG = LoggerFactory.getLogger(DomainLoader.class);

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private DomainConfFactory domainConfFactory;

    @Value("${content.directory}")
    private String contentDirectory;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void load() {
        try {
            for (Resource domainProp : ctx.getResources("classpath:/domains/*.properties")) {
                String domainPropFile = StringUtils.substringAfterLast(domainProp.getURL().toExternalForm(), "/");
                String domain = StringUtils.substringBefore(domainPropFile, ".");

                if (!SyncopeConstants.MASTER_DOMAIN.equals(domain)) {
                    DomainCR.Builder builder = new DomainCR.Builder(domain);

                    LOG.info("Domain {} initialization", domain);

                    Properties props = PropertyUtils.read(getClass(), "domains/" + domainPropFile, contentDirectory);
                    for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements();) {
                        String prop = (String) e.nextElement();

                        if (prop.endsWith(".driverClassName")) {
                            builder.jdbcDriver(props.getProperty(prop));
                        } else if (prop.endsWith(".url")) {
                            builder.jdbcURL(props.getProperty(prop));
                        } else if (prop.endsWith(".schema")) {
                            builder.dbSchema(props.getProperty(prop));
                        } else if (prop.endsWith(".username")) {
                            builder.dbUsername(props.getProperty(prop));
                        } else if (prop.endsWith(".password")) {
                            builder.dbPassword(props.getProperty(prop));
                        } else if (prop.endsWith(".databasePlatform")) {
                            builder.databasePlatform(props.getProperty(prop));
                        } else if (prop.endsWith(".orm")) {
                            builder.orm(props.getProperty(prop));
                        } else if (prop.endsWith(".pool.maxActive")) {
                            builder.maxPoolSize(Integer.parseInt(props.getProperty(prop)));
                        } else if (prop.endsWith(".pool.minIdle")) {
                            builder.minIdle(Integer.parseInt(props.getProperty(prop)));
                        } else if (prop.endsWith(".audit.sql")) {
                            builder.auditSql(props.getProperty(prop));
                        }
                    }

                    domainConfFactory.register(builder.build());

                    LOG.info("Domain {} successfully inited", domain);
                }
            }
        } catch (IOException e) {
            LOG.error("Error during domain initialization", e);
        }
    }
}
