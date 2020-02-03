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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.syncope.fit.buildtools.cxf.DateParamConverterProvider;
import org.apache.syncope.fit.buildtools.cxf.ProvisioningImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@SpringBootApplication(scanBasePackages = "org.apache.syncope.fit.buildtools",
        exclude = {
            ErrorMvcAutoConfiguration.class,
            WebMvcAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class })
public class SyncopeBuildToolsApplication extends SpringBootServletInitializer {

    public static void main(final String[] args) {
        SpringApplication.run(SyncopeBuildToolsApplication.class, args);
    }

    @Value("${testdb.driver}")
    private String testDbDriver;

    @Value("${testdb.url}")
    private String testDbUrl;

    @Value("${testdb.username}")
    private String testDbUsername;

    @Value("${testdb.password}")
    private String testDbPassword;

    @Autowired
    private Bus bus;

    @Autowired
    private ProvisioningImpl provisioningImpl;

    @Autowired
    private ApplicationContext ctx;

    @Bean
    public DriverManagerDataSource testDataSource() {
        DriverManagerDataSource testDataSource = new DriverManagerDataSource(testDbUrl, testDbUsername, testDbPassword);
        testDataSource.setDriverClassName(testDbDriver);
        return testDataSource;
    }

    @Bean
    public Endpoint soapProvisioning() {
        EndpointImpl soapProvisioning = new EndpointImpl(provisioningImpl);
        soapProvisioning.setBus(bus);
        soapProvisioning.publish("/soap");
        return soapProvisioning;
    }

    @Bean
    public Server restProvisioning() {
        SpringJAXRSServerFactoryBean restProvisioning = new SpringJAXRSServerFactoryBean();
        restProvisioning.setApplicationContext(ctx);
        restProvisioning.setBus(bus);
        restProvisioning.setAddress("/rest");
        restProvisioning.setStaticSubresourceResolution(true);
        restProvisioning.setBasePackages(List.of("org.apache.syncope.fit.buildtools.cxf"));
        restProvisioning.setProviders(List.of(new JacksonJsonProvider(), new DateParamConverterProvider()));
        return restProvisioning.create();
    }

    @Override
    public void onStartup(final ServletContext sc) throws ServletException {
        sc.addListener(new ConnectorServerStartStopListener());
        sc.addListener(new ApacheDSStartStopListener());
        sc.addListener(new H2StartStopListener());
        sc.addListener(new GreenMailStartStopListener());

        ServletRegistration.Dynamic apacheDS = sc.addServlet("ApacheDSRootDseServlet", ApacheDSRootDseServlet.class);
        apacheDS.addMapping("/apacheDS");
        ServletRegistration.Dynamic sts = sc.addServlet("ServiceTimeoutServlet", ServiceTimeoutServlet.class);
        sts.addMapping("/services/*");

        super.onStartup(sc);
    }
}
