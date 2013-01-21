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
package org.apache.syncope.console.pages;

import org.apache.syncope.common.to.TaskTO;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.model.PropertyModel;

/**
 * Modal window with Task form (to stop and start execution).
 */
public class PropagationTaskModalPage extends TaskModalPage {

    private static final long serialVersionUID = 523379887023786151L;

    public PropagationTaskModalPage(final TaskTO taskTO) {
        super(taskTO);

        final AjaxTextFieldPanel accountId = new AjaxTextFieldPanel("accountId", getString("accountId"),
                new PropertyModel<String>(taskTO, "accountId"));
        accountId.setEnabled(false);
        profile.add(accountId);

        final AjaxTextFieldPanel resource = new AjaxTextFieldPanel("resource", getString("resource"),
                new PropertyModel<String>(taskTO, "resource"));
        resource.setEnabled(false);
        profile.add(resource);
    }
}
