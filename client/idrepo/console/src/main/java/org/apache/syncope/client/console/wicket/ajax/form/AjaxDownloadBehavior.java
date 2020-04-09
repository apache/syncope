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

import java.time.Duration;
import org.apache.syncope.client.ui.commons.HttpResourceStream;
import org.apache.syncope.client.ui.commons.rest.ResponseHolder;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AjaxDownloadBehavior extends AbstractAjaxBehavior {

    private static final long serialVersionUID = 6833760760338614245L;

    protected static final Logger LOG = LoggerFactory.getLogger(AjaxDownloadBehavior.class);

    protected ResponseHolder responseHolder;

    public void setResponse(final ResponseHolder response) {
        this.responseHolder = response;
    }

    /**
     * Call this method to initiate the download.
     *
     * @param target request target.
     */
    public void initiate(final AjaxRequestTarget target) {
        CharSequence url = getCallbackUrl();
        target.appendJavaScript("window.location.href='" + url + "'");
    }

    protected HttpResourceStream getResourceStream() {
        HttpResourceStream stream = null;

        if (responseHolder != null) {
            stream = new HttpResourceStream(responseHolder);
            responseHolder = null;
        }

        return stream;
    }

    @Override
    public void onRequest() {
        try {
            HttpResourceStream resourceStream = getResourceStream();
            if (resourceStream != null) {
                getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(
                        new ResourceStreamRequestHandler(
                                resourceStream, resourceStream.getFilename()).setCacheDuration(Duration.ZERO));
            }
        } catch (Exception e) {
            // cannot be notifies beacause the use of scheduleRequestHandlerAfterCurrent
            LOG.error("Error downloading file", e);
        }
    }
}
