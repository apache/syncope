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
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.search.SearchableFields;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

/**
 * Modal window with Display attributes form.
 *
 * @param <T> can be {@link AnyTO} or {@link org.apache.syncope.client.console.wizards.any.AnyWrapper}
 */
public abstract class DisplayAttributesModalPanel<T extends Serializable> extends AbstractModalPanel<T> {

    private static final long serialVersionUID = -4274117450918385110L;

    /**
     * Max allowed selections.
     */
    private static final int MAX_SELECTIONS = 9;

    private final PreferenceManager prefMan = new PreferenceManager();

    private final List<String> selectedDetails;

    private final List<String> selectedPlainSchemas;

    private final List<String> selectedDerSchemas;

    protected final String type;

    public DisplayAttributesModalPanel(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final List<String> schemaNames,
            final List<String> dSchemaNames) {
        this(modal, pageRef, schemaNames, dSchemaNames, null);
    }

    public DisplayAttributesModalPanel(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final List<String> pSchemaNames,
            final List<String> dSchemaNames,
            final String type) {

        super(modal, pageRef);
        this.type = type;

        final List<String> detailslList = SearchableFields.get(getTOClass());
        Collections.sort(detailslList);
        Collections.sort(pSchemaNames);
        Collections.sort(dSchemaNames);

        final IModel<List<String>> fnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return detailslList;
            }
        };

        final IModel<List<String>> psnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return pSchemaNames;
            }
        };

        final IModel<List<String>> dsnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return dSchemaNames;
            }
        };

        selectedDetails = prefMan.getList(getRequest(), getPrefDetailView());
        selectedPlainSchemas = prefMan.getList(getRequest(), getPrefPlainAttributeView());
        selectedDerSchemas = prefMan.getList(getRequest(), getPrefDerivedAttributeView());

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        AjaxPalettePanel<String> details = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("details",
                        new PropertyModel<List<String>>(this, "selectedDetails"),
                        new ListModel<>(fnames.getObject()));
        details.hideLabel();
        details.setOutputMarkupId(true);
        container.add(details);

        AjaxPalettePanel<String> plainSchemas = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("plainSchemas",
                        new PropertyModel<List<String>>(this, "selectedPlainSchemas"),
                        new ListModel<>(psnames.getObject()));
        plainSchemas.hideLabel();
        plainSchemas.setOutputMarkupId(true);
        container.add(plainSchemas);

        AjaxPalettePanel<String> derSchemas = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("derSchemas",
                        new PropertyModel<List<String>>(this, "selectedDerSchemas"),
                        new ListModel<>(dsnames.getObject()));
        derSchemas.hideLabel();
        derSchemas.setOutputMarkupId(true);
        container.add(derSchemas);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        if (selectedDetails.size() + selectedPlainSchemas.size() + selectedDerSchemas.size() > MAX_SELECTIONS) {
            SyncopeConsoleSession.get().error(getString("tooManySelections"));
            onError(target, form);
        } else {
            final Map<String, List<String>> prefs = new HashMap<>();

            prefs.put(getPrefDetailView(), selectedDetails);
            prefs.put(getPrefPlainAttributeView(), selectedPlainSchemas);
            prefs.put(getPrefDerivedAttributeView(), selectedDerSchemas);
            prefMan.setList(getRequest(), getResponse(), prefs);

            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }

    protected abstract String getPrefDetailView();

    protected abstract String getPrefPlainAttributeView();

    protected abstract String getPrefDerivedAttributeView();

    protected abstract Class<? extends AnyTO> getTOClass();

}
