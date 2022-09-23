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
package org.apache.syncope.core.provisioning.livesync;

import java.util.concurrent.ThreadPoolExecutor;
import org.apache.syncope.core.provisioning.java.LiveSyncProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableKafka
@EnableConfigurationProperties(LiveSyncProperties.class)
@Configuration(proxyBeanMethods = false)
public class LiveSyncProvisioningContext {

    @Bean
    public KafkaProvisioningListener kafkaProvisioningListener() {
        return new KafkaProvisioningListener();
    }

    @Bean
    public ThreadPoolTaskExecutor livesyncTaskExecutorAsyncExecutor(final LiveSyncProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getPropagationTaskExecutorAsyncExecutor().getCorePoolSize());
        executor.setMaxPoolSize(props.getPropagationTaskExecutorAsyncExecutor().getMaxPoolSize());
        executor.setQueueCapacity(props.getPropagationTaskExecutorAsyncExecutor().getQueueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(
                props.getPropagationTaskExecutorAsyncExecutor().getAwaitTerminationSeconds());
        executor.setThreadNamePrefix("LiveSyncTaskExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
