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

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.xml.ws.Endpoint;
import java.util.List;
import javax.sql.DataSource;
import net.tirasa.connid.bundles.soap.provisioning.interfaces.Provisioning;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.syncope.fit.buildtools.cxf.DateParamConverterProvider;
import org.apache.syncope.fit.buildtools.cxf.GreenMailService;
import org.apache.syncope.fit.buildtools.cxf.ProvisioningImpl;
import org.apache.syncope.fit.buildtools.cxf.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
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

@SpringBootApplication(exclude = {
    ErrorMvcAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class }, proxyBeanMethods = false)
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

    @Bean
    public DriverManagerDataSource testDataSource() {
        DriverManagerDataSource testDataSource = new DriverManagerDataSource(testDbUrl, testDbUsername, testDbPassword);
        testDataSource.setDriverClassName(testDbDriver);
        return testDataSource;
    }

    @Bean
    public Provisioning provisioningImpl(@Qualifier("testDataSource") final DataSource dataSource) {
        return new ProvisioningImpl(dataSource);
    }

    @Bean
    public Endpoint soapProvisioning(final Provisioning provisioning, final Bus bus) {
        EndpointImpl soapProvisioning = new EndpointImpl(provisioning);
        soapProvisioning.setBus(bus);
        soapProvisioning.publish("/soap");
        return soapProvisioning;
    }

    @Bean
    public GreenMailService greenMailService() {
        return new GreenMailService();
    }

    @Bean
    public UserService userService() {
        return new UserService();
    }

    @Bean
    public Server restProvisioning(
            final GreenMailService greenMailService,
            final UserService userService,
            final Bus bus,
            final ApplicationContext ctx) {

        SpringJAXRSServerFactoryBean restProvisioning = new SpringJAXRSServerFactoryBean();
        restProvisioning.setApplicationContext(ctx);
        restProvisioning.setBus(bus);
        restProvisioning.setAddress("/rest");
        restProvisioning.setStaticSubresourceResolution(true);
        restProvisioning.setServiceBeans(List.of(greenMailService, userService));
        restProvisioning.setProviders(List.of(new JacksonJsonProvider(), new DateParamConverterProvider()));
        return restProvisioning.create();
    }

    @Override
    public void onStartup(final ServletContext sc) throws ServletException {
        sc.addListener(new ConnectorServerStartStopListener());
        sc.addListener(new LDAPStartStopListener());
        sc.addListener(new H2StartStopListener());
        sc.addListener(new GreenMailStartStopListener());
        sc.addListener(new KafkaBrokerStartStopListener());

        ServletRegistration.Dynamic sts = sc.addServlet("ServiceTimeoutServlet", ServiceTimeoutServlet.class);
        sts.addMapping("/services/*");

        super.onStartup(sc);
    }
}
