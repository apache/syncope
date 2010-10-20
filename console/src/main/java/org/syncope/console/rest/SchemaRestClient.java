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
package org.syncope.console.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.SyncopeClientExceptionType;

/**
 * Console client for invoking rest schema services.
 */
public class SchemaRestClient {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(SchemaRestClient.class);
    protected RestClient restClient;

    /**
     * Get all user's schemas.
     * @return SchemaTOs
     */
    public List<SchemaTO> getAllUserSchemas() {
        List<SchemaTO> userSchemas = null;

        try {
            userSchemas = Arrays.asList(
                    restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL() + "schema/user/list.json",
                    SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            for (SyncopeClientExceptionType exceptionType :
                    SyncopeClientExceptionType.values()) {
                if (e.hasException(exceptionType)) {
                    LOG.error(exceptionType.toString());
                    LOG.error(e.getException(exceptionType).
                            getElements().toString());
                }
            }
        } catch (RestClientException e) {
            e.printStackTrace();
        }

        return userSchemas;
    }

    /**
     * Get all user's schemas names.
     * @return String list of schemas' names.
     */
    public List<String> getAllUserSchemasNames() {

        List<SchemaTO>  userSchemas = null;
        List<String> schemasNames = new ArrayList<String>();
        try {
            userSchemas = Arrays.asList(restClient.getRestTemplate()
                    .getForObject(restClient.getBaseURL()
                    + "schema/user/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        } catch (RestClientException e) {
            e.printStackTrace();
        }

        for (SchemaTO schemaTO : userSchemas) {
            schemasNames.add(schemaTO.getName());
        }

        return schemasNames;
    }

    /**
     * Get all user's derived schemas.
     * @return DerivedSchemaTOs
     */
    public List<DerivedSchemaTO> getAllUserDerivedSchemas() {

        List<DerivedSchemaTO> userDerivedSchemas = null;
        try {
            userDerivedSchemas = Arrays.asList(
                    restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL() + "derivedSchema/user/list.json",
                    DerivedSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
        return userDerivedSchemas;
    }

    /**
     * Get all user's derived schemas names.
     * @return String list of derived schemas' names.
     */
    public List<String> getAllUserDerivedSchemasNames() {

        List<DerivedSchemaTO> userDerivedSchemas = null;
        List<String> userDerivedSchemasNames = new ArrayList<String>();
        try {
            userDerivedSchemas = Arrays.asList(restClient.getRestTemplate()
                    .getForObject(restClient.getBaseURL()
                    + "derivedSchema/user/list.json", DerivedSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        } catch (RestClientException e) {
            e.printStackTrace();
        }

        for (DerivedSchemaTO schemaTO : userDerivedSchemas) {
            userDerivedSchemasNames.add(schemaTO.getName());
        }

        return userDerivedSchemasNames;
    }

    /**
     * Create new user schema.
     * @param schemaTO
     */
    public void createUserSchema(SchemaTO schemaTO) {
        try {
            restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                    + "schema/user/create", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public SchemaTO readUserSchema(String name) {
        SchemaTO schema = null;

        try {
            schema = restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL()
                    + "schema/user/read/" + name + ".json", SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
        return schema;
    }

    /**
     * Update an already existent user schema.
     * @param schemaTO updated
     */
    public void updateUserSchema(SchemaTO schemaTO) {
        try {
            restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                    + "schema/user/update", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteUserSchema(String name) {
        try {
            restClient.getRestTemplate().delete(restClient.getBaseURL()
                    + "schema/user/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create new derived user schema.
     * @param schemaTO
     */
    public void createUserDerivedSchema(DerivedSchemaTO schemaTO) {
        try {
            restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                    + "derivedSchema/user/create", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return DerivedSchemaTO
     */
    public DerivedSchemaTO readUserDerivedSchema(String name) {
        DerivedSchemaTO derivedSchemaTO = null;
        try {
            derivedSchemaTO = restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL()
                    + "derivedSchema/user/read/" + name + ".json",
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
        return derivedSchemaTO;
    }

    /**
     * Update an already existent user derived schema.
     * @param schemaTO updated
     */
    public void updateUserDerivedSchema(DerivedSchemaTO schemaTO) {
        try {
            restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                    + "derivedSchema/user/update", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }

    }

    /**
     * Delete an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteUserDerivedSchema(String name) {
        try {
            restClient.getRestTemplate().delete(restClient.getBaseURL()
                    + "derivedSchema/user/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Get all role's schemas.
     * @return SchemaTOs
     */
    public List<SchemaTO> getAllRoleSchemas() {

         List<SchemaTO> roleSchemas = null;

        try {
            roleSchemas = Arrays.asList(restClient.getRestTemplate()
                    .getForObject(restClient.getBaseURL() +
                    "schema/role/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
        } catch (RestClientException e) {
            e.printStackTrace();
        }

        return roleSchemas;
    }

    /**
     * Get all role's schemas names.
     * @return String list of role schemas' names
     */
    public List<String> getAllRoleSchemasNames() {

        List<SchemaTO> roleSchemas = null;
        List<String> roleSchemasNames = new ArrayList<String>();

        try {
            roleSchemas = Arrays.asList(restClient.getRestTemplate()
                    .getForObject( restClient.getBaseURL() +
                    "schema/role/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
        } catch (RestClientException e) {
            e.printStackTrace();
        }

        for (SchemaTO schemaTO : roleSchemas) {
            roleSchemasNames.add(schemaTO.getName());
        }

        return roleSchemasNames;
    }

    /**
     * Get all role's schemas names.
     * @return String list of role schemas' names
     */
    public List<String> getAllMembershipSchemasNames() {

        List<SchemaTO> membershipSchemas = null;
        List<String> membershipSchemasNames = new ArrayList<String>();

        try {
            membershipSchemas = Arrays.asList(restClient.getRestTemplate()
                    .getForObject( restClient.getBaseURL() +
                    "schema/membership/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
        } catch (RestClientException e) {
            e.printStackTrace();
        }

        for (SchemaTO schemaTO : membershipSchemas) {
            membershipSchemasNames.add(schemaTO.getName());
        }

        return membershipSchemasNames;
    }

    /**
     * Create new user schema.
     * @param schemaTO
     */
    public void createRoleSchema(SchemaTO schemaTO) {
        try {
            restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                    + "schema/role/create", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public SchemaTO readRoleSchema(String name) {
        SchemaTO schema = null;

        try {
            schema = restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL()
                    + "schema/role/read/" + name + ".json", SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
        return schema;
    }

    /**
     * Update an already existent user schema.
     * @param schemaTO updated
     */
    public void updateRoleSchema(SchemaTO schemaTO) {
        try {
            SchemaTO updatedTO = restClient.getRestTemplate().postForObject(
                    restClient.getBaseURL()
                    + "schema/role/update", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteRoleSchema(String name) {
        try {
            restClient.getRestTemplate().delete(restClient.getBaseURL()
                    + "schema/role/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get all role's derived schemas.
     * @return DerivedSchemaTOs
     */
    public List<DerivedSchemaTO> getAllRoleDerivedSchemas() {
        List<DerivedSchemaTO>  roleDerivedSchemas = null;

        try {
            roleDerivedSchemas = Arrays.asList(restClient.getRestTemplate()
                    .getForObject(restClient.getBaseURL() +
                    "derivedSchema/role/list.json", DerivedSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
        }
        return roleDerivedSchemas;
    }

    /**
     * Create new user schema.
     * @param schemaTO
     */
    public void createRoleDerivedSchema(DerivedSchemaTO schemaTO) {
        try {
            restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                    + "derivedSchema/role/create", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return DerivedSchemaTO
     */
    public DerivedSchemaTO readRoleDerivedSchema(String name) {
        DerivedSchemaTO derivedSchemaTO = null;

        try {
            derivedSchemaTO = restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL()
                    + "derivedSchema/role/read/" + name + ".json",
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
        return derivedSchemaTO;
    }

    /**
     * Update an already existent user derived schema.
     * @param schemaTO updated
     */
    public void updateRoleDerivedSchema(DerivedSchemaTO schemaTO) {

        try {
            restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                    + "derivedSchema/role/update", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteRoleDerivedSchema(String name) {
        try {
            restClient.getRestTemplate().delete(restClient.getBaseURL()
                    + "derivedSchema/role/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get all role's schemas.
     * @return SchemaTOs
     */
    public List<SchemaTO> getAllMemberhipSchemas() {

        List<SchemaTO> memberhipSchemas = null;

        try {
            memberhipSchemas = Arrays.asList(restClient.getRestTemplate()
                    .getForObject(restClient.getBaseURL()
                    + "schema/membership/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
        } catch (RestClientException e) {
            e.printStackTrace();
        }

        return memberhipSchemas;
    }

    /**
     * Create new membership schema.
     * @param schemaTO
     */
    public void createMemberhipSchema(SchemaTO schemaTO) {
        try {
            restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                    + "schema/membership/create", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load an already existent membership schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public SchemaTO readMemberhipSchema(String name) {
        SchemaTO schema = null;

        try {
            schema = restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL()
                    + "schema/membership/read/" + name + ".json",
                    SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
        return schema;
    }

    /**
     * Update an already existent membership schema.
     * @param schemaTO updated
     */
    public void updateMemberhipSchema(SchemaTO schemaTO) {
        try {
            SchemaTO updatedTO = restClient.getRestTemplate().postForObject(
                    restClient.getBaseURL()
                    + "schema/membership/update", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete an already existent membership schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteMemberhipSchema(String name) {
        try {
            restClient.getRestTemplate().delete(restClient.getBaseURL()
                    + "schema/membership/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Populator for Validator Schema DropDown components.
     */
    public Set<String> getAllValidatorClasses() {
        Set<String> validators = null;

        try {
            validators = restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL() + "configuration/validators.json",
                    Set.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
        return validators;
    }

    /**
     * Get all membership's derived schemas.
     * @return DerivedSchemaTOs
     */
    public List<DerivedSchemaTO> getAllMembershipDerivedSchemas() {
        List<DerivedSchemaTO> roleDerivedSchemas = null;

        try {
            roleDerivedSchemas = Arrays.asList(restClient.getRestTemplate()
                    .getForObject( restClient.getBaseURL() +
                    "derivedSchema/membership/list.json",
                    DerivedSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
        }
        return roleDerivedSchemas;
    }

    /**
     * Create new membership schema.
     * @param schemaTO
     */
    public void createMembershipDerivedSchema(DerivedSchemaTO schemaTO) {
        try {
            restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                    + "derivedSchema/membership/create", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load an already existent membership derived schema by its name.
     * @param name (e.g.:surname)
     * @return DerivedSchemaTO
     */
    public DerivedSchemaTO readMembershipDerivedSchema(String name) {
        DerivedSchemaTO derivedSchemaTO = null;

        try {
            derivedSchemaTO = restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL()
                    + "derivedSchema/membership/read/" + name + ".json",
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
        return derivedSchemaTO;
    }

    /**
     * Update an already existent membership derived schema.
     * @param schemaTO updated
     */
    public void updateMembershipDerivedSchema(DerivedSchemaTO schemaTO) {

        try {
            restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                    + "derivedSchema/membership/update", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete an already existent membership derived schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteMembershipDerivedSchema(String name) {
        try {
            restClient.getRestTemplate().delete(restClient.getBaseURL()
                    + "derivedSchema/membership/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }
}
