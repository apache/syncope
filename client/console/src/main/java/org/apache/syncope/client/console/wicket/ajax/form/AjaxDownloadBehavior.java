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
package org.apache.syncope.client.console.wicket.ajax.form;

import javax.ws.rs.core.Response;
import org.apache.syncope.client.console.SerializableSupplier;
import org.apache.syncope.client.console.commons.HttpResourceStream;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AjaxDownloadBehavior extends AbstractAjaxBehavior {

    private static final long serialVersionUID = 6833760760338614245L;

    protected static final Logger LOG = LoggerFactory.getLogger(AjaxDownloadBehavior.class);

    protected SerializableSupplier<Response> response;

    protected HttpResourceStream stream;

    /**
     * Call this method to initiate the download.
     *
     * @param target request target.
     */
    public void initiate(final AjaxRequestTarget target) {
        CharSequence url = getCallbackUrl();
        target.appendJavaScript("window.location.href='" + url + "'");
    }

    @Override
    public void onRequest() {
        try {
            getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(
                    new ResourceStreamRequestHandler(
                            getResourceStream(), getFileName()).setCacheDuration(Duration.NONE));
        } catch (Exception e) {
            // cannot be notifies beacause the use of scheduleRequestHandlerAfterCurrent
            LOG.error("Error downloading file", e);
        }
    }

    public boolean hasResponse() {
        return response != null;
    }

    public void setResponse(final SerializableSupplier<Response> response) {
        this.response = response;
    }

    private void createResourceStream() {
        if (stream == null && response != null) {
            stream = new HttpResourceStream(response.get());
            response = null;
        }
    }

    protected String getFileName() {
        createResourceStream();
        return stream == null ? null : stream.getFilename();
    }

    protected IResourceStream getResourceStream() {
        createResourceStream();
        return stream;
    }
}
