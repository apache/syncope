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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.spring.ResourceWithFallbackLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class StartupDomainLoader implements SyncopeCoreLoader {

    private static final Logger LOG = LoggerFactory.getLogger(StartupDomainLoader.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private DomainOps domainOps;

    @Autowired
    private ConfigurableApplicationContext ctx;

    @Autowired
    private DomainHolder domainHolder;

    @Autowired
    private DomainRegistry domainRegistry;

    @Value("${content.directory}")
    private String contentDirectory;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void load() {
        try {
            Map<String, Domain> keymasterDomains = domainOps.list().stream().
                    collect(Collectors.toMap(Domain::getKey, Function.identity()));

            for (Resource domainProp : ctx.getResources("classpath:/domains/*.properties")) {
                String domainPropFile = StringUtils.substringAfterLast(domainProp.getURL().toExternalForm(), "/");
                String key = StringUtils.substringBefore(domainPropFile, ".");

                if (!domainHolder.getDomains().containsKey(key)) {
                    if (keymasterDomains.containsKey(key)) {
                        LOG.info("Domain {} initialization", key);

                        domainRegistry.register(keymasterDomains.get(key));

                        LOG.info("Domain {} successfully inited", key);
                    } else {
                        Domain.Builder builder = new Domain.Builder(key);

                        Properties props = PropertyUtils.read(getClass(),
                                "domains/" + domainPropFile, contentDirectory);
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
                                builder.poolMaxActive(Integer.parseInt(props.getProperty(prop)));
                            } else if (prop.endsWith(".pool.minIdle")) {
                                builder.poolMinIdle(Integer.parseInt(props.getProperty(prop)));
                            } else if (prop.endsWith(".audit.sql")) {
                                builder.auditSql(props.getProperty(prop));
                            }
                        }

                        ResourceWithFallbackLoader content =
                                ctx.getBeanFactory().createBean(ResourceWithFallbackLoader.class);
                        content.setPrimary("file:" + contentDirectory + "/domains/" + key + "Content.xml");
                        content.setFallback("classpath:domains/" + key + "Content.xml");
                        builder.content(IOUtils.toString(content.getResource().getInputStream()));

                        ResourceWithFallbackLoader keymasterConfParams =
                                ctx.getBeanFactory().createBean(ResourceWithFallbackLoader.class);
                        keymasterConfParams.setPrimary(
                                "file:" + contentDirectory + "/domains/" + key + "KeymasterConfParams.json");
                        keymasterConfParams.setFallback("classpath:domains/" + key + "KeymasterConfParams.json");
                        builder.keymasterConfParams(
                                IOUtils.toString(keymasterConfParams.getResource().getInputStream()));

                        ResourceWithFallbackLoader security =
                                ctx.getBeanFactory().createBean(ResourceWithFallbackLoader.class);
                        security.setPrimary("file:" + contentDirectory + "/domains/" + key + "Security.json");
                        security.setFallback("classpath:domains/" + key + "Security.json");

                        JsonNode securityInfo = MAPPER.readTree(security.getResource().getInputStream());
                        builder.adminPassword(securityInfo.get("password").asText());
                        builder.adminCipherAlgorithm(
                                CipherAlgorithm.valueOf(securityInfo.get("cipherAlgorithm").asText()));

                        domainOps.create(builder.build());
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            LOG.error("Error during domain initialization", e);
        }
    }
}
