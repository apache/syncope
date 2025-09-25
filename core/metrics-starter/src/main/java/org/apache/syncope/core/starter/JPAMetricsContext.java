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
package org.apache.syncope.core.starter;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.List;
import javax.sql.DataSource;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.metrics.data.MetricsRepositoryMethodInvocationListener;
import org.springframework.boot.actuate.metrics.jdbc.DataSourcePoolMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

@ConditionalOnClass(HikariDataSource.class)
@Import(DataSourcePoolMetadataProvidersConfiguration.class)
@Configuration(proxyBeanMethods = false)
public class JPAMetricsContext {

    @Bean
    public MeterBinder dataSourcePoolMetadataMeterBinder(
            final ConfigurableListableBeanFactory beanFactory,
            final ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders,
            final DomainHolder<DataSource> domainHolder) {

        return new MeterBinder() {

            @Override
            public void bindTo(final MeterRegistry registry) {
                List<DataSourcePoolMetadataProvider> metadataProvidersList = metadataProviders.stream().toList();
                domainHolder.getDomains().forEach((name, dataSource) -> new DataSourcePoolMetrics(
                        dataSource, metadataProvidersList, name, List.of()).bindTo(registry));
            }
        };
    }

    @Bean
    public static BeanPostProcessor jpaRepositoryFactoryBeanPostProcessor(
            final ObjectProvider<MetricsRepositoryMethodInvocationListener> metricsRepositoryMethodInvocationListener) {

        return new BeanPostProcessor() {

            @Override
            public Object postProcessBeforeInitialization(final Object bean, final String beanName)
                    throws BeansException {

                if (bean instanceof JpaRepositoryFactory jpaRepositoryFactory) {
                    MetricsRepositoryMethodInvocationListener listener =
                            SingletonSupplier.of(metricsRepositoryMethodInvocationListener::getObject).get();
                    Assert.state(listener != null, "'listener' must not be null");
                    jpaRepositoryFactory.addInvocationListener(listener);
                }
                return bean;
            }
        };
    }
}
