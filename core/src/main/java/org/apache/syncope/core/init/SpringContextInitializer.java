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
package org.apache.syncope.core.init;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.servlet.ServletContext;
import org.apache.syncope.core.workflow.ActivitiUserWorkflowAdapter;
import org.apache.syncope.core.workflow.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

/**
 * Take care of all initializations needed by Syncope to run up and safe.
 */
@Component
public class SpringContextInitializer implements ServletContextAware, BeanFactoryAware, InitializingBean {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SpringContextInitializer.class);

    private static String wfAdapterClassName;

    static {
        try {
            initWFAdapterClassName();
        } catch (IOException e) {
            LOG.error("Could not init wfAdapterClassName", e);
        }
    }

    /**
     * Read classpath:/workflow.properties in order to determine the configured workflow adapter class name.
     *
     * @throws IOException if anything goes wrong
     */
    public static void initWFAdapterClassName() throws IOException {
        Properties props = new java.util.Properties();
        InputStream propStream = null;
        try {
            propStream = ContentLoader.class.getResourceAsStream("/workflow.properties");
            props.load(propStream);
            wfAdapterClassName = props.getProperty("wfAdapter");
        } catch (Exception e) {
            LOG.error("Could not load workflow.properties", e);
        } finally {
            if (propStream != null) {
                propStream.close();
            }
        }
    }

    /**
     * Check if the configured user workflow adapter is Activiti's.
     *
     * @return whether Activiti is configured for workflow or not
     */
    public static boolean isActivitiConfigured() {
        return wfAdapterClassName != null && wfAdapterClassName.equals(ActivitiUserWorkflowAdapter.class.getName());
    }

    @Autowired
    private ConnInstanceLoader connInstanceLoader;

    @Autowired
    private ContentLoader contentLoader;

    @Autowired
    private JobInstanceLoader jobInstanceLoader;

    @Autowired
    private UserWorkflowAdapter wfAdapter;

    @Autowired
    private LoggerLoader loggerLoader;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    private DefaultListableBeanFactory beanFactory;

    @Override
    public void setServletContext(final ServletContext servletContext) {
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        contentLoader.load();
        connInstanceLoader.load();
        jobInstanceLoader.load();
        loggerLoader.load();
        classNamesLoader.load();

        if (wfAdapter.getLoaderClass() != null) {
            final WorkflowLoader wfLoader = (WorkflowLoader) beanFactory.createBean(
                    wfAdapter.getLoaderClass(), AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
            wfLoader.load();
        }
    }
}
