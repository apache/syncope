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
package org.apache.syncope.client.console.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonDiffPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class HistoryAuditDetails extends MultilevelPanel.SecondLevel {

    private static final String KEY_CURRENT = "current";

    private static final long serialVersionUID = -7400543686272100483L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AuditEntryTO selected;

    private final List<AuditEntryTO> availableTOs;

    private AbstractModalPanel<String> jsonPanel;

    private final AnyTO currentTO;

    public HistoryAuditDetails(
            final BaseModal<?> baseModal,
            final AuditEntryTO selected,
            final PageReference pageRef,
            final List<AuditEntryTO> availableTOs,
            final AnyTO currentTO) {

        super();
        this.availableTOs = availableTOs.stream().
                filter(object -> !selected.equals(object) && selected.getBefore() != null).
                collect(Collectors.toList());
        this.selected = selected;
        this.currentTO = currentTO;

        addCurrentInstanceConf();
        Form<?> form = initDropdownDiffConfForm();
        add(form);
        form.setVisible(!this.availableTOs.isEmpty());

        showConfigurationSinglePanel();
    }

    private void showConfigurationSinglePanel() {
        Pair<String, String> info = getJSONInfo(selected);

        jsonPanel = new JsonEditorPanel(null, new PropertyModel<>(info, "right"), true, null) {

            private static final long serialVersionUID = -8927036362466990179L;

            @Override
            public void onSubmit(final AjaxRequestTarget target) {
                modal.close(target);
            }
        };
        jsonPanel.setOutputMarkupId(true);

        addOrReplace(jsonPanel);
    }

    private void showConfigurationDiffPanel(final List<AuditEntryTO> entries) {
        List<Pair<String, String>> infos = new ArrayList<>();
        entries.forEach(entry -> infos.add(getJSONInfo(entry)));

        jsonPanel = new JsonDiffPanel(null, new PropertyModel<>(infos.get(0), "value"),
                new PropertyModel<>(infos.get(1), "value"), null) {

            private static final long serialVersionUID = -8927036362466990179L;

            @Override
            public void onSubmit(final AjaxRequestTarget target) {
                modal.close(target);
            }
        };

        replace(jsonPanel);
    }

    private String getSanitizedTOAsJSON(final AnyTO anyTO) throws Exception {
        if (anyTO instanceof UserTO) {
            UserTO userTO = (UserTO) anyTO;
            userTO.setPassword(null);
            userTO.setSecurityAnswer(null);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(userTO);
        }
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(anyTO);
    }

    private Pair<String, String> getJSONInfo(final AuditEntryTO auditEntryBean) {
        try {
            String content = auditEntryBean.getBefore() == null
                    ? MAPPER.readTree(auditEntryBean.getOutput()).get("entity").toPrettyString()
                    : auditEntryBean.getBefore();

            AnyTO anyTO = MAPPER.readValue(content, currentTO.getClass());
            String json = getSanitizedTOAsJSON(anyTO);
            return Pair.of(auditEntryBean.getKey(), json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T extends AuditEntryTO> Map<String, String> getDropdownNamesMap(final List<T> entries) {
        Map<String, String> map = new LinkedHashMap<>();
        entries.forEach(audit -> {
            String value = audit.getWho() + " - " + SyncopeConsoleSession.get().getDateFormat().format(audit.getDate());
            if (audit.getKey().equalsIgnoreCase(KEY_CURRENT)) {
                value += " - " + audit.getKey();
            }
            map.put(audit.getKey(), value);
        });
        return map;
    }

    private Form<?> initDropdownDiffConfForm() {
        final Form<AuditEntryTO> form = new Form<>("form");
        form.setModel(new CompoundPropertyModel<>(selected));
        form.setOutputMarkupId(true);

        Map<String, String> namesMap = getDropdownNamesMap(availableTOs);
        List<String> keys = new ArrayList<>(namesMap.keySet());

        final AjaxDropDownChoicePanel<String> dropdownElem = new AjaxDropDownChoicePanel<>(
                "compareDropdown",
                getString("compare"),
                new PropertyModel<>(selected, Constants.KEY_FIELD_NAME),
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
        dropdownElem.setNullValid(false);
        dropdownElem.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                List<AuditEntryTO> elemsToCompare = new ArrayList<>();
                elemsToCompare.add(selected);

                final String selectedKey = dropdownElem.getModelObject();
                if (selectedKey != null) {
                    if (!selectedKey.isEmpty()) {
                        AuditEntryTO confToCompare = availableTOs.stream().
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

    private void addCurrentInstanceConf() {
        try {
            AuditEntryTO entryBean = new AuditEntryTO();
            entryBean.setKey(KEY_CURRENT);
            entryBean.setWho(currentTO.getCreator());
            entryBean.setDate(currentTO.getCreationDate());
            entryBean.setBefore(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(currentTO));
            availableTOs.add(entryBean);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
