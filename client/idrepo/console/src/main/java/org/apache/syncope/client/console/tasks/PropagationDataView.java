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
package org.apache.syncope.client.console.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task attributes details.
 */
public class PropagationDataView extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = -4110576026663173545L;

    private static final Logger LOG = LoggerFactory.getLogger(AnyPropagationTasks.class);

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    public PropagationDataView(final PropagationTaskTO taskTO) {
        super();

        Pair<String, String> info = Pair.of(taskTO.getEntityKey(), getJSONInfo(taskTO));
        JsonEditorPanel jsonPanel =
                new JsonEditorPanel(null, new PropertyModel<>(info, "value"), true, null) {

            private static final long serialVersionUID = -8927036362466990179L;

            @Override
            public void onSubmit(final AjaxRequestTarget target) {
                modal.close(target);
            }
        };

        add(jsonPanel);
    }

    private static String getJSONInfo(final PropagationTaskTO taskTO) {
        String json = "";
        try {
            JsonNode list = MAPPER.readTree(taskTO.getPropagationData());
            json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(list);
        } catch (IOException ex) {
            LOG.error("Error converting objects to JSON", ex);
        }

        return json;
    }
}
