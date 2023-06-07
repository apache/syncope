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

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInputField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.SAML2IdPsRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ImportMetadata extends TogglePanel<Serializable> {

    private static final long serialVersionUID = 6959177759869415782L;

    @SpringBean
    protected SAML2IdPsRestClient saml2IdPsRestClient;
    
    public ImportMetadata(final String id, final WebMarkupContainer container, final PageReference pageRef) {
        super(id, pageRef);

        Form<?> form = new Form<>("metadataForm");
        addInnerObject(form);

        FileInputConfig config = new FileInputConfig().
                showUpload(false).showRemove(false).showPreview(false).
                browseClass("btn btn-success").browseIcon("<i class=\"fas fa-folder-open\"></i> &nbsp;");
        String language = SyncopeConsoleSession.get().getLocale().getLanguage();
        if (!Locale.ENGLISH.getLanguage().equals(language)) {
            config.withLocale(language);
        }
        BootstrapFileInputField fileUpload =
                new BootstrapFileInputField("fileUpload", new ListModel<>(new ArrayList<>()), config);
        form.add(fileUpload.setOutputMarkupId(true));

        form.add(new AjaxSubmitLink("doUpload", form) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                FileUpload uploaded = fileUpload.getFileUpload();
                if (uploaded != null) {
                    try {
                        saml2IdPsRestClient.importIdPs(uploaded.getInputStream());

                        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        toggle(target, false);
                        target.add(container);
                    } catch (Exception e) {
                        LOG.error("While importing SAML 2.0 IdP metadata", e);
                        SyncopeConsoleSession.get().onException(e);
                    }
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });
    }
}
