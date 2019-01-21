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
package org.apache.syncope.fit.buildtools;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("classpath:/buildToolsContext.xml")
@ComponentScan("org.apache.syncope.fit.buildtools")
@EnableAutoConfiguration(exclude = {
    ErrorMvcAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class })
public class SyncopeBuildToolsApplication extends SpringBootServletInitializer {

    public static void main(final String[] args) {
        SpringApplication.run(SyncopeBuildToolsApplication.class, args);
    }

    @Override
    public void onStartup(final ServletContext sc) throws ServletException {
        sc.addListener(new ConnectorServerStartStopListener());
        sc.addListener(new ApacheDSStartStopListener());
        sc.addListener(new H2StartStopListener());

        ServletRegistration.Dynamic apacheDS = sc.addServlet("ApacheDSRootDseServlet", ApacheDSRootDseServlet.class);
        apacheDS.addMapping("/apacheDS");
        ServletRegistration.Dynamic sts = sc.addServlet("ServiceTimeoutServlet", ServiceTimeoutServlet.class);
        sts.addMapping("/services/*");

        super.onStartup(sc);
    }
}
