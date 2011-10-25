/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.init;

import javax.servlet.ServletContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

/**
 * Take care of all initializations needed by Syncope to run up and safe.
 */
@Component
public class SpringContextInitializer implements ServletContextAware,
        BeanFactoryAware, InitializingBean {

    @Autowired
    private ConnInstanceLoader connInstanceLoader;

    @Autowired
    private ContentLoader contentLoader;

    @Autowired
    private JobInstanceLoader jobInstanceLoader;

    @Autowired
    private ActivitiWorkflowLoader activitiWorkflowLoader;

    @Override
    public void setServletContext(final ServletContext servletContext) {
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory)
            throws BeansException {
    }

    @Override
    public void afterPropertiesSet()
            throws Exception {

        contentLoader.load();
        connInstanceLoader.load();
        jobInstanceLoader.load();
        activitiWorkflowLoader.load();
    }
}
