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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonDiffPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.common.lib.to.AbstractHistoryConf;
import org.apache.syncope.common.lib.to.ConnInstanceHistoryConfTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceHistoryConfTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class HistoryConfDetails<T extends AbstractHistoryConf> extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = -7400543686272100483L;

    private final T selectedHistoryConfTO;

    private final List<T> availableHistoryConfTOs;

    private AbstractModalPanel jsonEditorPanel;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public HistoryConfDetails(final BaseModal<?> baseModal, final T selectedHistoryConfTO,
            final PageReference pageRef, final List<T> availableHistoryConfTOs) {
        super();

        // remove selected conf from list
        CollectionUtils.filter(availableHistoryConfTOs, new Predicate<T>() {

            @Override
            public boolean evaluate(final T object) {
                return !object.getKey().equals(selectedHistoryConfTO.getKey());
            }
        });
        this.availableHistoryConfTOs = availableHistoryConfTOs;
        this.selectedHistoryConfTO = selectedHistoryConfTO;

        // add current conf to list
        addCurrentInstanceConf();

        Form form = initDropdownDiffConfForm();
        add(form);
        if (availableHistoryConfTOs.isEmpty()) {
            form.setVisible(false);
        } else {
            form.setVisible(true);
        }

        showConfigurationSinglePanel();
    }

    private void showConfigurationSinglePanel() {
        final Pair info = getJSONInfo(selectedHistoryConfTO);

        jsonEditorPanel = new JsonEditorPanel(null, new PropertyModel<String>(info, "value"), true, null) {

            private static final long serialVersionUID = -8927036362466990179L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    modal.close(target);
                } catch (Exception e) {
                    LOG.error("While updating console layout info for history configuration {}", info.getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        jsonEditorPanel.setOutputMarkupId(true);

        addOrReplace(jsonEditorPanel);
    }

    private void showConfigurationDiffPanel(final List<T> historyConfTOs) {
        final List<Pair> infos = new ArrayList<>();
        for (T historyConfTO : historyConfTOs) {
            infos.add(getJSONInfo(historyConfTO));
        }

        jsonEditorPanel = new JsonDiffPanel(null, new PropertyModel<String>(infos.get(0), "value"),
                new PropertyModel<String>(infos.get(1), "value"), null) {

            private static final long serialVersionUID = -8927036362466990179L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    modal.close(target);
                } catch (Exception e) {
                    LOG.error("While updating console layout info for history configurations {}", infos, e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };

        replace(jsonEditorPanel);
    }

    private Pair getJSONInfo(final T historyConfTO) {
        Object conf = null; // selected configuration instance
        String key = "";
        if (historyConfTO instanceof ConnInstanceHistoryConfTO) {
            ConnInstanceHistoryConfTO historyConf = ConnInstanceHistoryConfTO.class.cast(historyConfTO);
            conf = historyConf.getConnInstanceTO();
            key = historyConf.getKey();
        } else if (historyConfTO instanceof ResourceHistoryConfTO) {
            ResourceHistoryConfTO historyConf = ResourceHistoryConfTO.class.cast(historyConfTO);
            conf = historyConf.getResourceTO();
            key = historyConf.getKey();
        }

        String json = "";
        try {
            json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(conf);
        } catch (IOException ex) {
            DirectoryPanel.LOG.error("Error converting objects to JSON", ex);
        }

        Pair info = new ImmutablePair<>(key, json);

        return info;
    }

    private Map<String, String> getDropdownNamesMap(final List<T> historyConfTOs) {
        Map<String, String> historyConfMap = new HashMap<>();
        if (selectedHistoryConfTO instanceof ConnInstanceHistoryConfTO) {
            for (T historyConfValue : historyConfTOs) {
                ConnInstanceHistoryConfTO historyConf = ConnInstanceHistoryConfTO.class.cast(historyConfValue);
                historyConfMap.put(historyConf.getKey(),
                        historyConf.getCreation() != null ? historyConf.getCreator() + " - " + SyncopeConsoleSession.
                        get().getDateFormat().format(
                                historyConf.getCreation()) + " - " + historyConf.getKey() : getString("current"));
            }
        } else if (selectedHistoryConfTO instanceof ResourceHistoryConfTO) {
            for (T historyConfValue : historyConfTOs) {
                ResourceHistoryConfTO historyConf = ResourceHistoryConfTO.class.cast(historyConfValue);
                historyConfMap.put(historyConf.getKey(),
                        historyConf.getCreation() != null ? historyConf.getCreator() + " - " + SyncopeConsoleSession.
                        get().getDateFormat().format(
                                historyConf.getCreation()) + " - " + historyConf.getKey() : getString("current"));
            }
        }
        return historyConfMap;
    }

    private Form initDropdownDiffConfForm() {
        final Form<T> form = new Form<>("form");
        form.setModel(new CompoundPropertyModel<>(selectedHistoryConfTO));
        form.setOutputMarkupId(true);

        final Map<String, String> namesMap = getDropdownNamesMap(availableHistoryConfTOs);
        List<String> keys = new ArrayList<>(namesMap.keySet());

        final AjaxDropDownChoicePanel<String> dropdownElem = new AjaxDropDownChoicePanel<>(
                "compareDropdown",
                getString("compare"),
                new PropertyModel<String>(selectedHistoryConfTO, "key"),
                false);
        dropdownElem.setChoices(keys);
        dropdownElem.setChoiceRenderer(new IChoiceRenderer<String>() {

            private static final long serialVersionUID = -6265603675261014912L;

            @Override
            public Object getDisplayValue(final String value) {
                return namesMap.get(value) == null ? value : namesMap.get(value);
            }

            @Override
            public String getIdValue(final String value, final int i) {
                return value;
            }

            @Override
            public String getObject(
                    final String id, final IModel<? extends List<? extends String>> choices) {
                return id;
            }
        });
        dropdownElem.setNullValid(true);
        dropdownElem.getField().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                List<T> elemsToCompare = new ArrayList<>();
                elemsToCompare.add(selectedHistoryConfTO);

                final String selectedKey = dropdownElem.getModelObject();
                if (selectedKey != null) {
                    if (!selectedKey.isEmpty()) {
                        T confToCompare = IterableUtils.find(availableHistoryConfTOs, new Predicate<T>() {

                            @Override
                            public boolean evaluate(final T object) {
                                return object.getKey().equals(selectedKey);
                            }
                        });
                        elemsToCompare.add(confToCompare);
                        showConfigurationDiffPanel(elemsToCompare);
                    } else {
                        showConfigurationSinglePanel();
                    }
                }
                target.add(jsonEditorPanel);
            }
        });
        form.add(dropdownElem);

        return form;
    }

    @SuppressWarnings("unchecked")
    private void addCurrentInstanceConf() {
        if (selectedHistoryConfTO instanceof ConnInstanceHistoryConfTO) {
            ConnectorRestClient connRestClient = new ConnectorRestClient();
            ConnInstanceTO currentConn = connRestClient.read(
                    ConnInstanceHistoryConfTO.class.cast(selectedHistoryConfTO).getConnInstanceTO().getKey());
            ConnInstanceHistoryConfTO newConnInstanceHistoryConf = new ConnInstanceHistoryConfTO();
            newConnInstanceHistoryConf.setConnInstanceTO(currentConn);
            newConnInstanceHistoryConf.setCreator(selectedHistoryConfTO.getCreator());
            newConnInstanceHistoryConf.setKey("current");
            availableHistoryConfTOs.add((T) newConnInstanceHistoryConf);
        } else if (selectedHistoryConfTO instanceof ResourceHistoryConfTO) {
            ResourceRestClient resourceRestClient = new ResourceRestClient();
            ResourceTO currentRes = resourceRestClient.read(
                    ResourceHistoryConfTO.class.cast(selectedHistoryConfTO).getResourceTO().getKey());
            ResourceHistoryConfTO newResHistoryConf = new ResourceHistoryConfTO();
            newResHistoryConf.setResourceTO(currentRes);
            newResHistoryConf.setCreator(selectedHistoryConfTO.getCreator());
            newResHistoryConf.setKey("current");
            availableHistoryConfTOs.add((T) newResHistoryConf);
        }
    }
}
