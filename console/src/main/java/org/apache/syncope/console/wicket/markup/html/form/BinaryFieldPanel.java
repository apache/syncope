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
package org.apache.syncope.console.wicket.markup.html.form;

import java.io.ByteArrayInputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.HttpResourceStream;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.crypt.Base64;
import org.apache.wicket.util.lang.Bytes;

public class BinaryFieldPanel extends FieldPanel<String> {

    private static final long serialVersionUID = 6264462604183088931L;

    private final String mimeType;

    public BinaryFieldPanel(final String id, final String name, final IModel<String> model, final String mimeType) {
        super(id, model);
        this.mimeType = mimeType;

        field = new TextField<String>("textField", model);
        add(field.setLabel(new Model<String>(name)).setOutputMarkupId(true));

        add(new Label("preview", StringUtils.isBlank(mimeType) ? StringUtils.EMPTY : "(" + mimeType + ")"));

        Link<Void> downloadLink = new Link<Void>("downloadLink") {

            private static final long serialVersionUID = -4331619903296515985L;

            @Override
            public void onClick() {
                try {
                    HttpResourceStream stream = new HttpResourceStream(buildResponse());

                    ResourceStreamRequestHandler rsrh = new ResourceStreamRequestHandler(stream);
                    rsrh.setFileName(stream.getFilename() == null ? name : stream.getFilename());
                    rsrh.setContentDisposition(ContentDisposition.ATTACHMENT);

                    getRequestCycle().scheduleRequestHandlerAfterCurrent(rsrh);
                } catch (Exception e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                }
            }

        };
        add(downloadLink);

        Form<?> uploadForm = new StatelessForm<Void>("uploadForm");
        uploadForm.setMultiPart(true);
        uploadForm.setMaxSize(Bytes.megabytes(4));
        add(uploadForm);

        @SuppressWarnings("unchecked")
        final FileUploadField fileUpload = new FileUploadField("fileUpload", new Model());
        fileUpload.setOutputMarkupId(true);
        fileUpload.add(new AjaxFormSubmitBehavior(Constants.ON_BLUR) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                final FileUpload uploadedFile = fileUpload.getFileUpload();
                if (uploadedFile != null) {
                    try {
                        final String uploaded = new String(
                                Base64.encodeBase64(uploadedFile.getBytes()),
                                SyncopeConstants.DEFAULT_ENCODING);
                        field.setModelObject(uploaded);
                        target.add(field);

                        fileUpload.setModelObject(null);
                        target.add(fileUpload);
                    } catch (Exception e) {
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        LOG.error("While saving uploaded file", e);
                    }
                }
            }
        });

        uploadForm.add(fileUpload);

        IndicatingAjaxLink<Void> uploadLink = new IndicatingAjaxLink<Void>("uploadLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
            }
        };
        add(uploadLink);

        IndicatingAjaxLink<Void> resetLink = new IndicatingAjaxLink<Void>("resetLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                field.setModelObject(null);
                target.add(field);
            }
        };
        add(resetLink);
    }

    private Response buildResponse() {
        return Response.ok(new ByteArrayInputStream(Base64.decodeBase64(getModelObject()))).
                type(StringUtils.isBlank(mimeType) ? MediaType.APPLICATION_OCTET_STREAM : mimeType).build();
    }

    @Override
    public BinaryFieldPanel clone() {
        return (BinaryFieldPanel) super.clone();
    }

}
