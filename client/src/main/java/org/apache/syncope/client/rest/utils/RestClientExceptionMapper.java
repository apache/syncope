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
package org.apache.syncope.client.rest.utils;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.http.HttpStatus;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientErrorHandler;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class RestClientExceptionMapper implements ExceptionMapper<Exception>,
		ResponseExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientExceptionMapper.class);

    @Override
	public Response toResponse(Exception e) {
		throw new UnsupportedOperationException("Call of toResponse() method is not expected in RestClientExceptionnMapper");
	}

	@Override
	public Exception fromResponse(Response response) {
		Exception ex = null;
		int statusCode = response.getStatus();
		
		// 1. Check for composite exception in HTTP header 
		SyncopeClientCompositeErrorException scce = checkCompositeException(response);
		if (scce != null) {
			ex = scce;
			
			// TODO reduce SCCEE to really composite ones and use normal exception for others
//		} else if (statusCode == HttpStatus.SC_FORBIDDEN) {
//			ex = new UnauthorizedRoleException(-1L);
			
			// 2. Map  SC_UNAUTHORIZED
		} else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
			ex = new AccessControlException("Remote unauthorized exception");
			
		} else {
			// 3. All other codes are mapped to runtime exception with HTTP code information 
			ex = new RuntimeException(String.format(
					"Remote exception with status code: %s",
					Response.Status.fromStatusCode(statusCode).name()));
		}
        LOG.error("Exception thrown by REST methods: " + ex.getMessage(), ex);
		return ex;
	}
	
	private SyncopeClientCompositeErrorException checkCompositeException(Response response) {
		int statusCode = response.getStatus();
        List<Object> exceptionTypesInHeaders = response.getHeaders().get(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER);
        if (exceptionTypesInHeaders == null) {
            LOG.debug("No " + SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER + " provided");
            return null;
        }

		SyncopeClientCompositeErrorException compositeException = new SyncopeClientCompositeErrorException(
				org.springframework.http.HttpStatus.valueOf(statusCode));

        Set<String> handledExceptions = new HashSet<String>();
        for (Object exceptionTypeValue : exceptionTypesInHeaders) {
        	String exceptionTypeAsString = (String) exceptionTypeValue; 
            SyncopeClientExceptionType exceptionType = null;
            try {
                exceptionType = SyncopeClientExceptionType.getFromHeaderValue(exceptionTypeAsString);
            } catch (IllegalArgumentException e) {
                LOG.error("Unexpected value of " + SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER + ": " + exceptionTypeAsString, e);
            }
            if (exceptionType != null) {
                handledExceptions.add(exceptionTypeAsString);

                SyncopeClientException clientException = new SyncopeClientException();
                clientException.setType(exceptionType);
                if (response.getHeaders().get(exceptionType.getElementHeaderName()) != null
                        && !response.getHeaders().get(exceptionType.getElementHeaderName()).isEmpty()) {
                	// TODO: update clientException to support list of objects
                	List<Object> elementsObjectList = response.getHeaders().get(exceptionType.getElementHeaderName());
                	List<String> elementsStringList = new ArrayList<String>();
                	for (Object elementObject : elementsObjectList) {
                		if (elementObject instanceof String) {
                			elementsStringList.add((String) elementObject);
                		}
                	}
                    clientException.setElements(elementsStringList);
                }
                compositeException.addException(clientException);
            }
        }

        exceptionTypesInHeaders.removeAll(handledExceptions);
        if (!exceptionTypesInHeaders.isEmpty()) {
            LOG.error("Unmanaged exceptions: " + exceptionTypesInHeaders);
        }

        if (compositeException.hasExceptions()) {
            return compositeException;
        }
        
        return null;
	}
}
