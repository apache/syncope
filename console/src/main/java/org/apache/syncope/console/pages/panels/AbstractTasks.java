/*
 * Copyright 2013 The Apache Software Foundation.
 *
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
package org.apache.syncope.console.pages.panels;

import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.rest.TaskRestClient;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public abstract class AbstractTasks extends Panel {

    private static final long serialVersionUID = -1190836516156843561L;

    protected static final String VIEW_TASK_WIN_COOKIE_NAME = "view-task-win";

    protected static final String TASKS = "Tasks";

    protected static final String FEEDBACK = "feedback";

    protected final String OPERATION_SUCCEEDED = getString("operation_succeeded");

    protected static final int WIN_HEIGHT = 500;

    protected static final int WIN_WIDTH = 700;

    @SpringBean
    protected TaskRestClient restClient;

    @SpringBean
    protected PreferenceManager prefMan;

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    public AbstractTasks(final String id) {
        super(id);
    }
}
