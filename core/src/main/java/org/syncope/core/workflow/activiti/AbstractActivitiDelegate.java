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
package org.syncope.core.workflow.activiti;

import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.rest.data.UserDataBinder;
import org.syncope.core.util.ApplicationContextManager;

/**
 * Abstract base class for Activiti's JavaDelegate implementations in Syncope.
 */
public abstract class AbstractActivitiDelegate implements JavaDelegate {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(AbstractActivitiDelegate.class);

    protected static final ConfigurableApplicationContext CONTEXT =
            ApplicationContextManager.getApplicationContext();

    protected TaskService taskService;

    protected UserDataBinder dataBinder;

    protected ConfDAO confDAO;

    @Override
    public final void execute(final DelegateExecution execution)
            throws Exception {

        taskService = CONTEXT.getBean(TaskService.class);

        dataBinder = CONTEXT.getBean(UserDataBinder.class);
        confDAO = CONTEXT.getBean(ConfDAO.class);

        doExecute(execution);
    }

    protected abstract void doExecute(DelegateExecution execution)
            throws Exception;
}
