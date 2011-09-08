/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;

/**
 * Modal window with Task form (to stop and start execution).
 */
public class GTaskModalPage extends SchedTaskModalPage {

    private static final long serialVersionUID = -2501860242590060867L;

    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param connectorTO
     * @param create : set to true only if a CREATE operation is required
     */
    public GTaskModalPage(
            final BasePage basePage,
            final ModalWindow window,
            final SchedTaskTO taskTO) {

        super(basePage, window, taskTO);

        final IModel<List<String>> classNames =
                new LoadableDetachableModel<List<String>>() {

                    private static final long serialVersionUID = 5275935387613157437L;

                    @Override
                    protected List<String> load() {
                        final List<String> classes = new ArrayList<String>(
                                taskRestClient.getJobClasses());

                        return classes;
                    }
                };

        final AjaxDropDownChoicePanel<String> className =
                new AjaxDropDownChoicePanel<String>(
                "jobClassName", getString("class"),
                new PropertyModel(taskTO, "jobClassName"), false);
        className.setChoices(classNames.getObject());
        className.addRequiredLabel();
        className.setEnabled(taskTO.getId() == 0);
        className.setStyleShet(
                "ui-widget-content ui-corner-all long_dynamicsize");
        profile.add(className);
    }
}
