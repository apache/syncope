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
package org.apache.syncope.client.console.panels;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMEnterpriseUserConf;
import org.apache.syncope.common.lib.scim.SCIMManagerConf;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCIMConfEnterpriseUserPanel extends SCIMConfTabPanel {

    protected static final Logger LOG = LoggerFactory.getLogger(SCIMConfEnterpriseUserPanel.class);

    private static final long serialVersionUID = -4183306437598820588L;

    private final SCIMEnterpriseUserConf scimEnterpriseUserConf;

    public SCIMConfEnterpriseUserPanel(final String id, final SCIMConf scimConf) {
        super(id);

        if (scimConf.getEnterpriseUserConf() == null) {
            scimConf.setEnterpriseUserConf(new SCIMEnterpriseUserConf());
        }
        if (scimConf.getEnterpriseUserConf().getManager() == null) {
            scimConf.getEnterpriseUserConf().setManager(new SCIMManagerConf());
        }
        scimEnterpriseUserConf = scimConf.getEnterpriseUserConf();

        AjaxTextFieldPanel costCenterPanel = new AjaxTextFieldPanel(
                "costCenter", "costCenter", new PropertyModel<>("costCenter", "costCenter") {

            private static final long serialVersionUID = -6427731218492117883L;

            @Override
            public String getObject() {
                return scimEnterpriseUserConf.getCostCenter();
            }

            @Override
            public void setObject(final String object) {
                scimEnterpriseUserConf.setCostCenter(object);
            }
        });
        costCenterPanel.setChoices(userPlainSchemas.getObject());

        AjaxTextFieldPanel departmentPanel = new AjaxTextFieldPanel(
                "department", "department", new PropertyModel<>("department", "department") {

            private static final long serialVersionUID = -6427731218492117883L;

            @Override
            public String getObject() {
                return scimEnterpriseUserConf.getDepartment();
            }

            @Override
            public void setObject(final String object) {
                scimEnterpriseUserConf.setDepartment(object);
            }
        });
        departmentPanel.setChoices(userPlainSchemas.getObject());

        AjaxTextFieldPanel divisionPanel = new AjaxTextFieldPanel(
                "division", "division", new PropertyModel<>("division", "division") {

            private static final long serialVersionUID = -6427731218492117883L;

            @Override
            public String getObject() {
                return scimEnterpriseUserConf.getDivision();
            }

            @Override
            public void setObject(final String object) {
                scimEnterpriseUserConf.setDivision(object);
            }
        });
        divisionPanel.setChoices(userPlainSchemas.getObject());

        AjaxTextFieldPanel employeeNumberPanel = new AjaxTextFieldPanel(
                "employeeNumber", "employeeNumber", new PropertyModel<>("employeeNumber", "employeeNumber") {

            private static final long serialVersionUID = -6427731218492117883L;

            @Override
            public String getObject() {
                return scimEnterpriseUserConf.getEmployeeNumber();
            }

            @Override
            public void setObject(final String object) {
                scimEnterpriseUserConf.setEmployeeNumber(object);
            }
        });
        employeeNumberPanel.setChoices(userPlainSchemas.getObject());

        AjaxTextFieldPanel organizationPanel = new AjaxTextFieldPanel(
                "organization", "organization", new PropertyModel<>("organization", "organization") {

            private static final long serialVersionUID = -6427731218492117883L;

            @Override
            public String getObject() {
                return scimEnterpriseUserConf.getOrganization();
            }

            @Override
            public void setObject(final String object) {
                scimEnterpriseUserConf.setOrganization(object);
            }
        });
        organizationPanel.setChoices(userPlainSchemas.getObject());

        // manager
        buildManagerAccordion();

        add(costCenterPanel);
        add(departmentPanel);
        add(divisionPanel);
        add(employeeNumberPanel);
        add(organizationPanel);
    }

    private void buildManagerAccordion() {
        Accordion accordion = new Accordion("managerAccordion", List.of(new AbstractTab(Model.of("manager")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return buildNameAccordionContent(panelId);
            }
        }), Model.of(-1)); // accordion closed at beginning
        add(accordion.setOutputMarkupId(true));
    }

    private SCIMConfAccordionContainer buildNameAccordionContent(final String panelId) {
        List<AjaxTextFieldPanel> panelList = new ArrayList<>();

        AjaxTextFieldPanel managerKeyPanel = new AjaxTextFieldPanel(
                "accordionContent", "manager.key",
                new PropertyModel<>(scimEnterpriseUserConf.getManager(), "accordionContent") {

            private static final long serialVersionUID = -6427731218492117883L;

            @Override
            public String getObject() {
                return scimEnterpriseUserConf.getManager().getKey();
            }

            @Override
            public void setObject(final String object) {
                scimEnterpriseUserConf.getManager().setKey(object);
            }
        });
        managerKeyPanel.setChoices(userPlainSchemas.getObject());

        AjaxTextFieldPanel managerDisplaNamePanel = new AjaxTextFieldPanel(
                "accordionContent", "manager.displayName",
                new PropertyModel<>(scimEnterpriseUserConf.getManager(), "accordionContent") {

            private static final long serialVersionUID = -6427731218492117883L;

            @Override
            public String getObject() {
                return scimEnterpriseUserConf.getManager().getDisplayName();
            }

            @Override
            public void setObject(final String object) {
                scimEnterpriseUserConf.getManager().setDisplayName(object);
            }
        });
        managerDisplaNamePanel.setChoices(userPlainSchemas.getObject());

        panelList.add(managerKeyPanel);
        panelList.add(managerDisplaNamePanel);

        add(new Label("managerLabel", Model.of("manager")));

        return new SCIMConfAccordionContainer(panelId, panelList);
    }
}
