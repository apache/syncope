package org.apache.syncope.exceptions;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.http.HttpStatus;
import org.apache.syncope.NotFoundException;
import org.apache.syncope.services.UnauthorizedRoleException;
import org.apache.syncope.validation.InvalidEntityException;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

@Provider
public class RestHttpStatusCodeMapper implements ExceptionMapper<Exception>,
        ResponseExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {

        String errorMsg = e.getMessage();
//        if (e.getCause() != null && e.getCause().getMessage() != null)
//            errorMsg += "\n" + e.getCause().getMessage();

        if (e instanceof DataIntegrityViolationException)
            errorMsg = e.getClass().getCanonicalName() + "\n" + e.getCause().toString();

        if (e instanceof NotFoundException)
            return Response.status(Response.Status.NOT_FOUND).entity(errorMsg)
                    .type(MediaType.TEXT_PLAIN_TYPE).build();
        if (e instanceof AccessDeniedException)
            return Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Spring Security Application\"")
                    .entity(errorMsg).type(MediaType.TEXT_PLAIN_TYPE).build();
        if (e instanceof UnauthorizedRoleException)
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorMsg).type(MediaType.TEXT_PLAIN_TYPE).build();
        if (e instanceof InvalidEntityException || e instanceof DataIntegrityViolationException || e instanceof SyncopeClientCompositeErrorException)
            return Response.status(Response.Status.BAD_REQUEST).entity(errorMsg)
                    .type(MediaType.TEXT_PLAIN_TYPE).build();

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMsg)
                .type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    @Override
    public Exception fromResponse(Response response) {
        int statusCode = response.getStatus();

        switch (statusCode) {
        case HttpStatus.SC_NOT_FOUND:
            return new NotFoundException(response.readEntity(String.class));
        case HttpStatus.SC_UNAUTHORIZED:
            // TODO find a way to enhance this error message with correct
            // RoleNumbers.
            return new UnauthorizedRoleException(-1L);
        }

        return null;
    }

}
