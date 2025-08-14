package org.apache.syncope.common.rest.api.service;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.to.PasswordModuleTO;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for password management modules.
 */
@Tag(name = "PasswordModules")
@SecurityRequirements({
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
@Path("passwordModules")
public interface PasswordModuleService extends JAXRSService {

    /**
     * Returns the password management module matching the given key.
     *
     * @param key key of requested password management module
     * @return password management module with matching id
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PasswordModuleTO read(@NotNull @PathParam("key") String key);

    /**
     * Returns a list of password management modules.
     *
     * @return list of password management modules
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<PasswordModuleTO> list();

    /**
     * Create a new password management module.
     *
     * @param passwordModuleTO PasswordModule to be created.
     * @return Response object featuring Location header of created password management module
     */
    @ApiResponses(
            @ApiResponse(responseCode = "201",
                    description = "PasswordModule successfully created", headers = {
                    @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                    @Schema(type = "string"),
                            description = "UUID generated for the entity created"),
                    @Header(name = HttpHeaders.LOCATION, schema =
                    @Schema(type = "string"),
                            description = "URL of the entity created") }))
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response create(@NotNull PasswordModuleTO passwordModuleTO);

    /**
     * Updates password management module matching the given key.
     *
     * @param passwordModuleTO PasswordModule to replace existing password management module
     */
    @Parameter(name = "key", description = "PasswordModule's key", in = ParameterIn.PATH, schema =
    @Schema(type = "string"))
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void update(@NotNull PasswordModuleTO passwordModuleTO);

    /**
     * Delete password management module matching the given key.
     *
     * @param key key of password management module to be deleted
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void delete(@NotNull @PathParam("key") String key);
}
