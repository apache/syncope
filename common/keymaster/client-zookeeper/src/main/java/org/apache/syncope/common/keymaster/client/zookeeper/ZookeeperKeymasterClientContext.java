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
package org.apache.syncope.common.keymaster.client.zookeeper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.AppConfigurationEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.server.auth.DigestLoginModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:keymaster.properties")
@PropertySource(value = "file:${conf.directory}/keymaster.properties", ignoreResourceNotFound = true)
@Configuration
public class ZookeeperKeymasterClientContext {

    @Value("${keymaster.address}")
    private String address;

    @Value("${keymaster.username}")
    private String username;

    @Value("${keymaster.password}")
    private String password;

    @Value("${keymaster.baseSleepTimeMs:100}")
    private Integer baseSleepTimeMs;

    @Value("${keymaster.maxRetries:3}")
    private Integer maxRetries;

    @ConditionalOnExpression("#{'${keymaster.address}' "
            + "matches '^((\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})|[a-z\\.]+):[0-9]+$'}")
    @Bean
    public CuratorFramework curatorFramework() throws InterruptedException {
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            javax.security.auth.login.Configuration.setConfiguration(new javax.security.auth.login.Configuration() {

                private final AppConfigurationEntry[] entries = {
                    new AppConfigurationEntry(
                    DigestLoginModule.class.getName(),
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    Map.of(
                    "username", username,
                    "password", password
                    ))
                };

                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(final String name) {
                    return entries;
                }
            });
        }

        CuratorFrameworkFactory.Builder clientBuilder = CuratorFrameworkFactory.builder().
                connectString(address).
                retryPolicy(new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries));
        if (StringUtils.isNotBlank(username)) {
            clientBuilder.authorization("digest", username.getBytes()).aclProvider(new ACLProvider() {

                @Override
                public List<ACL> getDefaultAcl() {
                    return ZooDefs.Ids.CREATOR_ALL_ACL;
                }

                @Override
                public List<ACL> getAclForPath(final String path) {
                    return ZooDefs.Ids.CREATOR_ALL_ACL;
                }
            });
        }
        CuratorFramework client = clientBuilder.build();
        client.start();
        client.blockUntilConnected(3, TimeUnit.SECONDS);

        return client;
    }

    @ConditionalOnExpression("#{'${keymaster.address}' "
            + "matches '^((\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})|[a-z\\.]+):[0-9]+$'}")
    @Bean
    public ConfParamOps selfConfParamOps() {
        return new ZookeeperConfParamOps();
    }

    @ConditionalOnExpression("#{'${keymaster.address}' "
            + "matches '^((\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})|[a-z\\.]+):[0-9]+$'}")
    @Bean
    public ServiceOps serviceOps() {
        return new ZookeeperServiceDiscoveryOps();
        //return new ZookeeperServiceOps();
    }

    @ConditionalOnExpression("#{'${keymaster.address}' "
            + "matches '^((\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})|[a-z\\.]+):[0-9]+$'}")
    @Bean
    public DomainOps domainOps() {
        return new ZookeeperDomainOps();
    }
}
