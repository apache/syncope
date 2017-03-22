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

import static org.apache.syncope.client.console.panels.TogglePanel.LOG;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInputField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.SAML2IdPsRestClient;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

public class ImportMetadata extends TogglePanel<Serializable> {

    private static final long serialVersionUID = 6959177759869415782L;

    private final SAML2IdPsRestClient restClient = new SAML2IdPsRestClient();

    private final Form<?> form;

    public ImportMetadata(final String id, final WebMarkupContainer container, final PageReference pageRef) {
        super(id, pageRef);

        form = new Form<>("metadataForm");
        addInnerObject(form);

        final Model<byte[]> metadata = new Model<>();

        FileInputConfig config = new FileInputConfig();
        config.showUpload(false);
        config.showRemove(false);
        config.showPreview(false);
        final BootstrapFileInputField fileUpload =
                new BootstrapFileInputField("fileUpload", new ListModel<>(new ArrayList<FileUpload>()), config);
        fileUpload.setOutputMarkupId(true);
        fileUpload.add(new AjaxFormSubmitBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = 5538299138211283825L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                FileUpload uploadedFile = fileUpload.getFileUpload();
                if (uploadedFile != null) {
                    metadata.setObject(uploadedFile.getBytes());
                }
            }
        });
        form.add(fileUpload);

        form.add(new AjaxSubmitLink("doUpload", form) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                if (ArrayUtils.isNotEmpty(metadata.getObject())) {
                    try {
                        restClient.importIdPs(new ByteArrayInputStream(metadata.getObject()));
                        metadata.setObject(null);

                        SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                        toggle(target, false);
                        target.add(container);
                    } catch (Exception e) {
                        LOG.error("While importing SAML 2.0 IdP metadata", e);
                        SyncopeConsoleSession.get().error(
                                StringUtils.isBlank(e.getMessage())
                                ? e.getClass().getName() : e.getMessage());
                    }
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });
    }

}
