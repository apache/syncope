<%--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
--%>
<%@page isErrorPage="true" session="false" contentType="application/json" pageEncoding="UTF-8"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Map"%>
<%@page import="javax.persistence.EntityExistsException"%>
<%@page import="javax.persistence.PersistenceException"%>
<%@page import="org.apache.syncope.common.types.EntityViolationType"%>
<%@page import="org.apache.syncope.core.persistence.validation.entity.InvalidEntityException"%>
<%@page import="org.apache.syncope.common.services.InvalidSearchConditionException"%>
<%@page import="org.apache.syncope.core.rest.controller.UnauthorizedRoleException"%>
<%@page import="org.apache.syncope.core.persistence.dao.MissingConfKeyException"%>
<%@page import="org.apache.syncope.common.validation.SyncopeClientException"%>
<%@page import="org.apache.syncope.common.validation.SyncopeClientCompositeErrorException"%>
<%@page import="org.apache.syncope.core.propagation.PropagationException"%>
<%@page import="org.apache.syncope.core.workflow.WorkflowException"%>
<%@page import="org.apache.syncope.common.types.SyncopeClientExceptionType"%>
<%@page import="org.apache.syncope.core.persistence.dao.NotFoundException"%>
<%@page import="org.identityconnectors.framework.common.exceptions.ConfigurationException"%>
<%@page import="org.apache.syncope.common.validation.SyncopeClientErrorHandler"%>
<%@page import="org.apache.syncope.core.rest.controller.AbstractController"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.springframework.dao.DataIntegrityViolationException"%>
<%@page import="org.springframework.orm.jpa.JpaSystemException"%>

<%!    static final Logger LOG = LoggerFactory.getLogger(AbstractController.class);%>

<%
    Throwable ex = pageContext.getErrorData().getThrowable();

    LOG.error("Exception thrown by REST methods", ex);

    int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    if (ex instanceof InvalidEntityException) {
        SyncopeClientExceptionType exType = SyncopeClientExceptionType.valueOf(
                "Invalid" + ((InvalidEntityException) ex).getEntityClassSimpleName());

        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER, exType.getHeaderValue());

        for (Map.Entry<Class, Set<EntityViolationType>> violation : ((InvalidEntityException) ex).getViolations().
                entrySet()) {

            for (EntityViolationType violationType : violation.getValue()) {
                response.addHeader(exType.getElementHeaderName(),
                        violationType.name() + ": " + violationType.getMessage());
            }
        }

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } else if (ex instanceof NotFoundException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.NotFound.getHeaderValue());
        response.setHeader(SyncopeClientExceptionType.NotFound.getElementHeaderName(), ex.getMessage());

        statusCode = HttpServletResponse.SC_NOT_FOUND;
    } else if (ex instanceof EntityExistsException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.EntityExists.getHeaderValue());
        response.setHeader(SyncopeClientExceptionType.EntityExists.getElementHeaderName(), ex.getMessage());

        statusCode = HttpServletResponse.SC_CONFLICT;
    } else if (ex instanceof WorkflowException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.Workflow.getHeaderValue());
        response.setHeader(SyncopeClientExceptionType.Workflow.getElementHeaderName(), ex.getCause().getMessage());

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } else if (ex instanceof org.apache.ibatis.exceptions.PersistenceException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.Workflow.getHeaderValue());
        response.setHeader(
                SyncopeClientExceptionType.Workflow.getElementHeaderName(), "Currently unavailable. Please try later.");

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } else if (ex instanceof PropagationException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.Propagation.getHeaderValue());
        response.setHeader(SyncopeClientExceptionType.Propagation.getElementHeaderName(),
                ((PropagationException) ex).getResourceName());

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } else if (ex instanceof SyncopeClientCompositeErrorException) {
        for (SyncopeClientException sce : ((SyncopeClientCompositeErrorException) ex).getExceptions()) {
            response.addHeader(
                    SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER, sce.getType().getHeaderValue());

            for (String attributeName : sce.getElements()) {
                response.addHeader(sce.getType().getElementHeaderName(), attributeName);
            }
        }

        statusCode = ((SyncopeClientCompositeErrorException) ex).getStatusCode().value();
    } else if (ex instanceof MissingConfKeyException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.NotFound.getHeaderValue());
        response.setHeader(SyncopeClientExceptionType.NotFound.getElementHeaderName(),
                ((MissingConfKeyException) ex).getConfKey());

        statusCode = HttpServletResponse.SC_NOT_FOUND;
    } else if (ex instanceof InvalidSearchConditionException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.InvalidSearchCondition.getHeaderValue());

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } else if (ex instanceof UnauthorizedRoleException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.UnauthorizedRole.getHeaderValue());
        response.setHeader(SyncopeClientExceptionType.UnauthorizedRole.getElementHeaderName(), ex.getMessage());

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } else if (ex instanceof DataIntegrityViolationException || ex instanceof JpaSystemException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.DataIntegrityViolation.getHeaderValue());
        response.setHeader(SyncopeClientExceptionType.DataIntegrityViolation.getElementHeaderName(),
                ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage());

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } else if (ex instanceof PersistenceException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.GenericPersistence.getHeaderValue());
        response.setHeader(SyncopeClientExceptionType.GenericPersistence.getElementHeaderName(),
                ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage());

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } else if (ex instanceof ConfigurationException) {
        response.setHeader(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
                SyncopeClientExceptionType.InvalidConnIdConf.getHeaderValue());
        response.setHeader(SyncopeClientExceptionType.InvalidConnIdConf.getElementHeaderName(),
                ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage());

        statusCode = HttpServletResponse.SC_BAD_REQUEST;
    }

    response.setStatus(statusCode);
%>
null