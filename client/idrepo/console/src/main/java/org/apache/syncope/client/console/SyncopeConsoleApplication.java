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
package org.apache.syncope.client.console;

import com.giffing.wicket.spring.boot.starter.web.config.WicketWebInitializerAutoConfig.WebSocketWicketWebInitializerAutoConfiguration;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.MIMETypesLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.HiddenHttpMethodFilter;

@SpringBootApplication(exclude = {
    ErrorMvcAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class })
public class SyncopeConsoleApplication extends SpringBootServletInitializer {

    public static void main(final String[] args) {
        SpringApplication.run(SyncopeConsoleApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder builder) {
        builder.properties(WebSocketWicketWebInitializerAutoConfiguration.REGISTER_SERVER_ENDPOINT_ENABLED + "=false");
        return super.configure(builder);
    }

    @ConditionalOnMissingBean(name = "classPathScanImplementationLookup")
    @Bean
    public ClassPathScanImplementationLookup classPathScanImplementationLookup() {
        ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup();
        lookup.load();
        return lookup;
    }

    @ConditionalOnMissingBean(name = "mimeTypesLoader")
    @Bean
    public MIMETypesLoader mimeTypesLoader() {
        MIMETypesLoader mimeTypesLoader = new MIMETypesLoader();
        mimeTypesLoader.load();
        return mimeTypesLoader;
    }

    @Bean
    public FilterRegistrationBean<HiddenHttpMethodFilter> registration(final HiddenHttpMethodFilter filter) {
        FilterRegistrationBean<HiddenHttpMethodFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
