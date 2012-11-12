package org.apache.syncope.controller;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.syncope.NotFoundException;
import org.apache.syncope.mod.RoleMod;
import org.apache.syncope.to.RoleTO;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Path("/role")
public interface RoleController {

	@GET
	@Path("/{roleId}")
	@PreAuthorize("isAuthenticated()")
	@Transactional(readOnly = true)
	public abstract RoleTO read(@PathParam("roleId") final Long roleId)
			throws NotFoundException, UnauthorizedRoleException;

	@GET
	@Path("/{roleId}/parent")
	@PreAuthorize("hasRole('ROLE_READ')")
	@Transactional(readOnly = true)
	public abstract RoleTO parent(@PathParam("roleId") final Long roleId)
			throws NotFoundException, UnauthorizedRoleException;

	@GET
	@Path("/{roleId}/children")
	@PreAuthorize("hasRole('ROLE_READ')")
	@Transactional(readOnly = true)
	public abstract List<RoleTO> children(@PathParam("roleId") final Long roleId)
			throws NotFoundException;

	@GET
	@Transactional(readOnly = true)
	public abstract List<RoleTO> list();

	@POST
	@Path("/")
	@PreAuthorize("hasRole('ROLE_CREATE')")
	public abstract Response create(final RoleTO roleTO) throws SyncopeClientCompositeErrorException,
			UnauthorizedRoleException;

	@PUT
	@Path("/{roleId}")
	@PreAuthorize("hasRole('ROLE_UPDATE')")
	public abstract RoleTO update(@PathParam("roleId") final Long roleId,
			final RoleMod roleMod) throws NotFoundException,
			UnauthorizedRoleException;

	@DELETE
	@Path("/{roleId}")
	@PreAuthorize("hasRole('ROLE_DELETE')")
	public abstract RoleTO delete(@PathParam("roleId") final Long roleId)
			throws NotFoundException, UnauthorizedRoleException;

}