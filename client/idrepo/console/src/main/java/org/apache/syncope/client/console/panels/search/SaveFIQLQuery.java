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
package org.apache.syncope.client.console.panels.search;

import java.io.Serializable;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.FIQLQueryRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.to.FIQLQueryTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SaveFIQLQuery extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -1519998802447270269L;

    @SpringBean
    protected FIQLQueryRestClient fiqlQueryRestClient;

    protected String fiql;

    protected TextField<String> name;

    public SaveFIQLQuery(final String id, final String target, final PageReference pageRef) {
        super(id, pageRef);

        Form<?> form = new Form<>("form");
        addInnerObject(form);

        name = new TextField<>("name", new Model<>());
        form.add(name);

        form.add(new AjaxSubmitLink("submit", form) {

            private static final long serialVersionUID = -5697330186048290602L;

            @Override
            protected void onSubmit(final AjaxRequestTarget art) {
                try {
                    FIQLQueryTO query = new FIQLQueryTO();
                    query.setName(name.getModelObject());
                    query.setTarget(target);
                    query.setFiql(fiql);

                    fiqlQueryRestClient.create(query);

                    name.getModel().setObject(null);
                    name.setRequired(false);

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(art, false);
                } catch (Exception e) {
                    LOG.error("While creating new FIQL query", e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(art);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });
    }

    public void setFiql(final String fiql) {
        this.fiql = fiql;
        this.name.setRequired(true);
    }

    @Override
    public void toggle(final AjaxRequestTarget target, final boolean toggle) {
        if (toggle) {
            setHeader(target, getString("newFIQLQuery"));
        }
        super.toggle(target, toggle);
    }
}
