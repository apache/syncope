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
import org.springframework.stereotype.Component;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking rest schema services.
 */
@Component
public class SchemaRestClient extends AbstractBaseRestClient {

    /**
     * Get all user's schemas.
     * @return SchemaTOs
     */
    public List<SchemaTO> getAllUserSchemas() {
        List<SchemaTO> userSchemas = null;

        try {
            userSchemas = Arrays.asList(
                    restTemplate.getForObject(
                    baseURL + "schema/user/list.json",
                    SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user schemas", e);
        }

        return userSchemas;
    }

    /**
     * Get all user's schemas names.
     * @return String list of schemas' names.
     */
    public List<String> getAllUserSchemasNames() {

        List<SchemaTO> userSchemas = null;
        List<String> schemasNames = new ArrayList<String>();
        try {
            userSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL
                    + "schema/user/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user schema names", e);
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
                    restTemplate.getForObject(
                    baseURL + "derivedSchema/user/list.json",
                    DerivedSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user derived schemas", e);
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
            userDerivedSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL
                    + "derivedSchema/user/list.json", DerivedSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all user derived schema names", e);
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
            restTemplate.postForObject(baseURL
                    + "schema/user/create", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While creating a user schema", e);
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
            schema = restTemplate.getForObject(
                    baseURL
                    + "schema/user/read/" + name + ".json", SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a user schema", e);
        }
        return schema;
    }

    /**
     * Update an already existent user schema.
     * @param schemaTO updated
     */
    public void updateUserSchema(SchemaTO schemaTO) {
        try {
            restTemplate.postForObject(baseURL
                    + "schema/user/update", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While updating a user schema", e);
        }
    }

    /**
     * Delete an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteUserSchema(String name) {
        try {
            restTemplate.delete(baseURL
                    + "schema/user/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deleting a user schema", e);
        }
    }

    /**
     * Create new derived user schema.
     * @param schemaTO
     */
    public void createUserDerivedSchema(DerivedSchemaTO schemaTO) {
        try {
            restTemplate.postForObject(baseURL
                    + "derivedSchema/user/create", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While creating a derived user schema", e);
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
            derivedSchemaTO = restTemplate.getForObject(
                    baseURL
                    + "derivedSchema/user/read/" + name + ".json",
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a derived user schema", e);
        }
        return derivedSchemaTO;
    }

    /**
     * Update an already existent user derived schema.
     * @param schemaTO updated
     */
    public void updateUserDerivedSchema(DerivedSchemaTO schemaTO) {
        try {
            restTemplate.postForObject(baseURL
                    + "derivedSchema/user/update", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While updating a derived user schema", e);
        }
    }

    /**
     * Delete an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteUserDerivedSchema(String name) {
        try {
            restTemplate.delete(baseURL
                    + "derivedSchema/user/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deleting a derived user schema", e);
        }
    }

    /**
     * Get all role's schemas.
     * @return SchemaTOs
     */
    public List<SchemaTO> getAllRoleSchemas() {

        List<SchemaTO> roleSchemas = null;

        try {
            roleSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL + "schema/role/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While creating a derived user schema", e);
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
            roleSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL + "schema/role/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all role schema names", e);
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
            membershipSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL + "schema/membership/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all membership schema names", e);
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
            restTemplate.postForObject(baseURL
                    + "schema/role/create", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While creating a role schema", e);
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
            schema = restTemplate.getForObject(
                    baseURL
                    + "schema/role/read/" + name + ".json", SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a role schema", e);
        }
        return schema;
    }

    /**
     * Update an already existent user schema.
     * @param schemaTO updated
     */
    public void updateRoleSchema(SchemaTO schemaTO) {
        try {
            SchemaTO updatedTO = restTemplate.postForObject(
                    baseURL
                    + "schema/role/update", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While updating a role schema", e);
        }
    }

    /**
     * Delete an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteRoleSchema(String name) {
        try {
            restTemplate.delete(baseURL
                    + "schema/role/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deleting a role schema", e);
        }
    }

    /**
     * Get all role's derived schemas.
     * @return DerivedSchemaTOs
     */
    public List<DerivedSchemaTO> getAllRoleDerivedSchemas() {
        List<DerivedSchemaTO> roleDerivedSchemas = null;

        try {
            roleDerivedSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL
                    + "derivedSchema/role/list.json", DerivedSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all role derived schemas", e);
        }

        return roleDerivedSchemas;
    }

    /**
     * Create new user schema.
     * @param schemaTO
     */
    public void createRoleDerivedSchema(DerivedSchemaTO schemaTO) {
        try {
            restTemplate.postForObject(baseURL
                    + "derivedSchema/role/create", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While creating a role derived schema", e);
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
            derivedSchemaTO = restTemplate.getForObject(
                    baseURL
                    + "derivedSchema/role/read/" + name + ".json",
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a role derived schema", e);
        }
        return derivedSchemaTO;
    }

    /**
     * Update an already existent user derived schema.
     * @param schemaTO updated
     */
    public void updateRoleDerivedSchema(DerivedSchemaTO schemaTO) {

        try {
            restTemplate.postForObject(baseURL
                    + "derivedSchema/role/update", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While updating a role derived schema", e);
        }
    }

    /**
     * Delete an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteRoleDerivedSchema(String name) {
        try {
            restTemplate.delete(baseURL
                    + "derivedSchema/role/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deleting a role derived schema", e);
        }
    }

    /**
     * Get all role's schemas.
     * @return SchemaTOs
     */
    public List<SchemaTO> getAllMemberhipSchemas() {

        List<SchemaTO> memberhipSchemas = null;

        try {
            memberhipSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL
                    + "schema/membership/list.json", SchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all membership schemas", e);
        }

        return memberhipSchemas;
    }

    /**
     * Create new membership schema.
     * @param schemaTO
     */
    public void createMemberhipSchema(SchemaTO schemaTO) {
        try {
            restTemplate.postForObject(baseURL
                    + "schema/membership/create", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While creating a membership schema", e);
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
            schema = restTemplate.getForObject(
                    baseURL
                    + "schema/membership/read/" + name + ".json",
                    SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a membership schema", e);
        }
        return schema;
    }

    /**
     * Update an already existent membership schema.
     * @param schemaTO updated
     */
    public void updateMemberhipSchema(SchemaTO schemaTO) {
        try {
            SchemaTO updatedTO = restTemplate.postForObject(
                    baseURL
                    + "schema/membership/update", schemaTO, SchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While updating a membership schema", e);
        }
    }

    /**
     * Delete an already existent membership schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteMemberhipSchema(String name) {
        try {
            restTemplate.delete(baseURL
                    + "schema/membership/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deleting a membership schema", e);
        }
    }

    /**
     * Populator for Validator Schema DropDown components.
     */
    public Set<String> getAllValidatorClasses() {
        Set<String> validators = null;

        try {
            validators = restTemplate.getForObject(
                    baseURL + "configuration/validators.json",
                    Set.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all validators", e);
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
            roleDerivedSchemas = Arrays.asList(restTemplate.getForObject(
                    baseURL
                    + "derivedSchema/membership/list.json",
                    DerivedSchemaTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all membership derived schemas", e);
        }

        return roleDerivedSchemas;
    }

    /**
     * Create new membership schema.
     * @param schemaTO
     */
    public void createMembershipDerivedSchema(DerivedSchemaTO schemaTO) {
        try {
            restTemplate.postForObject(baseURL
                    + "derivedSchema/membership/create", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While creating a membership derived schema", e);
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
            derivedSchemaTO = restTemplate.getForObject(
                    baseURL
                    + "derivedSchema/membership/read/" + name + ".json",
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a membership derived schema", e);
        }
        return derivedSchemaTO;
    }

    /**
     * Update an already existent membership derived schema.
     * @param schemaTO updated
     */
    public void updateMembershipDerivedSchema(DerivedSchemaTO schemaTO) {

        try {
            restTemplate.postForObject(baseURL
                    + "derivedSchema/membership/update", schemaTO,
                    DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While updating a membership derived schema", e);
        }
    }

    /**
     * Delete an already existent membership derived schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteMembershipDerivedSchema(String name) {
        try {
            restTemplate.delete(baseURL
                    + "derivedSchema/membership/delete/" + name + ".json");
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deleting a membership derived schema", e);
        }
    }
}
