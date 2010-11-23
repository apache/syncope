/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.client.validation;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.syncope.types.SyncopeClientExceptionType;

public class SyncopeClientErrorHandler extends DefaultResponseErrorHandler {

    public static final String EXCEPTION_TYPE_HEADER = "ExceptionType";

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SyncopeClientErrorHandler.class);

    public static final HttpStatus[] MANAGED_STATUSES = new HttpStatus[]{
        HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND};

    @Override
    public void handleError(final ClientHttpResponse response)
            throws IOException {

        if (!ArrayUtils.contains(MANAGED_STATUSES, response.getStatusCode())) {
            super.handleError(response);
        }

        List<String> exceptionTypesInHeaders = response.getHeaders().get(
                EXCEPTION_TYPE_HEADER);
        if (exceptionTypesInHeaders == null) {
            LOG.debug("No " + EXCEPTION_TYPE_HEADER + " provided");

            return;
        }

        SyncopeClientCompositeErrorException compositeException =
                new SyncopeClientCompositeErrorException(
                response.getStatusCode());

        SyncopeClientExceptionType exceptionType = null;
        SyncopeClientException clientException = null;
        Set<String> handledExceptions = new HashSet<String>();
        for (String exceptionTypeAsString : exceptionTypesInHeaders) {
            try {
                exceptionType = SyncopeClientExceptionType.getFromHeaderValue(
                        exceptionTypeAsString);
            } catch (IllegalArgumentException e) {
                LOG.error("Unexpected value of "
                        + EXCEPTION_TYPE_HEADER + ": " + exceptionTypeAsString,
                        e);
            }
            if (exceptionType != null) {
                handledExceptions.add(exceptionTypeAsString);

                clientException = new SyncopeClientException();
                clientException.setType(exceptionType);
                if (response.getHeaders().get(
                        exceptionType.getElementHeaderName()) != null
                        && !response.getHeaders().get(
                        exceptionType.getElementHeaderName()).isEmpty()) {

                    clientException.setElements(response.getHeaders().get(
                            exceptionType.getElementHeaderName()));
                }

                compositeException.addException(clientException);
            }
        }

        exceptionTypesInHeaders.removeAll(handledExceptions);
        if (!exceptionTypesInHeaders.isEmpty()) {
            LOG.error("Unmanaged exceptions: " + exceptionTypesInHeaders);
        }

        if (compositeException.hasExceptions()) {
            throw compositeException;
        }
    }
}
