package org.apache.syncope.core.rest;

import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.PersistenceException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.syncope.NotFoundException;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.exceptions.InvalidSearchConditionException;
import org.apache.syncope.exceptions.UnauthorizedRoleException;
import org.apache.syncope.propagation.PropagationException;
import org.apache.syncope.types.EntityViolationType;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.InvalidEntityException;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.validation.SyncopeClientErrorHandler;
import org.apache.syncope.validation.SyncopeClientException;
import org.apache.syncope.workflow.WorkflowException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;

@Provider
public class RestServiceExceptionMapper implements ExceptionMapper<Exception>,
		ResponseExceptionMapper<Exception> {

	private static final String BASIC_REALM_UNAUTHORIZED = "Basic realm=\"Spring Security Application\"";
	private static final Logger LOG = LoggerFactory
			.getLogger(RestServiceExceptionMapper.class);
	public static final String EXCEPTION_TYPE_HEADER = "ExceptionType";

	@Override
	public Response toResponse(Exception ex) {

		LOG.error("Exception thrown by REST methods: " + ex.getMessage(), ex);

		// 1. Process SyncopeClientCompositeErrorException
		Response response = processCompositeExceptions(ex);
		if (response != null) {
			return response;
		}

		// 2. Process Bad Requests
		response = processBadRequestExceptions(ex);
		if (response != null) {
			return response;
		}

		// 3. Process Unauthorized
		response = processUnauthorizedExceptions(ex);
		if (response != null) {
			return response;
		}

		// 4. Process Forbidden
		response = processForbiddenExceptions(ex);
		if (response != null) {
			return response;
		}

		// 4. Process NotFound
		response = processNotFoundExceptions(ex);
		if (response != null) {
			return response;
		}

		// 5. Process Conflict
		response = processConflictExceptions(ex);
		if (response != null) {
			return response;
		}

		// 5. Process InternalServerError
		response = processServerErrorExceptions(ex);
		if (response != null) {
			return response;
		}

		// 6. Rest is interpreted as InternalServerError
		ResponseBuilder responseBuilder = Response
				.status(Response.Status.INTERNAL_SERVER_ERROR);
		responseBuilder.header(SyncopeClientExceptionType.Unknown
				.getElementHeaderName(),
				ex.getCause() == null ? ex.getMessage() : ex.getCause()
						.getMessage());

		return responseBuilder.build();
	}

	@Override
	public Exception fromResponse(Response r) {
		throw new UnsupportedOperationException(
				"Call of fromResponse() method is not expected in RestServiceExceptionMapper");
	}

	private Response processCompositeExceptions(Exception ex) {
		Response response = null;

		if (ex instanceof SyncopeClientCompositeErrorException) {
			ResponseBuilder responseBuilder = Response
					.status(((SyncopeClientCompositeErrorException) ex)
							.getStatusCode().value());
			for (SyncopeClientException sce : ((SyncopeClientCompositeErrorException) ex)
					.getExceptions()) {
				responseBuilder.header(EXCEPTION_TYPE_HEADER, sce.getType()
						.getHeaderValue());

				for (String attributeName : sce.getElements()) {
					responseBuilder
							.header(sce.getType().getElementHeaderName(),
									attributeName);
				}
			}
			response = responseBuilder.build();
		}
		return response;
	}

	private Response processForbiddenExceptions(Exception ex) {
		Response response = null;
		ResponseBuilder responseBuilder = Response.status(Response.Status.FORBIDDEN);

		if (ex instanceof UnauthorizedRoleException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.UnauthorizedRole.getHeaderValue());
			responseBuilder.header(SyncopeClientExceptionType.UnauthorizedRole
					.getElementHeaderName(), ex.getMessage());
			response = responseBuilder.build();
		}
		return response;
	}

	private Response processUnauthorizedExceptions(Exception ex) {
		Response response = null;
		ResponseBuilder responseBuilder = Response.status(Response.Status.UNAUTHORIZED);

		if (ex instanceof org.springframework.security.access.AccessDeniedException) {
			response = responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE, BASIC_REALM_UNAUTHORIZED).build();			
		}
		return response;
	}

	private Response processConflictExceptions(Exception ex) {
		Response response = null;
		ResponseBuilder responseBuilder = Response.status(Response.Status.CONFLICT);

		if (ex instanceof DataIntegrityViolationException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.DataIntegrityViolation
							.getHeaderValue());
			responseBuilder.header(
					SyncopeClientExceptionType.DataIntegrityViolation
							.getElementHeaderName(),
					ex.getCause() == null ? ex.getMessage() : ex.getCause()
							.getMessage());
			response = responseBuilder.build();
		}
		return response;
	}

	private Response processServerErrorExceptions(Exception ex) {
		Response response = null;
		ResponseBuilder responseBuilder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);

		if (ex instanceof org.apache.ibatis.exceptions.PersistenceException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.Workflow.getHeaderValue());
			responseBuilder.header(
					SyncopeClientExceptionType.Workflow.getElementHeaderName(),
					"Currently unavailable. Please try later.");
			response = responseBuilder.build();

		} else if (ex instanceof JpaSystemException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.DataIntegrityViolation
							.getHeaderValue());
			responseBuilder.header(
					SyncopeClientExceptionType.DataIntegrityViolation
							.getElementHeaderName(),
					ex.getCause() == null ? ex.getMessage() : ex.getCause()
							.getMessage());
			response = responseBuilder.build();

		} else if (ex instanceof PersistenceException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.GenericPersistence
							.getHeaderValue());
			responseBuilder.header(
					SyncopeClientExceptionType.GenericPersistence
							.getElementHeaderName(),
					ex.getCause() == null ? ex.getMessage() : ex.getCause()
							.getMessage());
			response = responseBuilder.build();

		} else if (ex instanceof ConfigurationException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.InvalidExternalResource
							.getHeaderValue());
			responseBuilder.header(
					SyncopeClientExceptionType.InvalidExternalResource
							.getElementHeaderName(),
					ex.getCause() == null ? ex.getMessage() : ex.getCause()
							.getMessage());
			response = responseBuilder.build();
		}

		return response;
	}

	private Response processNotFoundExceptions(Exception ex) {
		Response response = null;
		ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);

		if (ex instanceof NotFoundException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.NotFound.getHeaderValue());
			responseBuilder.header(
					SyncopeClientExceptionType.NotFound.getElementHeaderName(),
					ex.getMessage());
			response = responseBuilder.build();

		} else if (ex instanceof MissingConfKeyException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.NotFound.getHeaderValue());
			responseBuilder.header(
					SyncopeClientExceptionType.NotFound.getElementHeaderName(),
					((MissingConfKeyException) ex).getConfKey());
			response = responseBuilder.build();
		}

		return response;
	}

	private Response processBadRequestExceptions(Exception ex) {
		Response response = null;
		ResponseBuilder responseBuilder = Response.status(Response.Status.BAD_REQUEST);

		if (ex instanceof InvalidEntityException) {
			SyncopeClientExceptionType exType = SyncopeClientExceptionType
					.valueOf("Invalid"
							+ ((InvalidEntityException) ex)
									.getEntityClassSimpleName());

			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					exType.getHeaderValue());

			for (@SuppressWarnings("rawtypes")
			Entry<Class, Set<EntityViolationType>> violation : ((InvalidEntityException) ex)
					.getViolations().entrySet()) {

				for (EntityViolationType violationType : violation.getValue()) {
					responseBuilder.header(exType.getElementHeaderName(),
							violation.getClass().getSimpleName() + ": "
									+ violationType);
				}
			}
			response = responseBuilder.build();

		} else if (ex instanceof WorkflowException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.Workflow.getHeaderValue());
			responseBuilder.header(
					SyncopeClientExceptionType.Workflow.getElementHeaderName(),
					ex.getCause().getMessage());
			response = responseBuilder.build();

		} else if (ex instanceof PropagationException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.Propagation.getHeaderValue());
			responseBuilder.header(SyncopeClientExceptionType.Propagation
					.getElementHeaderName(), ((PropagationException) ex)
					.getResourceName());
			response = responseBuilder.build();

		} else if (ex instanceof InvalidSearchConditionException) {
			responseBuilder.header(
					SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER,
					SyncopeClientExceptionType.InvalidSearchCondition
							.getHeaderValue());
			response = responseBuilder.build();
		}

		return response;
	}

}
