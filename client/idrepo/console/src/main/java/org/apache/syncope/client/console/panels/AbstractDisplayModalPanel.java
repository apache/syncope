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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public abstract class AbstractDisplayModalPanel<T extends Serializable> extends AbstractModalPanel<T> {

    private static final long serialVersionUID = 4692227252045461088L;

    private static final int MAX_SELECTIONS = 9;

    private final String detailsPreferenceKey;

    private final String plainAttrsPreferenceKey;

    private final String derAttrsPreferenceKey;

    protected final List<String> selectedDetails;

    protected final List<String> selectedPlainSchemas;

    protected final List<String> selectedDerSchemas;

    protected AbstractDisplayModalPanel(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final List<String> availableDetails,
            final List<String> availablePlainSchemas,
            final List<String> availableDerSchemas,
            final String detailsPreferenceKey,
            final String plainAttrsPreferenceKey,
            final String derAttrsPreferenceKey) {

        super(modal, pageRef);

        this.detailsPreferenceKey = detailsPreferenceKey;
        this.plainAttrsPreferenceKey = plainAttrsPreferenceKey;
        this.derAttrsPreferenceKey = derAttrsPreferenceKey;

        selectedDetails = PreferenceManager.getList(this.detailsPreferenceKey);
        selectedDetails.retainAll(availableDetails);

        selectedPlainSchemas = PreferenceManager.getList(this.plainAttrsPreferenceKey);
        selectedPlainSchemas.retainAll(availablePlainSchemas);

        selectedDerSchemas = PreferenceManager.getList(this.derAttrsPreferenceKey);
        selectedDerSchemas.retainAll(availableDerSchemas);

        WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        AjaxPalettePanel<String> details = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("details", new PropertyModel<>(this, "selectedDetails"), new ListModel<>(availableDetails));
        details.hideLabel();
        details.setOutputMarkupId(true);
        container.add(details);

        AjaxPalettePanel<String> plainSchemas = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("plainSchemas", new PropertyModel<>(this, "selectedPlainSchemas"),
                        new ListModel<>(availablePlainSchemas));
        plainSchemas.hideLabel();
        plainSchemas.setOutputMarkupId(true);
        container.add(plainSchemas);

        AjaxPalettePanel<String> derSchemas = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("derSchemas", new PropertyModel<>(this, "selectedDerSchemas"),
                        new ListModel<>(availableDerSchemas));
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
            prefs.put(detailsPreferenceKey, selectedDetails);
            prefs.put(plainAttrsPreferenceKey, selectedPlainSchemas);
            prefs.put(derAttrsPreferenceKey, selectedDerSchemas);
            PreferenceManager.setList(prefs);

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }
}
