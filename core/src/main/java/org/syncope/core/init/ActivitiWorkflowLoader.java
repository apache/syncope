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

import java.io.IOException;
import java.io.InputStream;
import org.activiti.engine.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivitiWorkflowLoader {

    private static final Logger LOG = LoggerFactory.getLogger(
            ActivitiWorkflowLoader.class);

    private static final String WF_DEF_NAME = "userWorkflow.bpmn20.xml";

    @Autowired
    private RepositoryService repositoryService;

    public void load() {
        InputStream wfDefinitionStream = null;
        try {
            wfDefinitionStream = getClass().getResourceAsStream("/"
                    + WF_DEF_NAME);

            repositoryService.createDeployment().
                    addInputStream(WF_DEF_NAME, wfDefinitionStream).deploy();
        } finally {
            try {
                if (wfDefinitionStream != null) {
                    wfDefinitionStream.close();
                }
            } catch (IOException e) {
                LOG.error("While closing input stream for "
                        + "user workflow definition", e);
            }
        }
    }
}
