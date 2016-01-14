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
package org.apache.syncope.client.console.pages;

import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.CamelRoutesRestClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.types.CamelEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class CamelRoutesPopupPage extends BasePopupPage {

    private static final long serialVersionUID = -1438441210568592931L;

    private final CamelRoutesRestClient restClient = new CamelRoutesRestClient();

    public CamelRoutesPopupPage(final CamelRouteTO routeTO) {
        super();

        Form<CamelRouteTO> routeDefForm = new Form<>("routeDefForm");

        TextArea<String> routeDefArea = new TextArea<>("content", new PropertyModel<String>(routeTO, "content"));
        routeDefForm.add(routeDefArea);
        routeDefForm.setModel(new CompoundPropertyModel<>(routeTO));

        AjaxButton submit = new IndicatingAjaxButton(APPLY, new Model<>(getString(SUBMIT)), routeDefForm) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    restClient.update(routeTO.getKey(), ((CamelRouteTO) form.getModelObject()).getContent());
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException scee) {
                    error(getString(Constants.ERROR) + ": " + scee.getMessage());
                }
                notificationPanel.refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                notificationPanel.refresh(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, CamelEntitlement.ROUTE_UPDATE);
        routeDefForm.add(submit);
        add(routeDefForm);
    }
}
