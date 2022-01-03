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
import java.util.regex.Pattern;
import javax.security.auth.login.AppConfigurationEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterProperties;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.server.auth.DigestLoginModule;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@EnableConfigurationProperties(KeymasterProperties.class)
@Configuration(proxyBeanMethods = false)
public class ZookeeperKeymasterClientContext {

    private static final Pattern IPV4 = Pattern.compile(
            "^((\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})|[a-z\\.]+):[0-9]+$");

    static class ZookeeperCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            String keymasterAddress = context.getEnvironment().getProperty("keymaster.address");
            return new ConditionOutcome(
                    keymasterAddress != null && IPV4.matcher(keymasterAddress).matches(),
                    "Keymaster address not set for Zookeeper: " + keymasterAddress);
        }
    }

    @Conditional(ZookeeperCondition.class)
    @Bean
    public CuratorFramework curatorFramework(final KeymasterProperties props) throws InterruptedException {
        if (StringUtils.isNotBlank(props.getUsername()) && StringUtils.isNotBlank(props.getPassword())) {
            javax.security.auth.login.Configuration.setConfiguration(new javax.security.auth.login.Configuration() {

                private final AppConfigurationEntry[] entries = {
                    new AppConfigurationEntry(
                    DigestLoginModule.class.getName(),
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    Map.of(
                    "username", props.getUsername(),
                    "password", props.getPassword()
                    ))
                };

                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(final String name) {
                    return entries;
                }
            });
        }

        CuratorFrameworkFactory.Builder clientBuilder = CuratorFrameworkFactory.builder().
                connectString(props.getAddress()).
                retryPolicy(new ExponentialBackoffRetry(props.getBaseSleepTimeMs(), props.getMaxRetries()));
        if (StringUtils.isNotBlank(props.getUsername())) {
            clientBuilder.authorization("digest", props.getUsername().getBytes()).aclProvider(new ACLProvider() {

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

    @Conditional(ZookeeperCondition.class)
    @Bean
    public ConfParamOps selfConfParamOps(final CuratorFramework client) {
        return new ZookeeperConfParamOps(client);
    }

    @Conditional(ZookeeperCondition.class)
    @Bean
    public ServiceOps serviceOps() {
        return new ZookeeperServiceDiscoveryOps();
        //return new ZookeeperServiceOps();
    }

    @Conditional(ZookeeperCondition.class)
    @Bean
    public DomainOps domainOps() {
        return new ZookeeperDomainOps();
    }
}
