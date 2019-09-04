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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final T selectedHistoryConfTO;

    private final List<T> availableHistoryConfTOs;

    private AbstractModalPanel<String> jsonPanel;

    public HistoryConfDetails(final BaseModal<?> baseModal, final T selectedHistoryConfTO,
            final PageReference pageRef, final List<T> availableHistoryConfTOs) {
        super();

        // remove selected conf from list
        this.availableHistoryConfTOs = availableHistoryConfTOs.stream().
                filter(object -> !object.getKey().equals(selectedHistoryConfTO.getKey())).collect(Collectors.toList());
        this.selectedHistoryConfTO = selectedHistoryConfTO;

        // add current conf to list
        addCurrentInstanceConf();

        Form<?> form = initDropdownDiffConfForm();
        add(form);
        form.setVisible(!availableHistoryConfTOs.isEmpty());

        showConfigurationSinglePanel();
    }

    private void showConfigurationSinglePanel() {
        Pair<String, String> info = getJSONInfo(selectedHistoryConfTO);

        jsonPanel = new JsonEditorPanel(null, new PropertyModel<String>(info, "right"), true, null) {

            private static final long serialVersionUID = -8927036362466990179L;

            @Override
            public void onSubmit(final AjaxRequestTarget target) {
                modal.close(target);
            }
        };
        jsonPanel.setOutputMarkupId(true);

        addOrReplace(jsonPanel);
    }

    private void showConfigurationDiffPanel(final List<T> historyConfTOs) {
        List<Pair<String, String>> infos = new ArrayList<>();
        historyConfTOs.forEach(historyConfTO -> infos.add(getJSONInfo(historyConfTO)));

        jsonPanel = new JsonDiffPanel(null, new PropertyModel<String>(infos.get(0), "value"),
                new PropertyModel<String>(infos.get(1), "value"), null) {

            private static final long serialVersionUID = -8927036362466990179L;

            @Override
            public void onSubmit(final AjaxRequestTarget target) {
                modal.close(target);
            }
        };

        replace(jsonPanel);
    }

    private Pair<String, String> getJSONInfo(final T historyConfTO) {
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

        return Pair.of(key, json);
    }

    private <T extends AbstractHistoryConf> Map<String, String> getDropdownNamesMap(final List<T> historyConfTOs) {
        Map<String, String> historyConfMap = new LinkedHashMap<>();

        String current = null;
        for (T historyConf : historyConfTOs) {
            if (historyConf.getCreation() == null) {
                current = historyConf.getKey();
            } else {
                historyConfMap.put(historyConf.getKey(), historyConf.getCreator() + " - "
                        + SyncopeConsoleSession.get().getDateFormat().format(
                                historyConf.getCreation()) + " - " + historyConf.getKey());
            }
        }
        if (current != null) {
            historyConfMap.put(current, getString("current"));
        }

        return historyConfMap;
    }

    private Form<?> initDropdownDiffConfForm() {
        final Form<T> form = new Form<>("form");
        form.setModel(new CompoundPropertyModel<>(selectedHistoryConfTO));
        form.setOutputMarkupId(true);

        final Map<String, String> namesMap = getDropdownNamesMap(availableHistoryConfTOs);
        List<String> keys = new ArrayList<>(namesMap.keySet());

        final AjaxDropDownChoicePanel<String> dropdownElem = new AjaxDropDownChoicePanel<>(
                "compareDropdown",
                getString("compare"),
                new PropertyModel<>(selectedHistoryConfTO, "key"),
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
        dropdownElem.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                List<T> elemsToCompare = new ArrayList<>();
                elemsToCompare.add(selectedHistoryConfTO);

                final String selectedKey = dropdownElem.getModelObject();
                if (selectedKey != null) {
                    if (!selectedKey.isEmpty()) {
                        T confToCompare = availableHistoryConfTOs.stream().
                                filter(object -> object.getKey().equals(selectedKey)).findAny().orElse(null);
                        elemsToCompare.add(confToCompare);
                        showConfigurationDiffPanel(elemsToCompare);
                    } else {
                        showConfigurationSinglePanel();
                    }
                }
                target.add(jsonPanel);
            }
        });
        form.add(dropdownElem);

        return form;
    }

    @SuppressWarnings("unchecked")
    private void addCurrentInstanceConf() {
        T conf = null;

        if (selectedHistoryConfTO instanceof ConnInstanceHistoryConfTO) {
            ConnInstanceTO current = new ConnectorRestClient().read(
                    ConnInstanceHistoryConfTO.class.cast(selectedHistoryConfTO).getConnInstanceTO().getKey());
            conf = (T) new ConnInstanceHistoryConfTO();
            ((ConnInstanceHistoryConfTO) conf).setConnInstanceTO(current);
        } else if (selectedHistoryConfTO instanceof ResourceHistoryConfTO) {
            ResourceTO current = new ResourceRestClient().read(
                    ResourceHistoryConfTO.class.cast(selectedHistoryConfTO).getResourceTO().getKey());
            conf = (T) new ResourceHistoryConfTO();
            ((ResourceHistoryConfTO) conf).setResourceTO(current);
        }

        if (conf != null) {
            conf.setCreator(selectedHistoryConfTO.getCreator());
            conf.setKey("current");
            availableHistoryConfTOs.add(conf);
        }
    }
}
