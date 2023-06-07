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
package org.apache.syncope.client.console.reports;

import org.apache.syncope.client.console.panels.StartAtTogglePanel;
import org.apache.syncope.client.console.rest.ExecutionRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ReportStartAtTogglePanel extends StartAtTogglePanel {

    private static final long serialVersionUID = -3195479265440591519L;

    @SpringBean
    protected ReportRestClient reportRestClient;

    public ReportStartAtTogglePanel(final WebMarkupContainer container, final PageReference pageRef) {
        super(container, pageRef);
    }

    @Override
    protected ExecutionRestClient getRestClient() {
        return reportRestClient;
    }
}
