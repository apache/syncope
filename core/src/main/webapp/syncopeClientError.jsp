<%@page isErrorPage="true" contentType="application/json" pageEncoding="UTF-8"%>
<%@page import="org.syncope.core.rest.data.InvalidSearchConditionException"%>
<%@page import="org.syncope.core.persistence.dao.MissingConfKeyException"%>
<%@page import="org.syncope.core.persistence.validation.MultiUniqueValueException"%>
<%@page import="org.syncope.client.validation.SyncopeClientException"%>
<%@page import="org.syncope.client.validation.SyncopeClientCompositeErrorException"%>
<%@page import="org.syncope.core.persistence.propagation.PropagationException"%>
<%@page import="com.opensymphony.workflow.WorkflowException"%>
<%@page import="org.syncope.types.SyncopeClientExceptionType"%>
<%@page import="org.syncope.client.validation.SyncopeClientErrorHandler"%>
<%@page import="javassist.NotFoundException"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.syncope.core.rest.controller.AbstractController"%>

<%!    static final Logger log =
            LoggerFactory.getLogger(AbstractController.class);%>

<%
            Throwable ex = pageContext.getErrorData().getThrowable();

            log.error("Exception thrown by REST methods", ex);

            int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

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

                statusCode = HttpServletResponse.SC_BAD_REQUEST;
            } else if (ex instanceof PropagationException) {
                response.setHeader(
                        SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                        SyncopeClientExceptionType.Propagation.getHeaderValue());
                response.setHeader(
                        SyncopeClientExceptionType.Propagation.getElementHeaderName(),
                        ((PropagationException) ex).getResourceName());

                statusCode = HttpServletResponse.SC_BAD_REQUEST;
            } else if (ex instanceof SyncopeClientCompositeErrorException) {
                for (SyncopeClientException sce :
                        ((SyncopeClientCompositeErrorException) ex).getExceptions()) {

                    response.addHeader(
                            SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                            sce.getType().getHeaderValue());

                    for (String attributeName : sce.getElements()) {
                        response.addHeader(
                                sce.getType().getElementHeaderName(),
                                attributeName);
                    }
                }

                statusCode = ((SyncopeClientCompositeErrorException) ex).getStatusCode().value();
            } else if (ex instanceof MultiUniqueValueException) {
                response.setHeader(
                        SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                        SyncopeClientExceptionType.InvalidSchemaDefinition.getHeaderValue());

                statusCode = HttpServletResponse.SC_BAD_REQUEST;
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

                statusCode = HttpServletResponse.SC_BAD_REQUEST;
            }

            response.setStatus(statusCode);
%>
null