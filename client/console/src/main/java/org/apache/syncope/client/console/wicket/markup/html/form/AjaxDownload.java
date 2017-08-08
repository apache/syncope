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
package org.apache.syncope.client.console.wicket.markup.html.form;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.commons.HttpResourceStream;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.init.MIMETypesLoader;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;

public abstract class AjaxDownload extends AbstractAjaxBehavior {

    private static final long serialVersionUID = 7203445884857810583L;

    private static final MIMETypesLoader MIME_TYPES_LOADER = (MIMETypesLoader) SyncopeConsoleApplication.get().
            getServletContext().getAttribute(ConsoleInitializer.MIMETYPES_LOADER);

    private final String name;

    private String fileKey;

    private String mimeType;

    private final boolean addAntiCache;

    public AjaxDownload(final String name, final boolean addAntiCache) {
        super();
        this.name = name;
        this.addAntiCache = addAntiCache;
    }

    public AjaxDownload(final String name, final String fileKey, final String mimeType, final boolean addAntiCache) {
        this(name, addAntiCache);
        this.fileKey = fileKey;
        this.mimeType = mimeType;
    }

    public void initiate(final AjaxRequestTarget target) {

        String url = getCallbackUrl().toString();
        if (addAntiCache) {
            url = url + (url.contains("?") ? "&" : "?");
            url = url + "antiCache=" + System.currentTimeMillis();
        }
        target.appendJavaScript("setTimeout(\"window.location.href='" + url + "'\", 100);");
    }

    @Override
    public void onRequest() {
        HttpResourceStream stream = getResourceStream();
        ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(stream);
        String key = StringUtils.isNotBlank(fileKey) ? fileKey + "_" : "";
        String ext = "";
        if (StringUtils.isNotBlank(mimeType)) {
            String extByMimeType = MIME_TYPES_LOADER.getFileExt(mimeType);
            ext = StringUtils.isBlank(extByMimeType) ? ".bin" : ("." + extByMimeType);
        }
        String fileName = key + (stream.getFilename() == null ? name : stream.getFilename()) + ext;

        handler.setFileName(fileName);
        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

    protected abstract HttpResourceStream getResourceStream();

}
