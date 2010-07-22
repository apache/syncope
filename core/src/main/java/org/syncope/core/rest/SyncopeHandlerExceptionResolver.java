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
package org.syncope.core.rest;

import com.opensymphony.workflow.WorkflowException;
import java.io.IOException;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientErrorHandler;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.propagation.PropagationException;
import org.syncope.core.persistence.validation.MultiUniqueValueException;
import org.syncope.core.rest.data.InvalidSearchConditionException;
import org.syncope.types.SyncopeClientExceptionType;

public class SyncopeHandlerExceptionResolver
        extends DefaultHandlerExceptionResolver {

    @Override
    protected ModelAndView doResolveException(HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {

        ModelAndView defaultResolution =
                super.doResolveException(request, response, handler, ex);
        if (defaultResolution != null) {
            return defaultResolution;
        }

        logger.error("Unexpected exception", ex);

        int statusCode = HttpServletResponse.SC_BAD_REQUEST;

        if (ex instanceof NotFoundException) {
            response.setHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                    SyncopeClientExceptionType.NotFound.getHeaderValue());
            response.setHeader(
                    SyncopeClientExceptionType.NotFound.getElementHeaderName(),
                    ex.getMessage());

            statusCode = HttpServletResponse.SC_NOT_FOUND;
        } else if (ex instanceof WorkflowException) {
            response.setHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                    SyncopeClientExceptionType.Workflow.getHeaderValue());
            response.setHeader(
                    SyncopeClientExceptionType.Workflow.getElementHeaderName(),
                    ex.getMessage());
        } else if (ex instanceof PropagationException) {
            response.setHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                    SyncopeClientExceptionType.Propagation.getHeaderValue());
            response.setHeader(
                    SyncopeClientExceptionType.Propagation.getElementHeaderName(),
                    ((PropagationException) ex).getResource());
        } else if (ex instanceof SyncopeClientCompositeErrorException) {
            for (SyncopeClientException exception :
                    ((SyncopeClientCompositeErrorException) ex).getExceptions()) {

                response.addHeader(
                        SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                        exception.getType().getHeaderValue());

                for (String attributeName : exception.getElements()) {
                    response.addHeader(
                            exception.getType().getElementHeaderName(),
                            attributeName);
                }
            }

            statusCode = ((SyncopeClientCompositeErrorException) ex).getStatusCode().value();
        } else if (ex instanceof MultiUniqueValueException) {
            response.setHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                    SyncopeClientExceptionType.InvalidSchemaDefinition.getHeaderValue());
        } else if (ex instanceof MissingConfKeyException) {
            response.setHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                    SyncopeClientExceptionType.NotFound.getHeaderValue());
            response.setHeader(
                    SyncopeClientExceptionType.NotFound.getElementHeaderName(),
                    ((MissingConfKeyException) ex).getConfKey());

            statusCode = HttpServletResponse.SC_NOT_FOUND;
        } else if (ex instanceof InvalidSearchConditionException) {
            response.setHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                    SyncopeClientExceptionType.InvalidSearchCondition.getHeaderValue());
        }

        try {
            response.sendError(statusCode);
        } catch (IOException ioe) {
            logger.warn("Handling of [" + ex.getClass().getName()
                    + "] resulted in Exception", ioe);
        }

        return new ModelAndView();
    }
}
