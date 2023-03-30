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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.search.SearchableFields;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

/**
 * Modal window with Display attributes form.
 *
 * @param <T> can be {@link AnyTO} or {@link org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper}
 */
public abstract class DisplayAttributesModalPanel<T extends Serializable> extends AbstractModalPanel<T> {

    private static final long serialVersionUID = -4274117450918385110L;

    /**
     * Max allowed selections.
     */
    private static final int MAX_SELECTIONS = 9;

    private final List<String> selectedDetails;

    private final List<String> selectedPlainSchemas;

    private final List<String> selectedDerSchemas;

    protected final String type;

    public DisplayAttributesModalPanel(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final List<String> pSchemaNames,
            final List<String> dSchemaNames,
            final String type) {

        super(modal, pageRef);
        this.type = type;

        Collections.sort(pSchemaNames);
        Collections.sort(dSchemaNames);

        final IModel<List<String>> fnames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SearchableFields.get(DisplayAttributesModalPanel.getTOClass(type))
                        .keySet().stream().sorted().collect(Collectors.toList());
            }
        };

        IModel<List<String>> psnames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return pSchemaNames;
            }
        };

        IModel<List<String>> dsnames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return dSchemaNames;
            }
        };

        selectedDetails = PreferenceManager.getList(DisplayAttributesModalPanel.getPrefDetailView(type));
        selectedPlainSchemas = PreferenceManager.getList(DisplayAttributesModalPanel.getPrefPlainAttributeView(type));
        selectedDerSchemas = PreferenceManager.getList(DisplayAttributesModalPanel.getPrefDerivedAttributeView(type));

        // remove old schemas from selected lists
        selectedPlainSchemas.retainAll(pSchemaNames);
        selectedDerSchemas.retainAll(dSchemaNames);

        WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        AjaxPalettePanel<String> details = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("details",
                        new PropertyModel<>(this, "selectedDetails"),
                        new ListModel<>(fnames.getObject()));
        details.hideLabel();
        details.setOutputMarkupId(true);
        container.add(details);

        AjaxPalettePanel<String> plainSchemas = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("plainSchemas",
                        new PropertyModel<>(this, "selectedPlainSchemas"),
                        new ListModel<>(psnames.getObject()));
        plainSchemas.hideLabel();
        plainSchemas.setOutputMarkupId(true);
        container.add(plainSchemas);

        AjaxPalettePanel<String> derSchemas = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("derSchemas",
                        new PropertyModel<>(this, "selectedDerSchemas"),
                        new ListModel<>(dsnames.getObject()));
        derSchemas.hideLabel();
        derSchemas.setOutputMarkupId(true);
        container.add(derSchemas);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        if (selectedDetails.size() + selectedPlainSchemas.size() + selectedDerSchemas.size() > MAX_SELECTIONS) {
            SyncopeConsoleSession.get().error(getString("tooManySelections"));
            onError(target);
        } else {
            Map<String, List<String>> prefs = new HashMap<>();
            prefs.put(DisplayAttributesModalPanel.getPrefDetailView(type), selectedDetails);
            prefs.put(DisplayAttributesModalPanel.getPrefPlainAttributeView(type), selectedPlainSchemas);
            prefs.put(DisplayAttributesModalPanel.getPrefDerivedAttributeView(type), selectedDerSchemas);
            PreferenceManager.setList(prefs);

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }

    public static final String getPrefDetailView(final String type) {
        return String.format(Constants.PREF_ANY_DETAILS_VIEW, type);
    }

    public static final String getPrefPlainAttributeView(final String type) {
        return String.format(Constants.PREF_ANY_PLAIN_ATTRS_VIEW, type);
    }

    public static final String getPrefDerivedAttributeView(final String type) {
        return String.format(Constants.PREF_ANY_DER_ATTRS_VIEW, type);
    }

    public static final Class<? extends AnyTO> getTOClass(final String type) {
        if (type.equalsIgnoreCase(AnyTypeKind.USER.name())) {
            return UserTO.class;
        }
        if (type.equalsIgnoreCase(AnyTypeKind.GROUP.name())) {
            return GroupTO.class;
        }
        return AnyObjectTO.class;
    }
}
