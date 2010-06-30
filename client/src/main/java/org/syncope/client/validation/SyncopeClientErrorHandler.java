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
    private static final Logger log = LoggerFactory.getLogger(
            SyncopeClientErrorHandler.class);
    public static final HttpStatus[] managedStatuses = new HttpStatus[]{
        HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND};

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if (!ArrayUtils.contains(managedStatuses, response.getStatusCode())) {
            super.handleError(response);
        }

        SyncopeClientCompositeErrorException compositeException =
                new SyncopeClientCompositeErrorException(
                response.getStatusCode());

        List<String> exceptionTypesInHeaders = response.getHeaders().get(
                EXCEPTION_TYPE_HEADER);
        SyncopeClientExceptionType exceptionType = null;
        SyncopeClientException clientException = null;
        Set<String> handledExceptions = new HashSet<String>();
        for (String exceptionTypeAsString : exceptionTypesInHeaders) {
            try {
                exceptionType = SyncopeClientExceptionType.getFromHeaderValue(
                        exceptionTypeAsString);
            } catch (IllegalArgumentException e) {
                log.error("Unexpected value of "
                        + EXCEPTION_TYPE_HEADER + ": " + exceptionTypeAsString,
                        e);
            }
            if (exceptionType != null) {
                handledExceptions.add(exceptionTypeAsString);

                clientException = new SyncopeClientException();
                clientException.setType(exceptionType);
                if (response.getHeaders().get(
                        exceptionType.getAttributeNameHeaderName()) != null
                        && !response.getHeaders().get(
                        exceptionType.getAttributeNameHeaderName()).isEmpty()) {

                    clientException.setAttributeNames(response.getHeaders().get(
                            exceptionType.getAttributeNameHeaderName()));
                }

                compositeException.addException(clientException);
            }
        }

        exceptionTypesInHeaders.removeAll(handledExceptions);
        if (!exceptionTypesInHeaders.isEmpty()) {
            log.error("Unmanaged exceptions: " + exceptionTypesInHeaders);
        }

        if (compositeException.hasExceptions()) {
            throw compositeException;
        }
    }
}
