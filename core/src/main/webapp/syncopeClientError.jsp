<%@page isErrorPage="true" session="false" contentType="application/json" pageEncoding="UTF-8"%>
<%@page import="org.syncope.types.EntityViolationType"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Map"%>
<%@page import="org.syncope.core.persistence.validation.entity.InvalidEntityException"%>
<%@page import="org.hibernate.exception.ConstraintViolationException"%>
<%@page import="javax.persistence.PersistenceException"%>
<%@page import="org.springframework.dao.DataIntegrityViolationException"%>
<%@page import="org.hibernate.exception.LockAcquisitionException"%>
<%@page import="org.syncope.core.rest.controller.InvalidSearchConditionException"%>
<%@page import="org.syncope.core.rest.controller.UnauthorizedRoleException"%>
<%@page import="org.syncope.core.persistence.dao.MissingConfKeyException"%>
<%@page import="org.syncope.client.validation.SyncopeClientException"%>
<%@page import="org.syncope.client.validation.SyncopeClientCompositeErrorException"%>
<%@page import="org.syncope.core.persistence.propagation.PropagationException"%>
<%@page import="org.syncope.core.workflow.WorkflowException"%>
<%@page import="org.syncope.types.SyncopeClientExceptionType"%>
<%@page import="org.syncope.client.validation.SyncopeClientErrorHandler"%>
<%@page import="javassist.NotFoundException"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.syncope.core.rest.controller.AbstractController"%>

<%!    static final Logger LOG =
            LoggerFactory.getLogger(AbstractController.class);%>

<%
    Throwable ex = pageContext.getErrorData().getThrowable();

    LOG.error("Exception thrown by REST methods", ex);

    int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    if (ex instanceof InvalidEntityException) {
        SyncopeClientExceptionType exType =
                SyncopeClientExceptionType.valueOf("Invalid"
                + ((InvalidEntityException) ex).getEntityClassSimpleName());

        response.setHeader(
                SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                exType.getHeaderValue());

        for (Map.Entry<Class, Set<EntityViolationType>> violation :
                ((InvalidEntityException) ex).getViolations().entrySet()) {

            for (EntityViolationType violationType : violation.getValue()) {
                response.addHeader(
                        exType.getElementHeaderName(),
                        violation.getClass().getSimpleName() + ": "
                        + violationType);
            }
        }

        statusCode = HttpServletResponse.SC_NOT_FOUND;
    } else if (ex instanceof NotFoundException) {
        response.setHeader(
                SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.NotFound.getHeaderValue());
        response.setHeader(
                SyncopeClientExceptionType.NotFound.getElementHeaderName(),
                ex.getMessage());

        statusCode = HttpServletResponse.SC_NOT_FOUND;
    } else if (ex instanceof WorkflowException) {
        if (ex.getCause().getCause() != null
                && ex.getCause().getCause().getCause() != null
                && ex.getCause().getCause().getCause() instanceof ConstraintViolationException) {

            response.setHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                    SyncopeClientExceptionType.DuplicateUniqueValue.
                    getHeaderValue());
        } else {
            response.setHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                    SyncopeClientExceptionType.Workflow.getHeaderValue());
            response.setHeader(
                    SyncopeClientExceptionType.Workflow.getElementHeaderName(),
                    ex.getCause().getMessage());
        }

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

        statusCode = ((SyncopeClientCompositeErrorException) ex).getStatusCode().
                value();
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
    } else if (ex instanceof UnauthorizedRoleException) {
        response.setHeader(
                SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.UnauthorizedRole.getHeaderValue());
        for (Long roleId : ((UnauthorizedRoleException) ex).getRoleIds()) {
            response.setHeader(
                    SyncopeClientExceptionType.UnauthorizedRole.
                    getElementHeaderName(), roleId.toString());
        }

        statusCode = HttpServletResponse.SC_UNAUTHORIZED;
    } else if (ex.getCause() instanceof LockAcquisitionException) {
        response.setHeader(
                SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.Deadlock.getHeaderValue());

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } else if (ex instanceof DataIntegrityViolationException) {
        response.setHeader(
                SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.DuplicateUniqueValue.getHeaderValue());

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } else if (ex instanceof PersistenceException) {
        if (ex.getCause() instanceof ConstraintViolationException) {
            response.setHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                    SyncopeClientExceptionType.DuplicateUniqueValue.
                    getHeaderValue());
        } else {
            response.setHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                    SyncopeClientExceptionType.GenericPersistence.getHeaderValue());
            response.setHeader(
                    SyncopeClientExceptionType.GenericPersistence.
                    getElementHeaderName(),
                    ex.getCause().getClass().getName());
        }

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    }

    response.setStatus(statusCode);
%>
null