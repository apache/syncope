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

import org.springframework.web.client.RestClientException;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.DerivedSchemaTOs;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.SchemaTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.SyncopeClientExceptionType;

/**
 * Console client for invoking rest schema services.
 */
public class SchemaRestClient {

    RestClient restClient;

    /**
     * Get all user's schemas.
     * @return SchemaTOs
     */
    public SchemaTOs getAllUserSchemas() {

        SchemaTOs userSchemas = null;

        try {
            userSchemas =
                    restClient.getRestTemplate().getForObject(restClient.getBaseURL() + "schema/user/list.json", SchemaTOs.class);
        } catch (SyncopeClientCompositeErrorException e) {

            for (SyncopeClientExceptionType exceptionType :
                    SyncopeClientExceptionType.values()) {
                if (e.hasException(exceptionType)) {
                    System.err.println(exceptionType);
                    System.err.println(
                            e.getException(exceptionType).getElements());
                }
            }

        } catch (RestClientException e) {
            e.printStackTrace();
        }

        return userSchemas;
    }

    /**
     * Get all user's derived schemas.
     * @return DerivedSchemaTOs
     */
    public DerivedSchemaTOs getAllUserDerivedSchemas() {

        DerivedSchemaTOs userDerivedSchemas = null;
        try {
            userDerivedSchemas = restClient.getRestTemplate().getForObject(restClient.getBaseURL()
                    + "derivedSchema/user/list.json", DerivedSchemaTOs.class);
        } catch (SyncopeClientCompositeErrorException e) {
        }
        return userDerivedSchemas;
    }

    /**
     * Create new user schema.
     * @param schemaTO
     */
    public void createUserSchema(SchemaTO schemaTO) {

        restClient.getRestTemplate().postForObject(restClient.getBaseURL() +
                "schema/user/create", schemaTO, SchemaTO.class);
    }

    /**
     * Load an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public SchemaTO readUserSchema(String name) {
        SchemaTO schema = restClient.getRestTemplate().getForObject(restClient.getBaseURL()
                + "schema/user/read/" + name + ".json", SchemaTO.class);
        return schema;
    }

    /**
     * Update an already existent user schema.
     * @param schemaTO updated
     */
    public void updateUserSchema(SchemaTO schemaTO) {

       SchemaTO updatedTO = restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                + "schema/user/update", schemaTO, SchemaTO.class);
    }
    
    /**
     * Delete an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteUserSchema(String name) {
        restClient.getRestTemplate().delete(restClient.getBaseURL() +
                "schema/user/delete/" + name + ".json");
    }

    /**
     * Create new derived user schema.
     * @param schemaTO
     */
    public void createUserDerivedSchema(DerivedSchemaTO schemaTO) {

        restClient.getRestTemplate().postForObject(restClient.getBaseURL() +
                "derivedSchema/user/create", schemaTO, DerivedSchemaTO.class);
    }

    /**
     * Load an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return DerivedSchemaTO
     */
    public DerivedSchemaTO readUserDerivedSchema(String name) {
        DerivedSchemaTO derivedSchemaTO = restClient.getRestTemplate().getForObject(restClient.getBaseURL()
                + "derivedSchema/user/read/" + name + ".json", DerivedSchemaTO.class);
        return derivedSchemaTO;
    }

    /**
     * Update an already existent user derived schema.
     * @param schemaTO updated
     */
    public void updateUserDerivedSchema(DerivedSchemaTO schemaTO) {

       DerivedSchemaTO updatedTO = restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                + "derivedSchema/user/update", schemaTO, DerivedSchemaTO.class);
    }

    /**
     * Delete an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteUserDerivedSchema(String name) {
        restClient.getRestTemplate().delete(restClient.getBaseURL()
                + "derivedSchema/user/delete/" + name + ".json");
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
    public SchemaTOs getAllRoleSchemas() {

        SchemaTOs roleSchemas = null;

        try {
            roleSchemas =
                    restClient.getRestTemplate().getForObject(restClient.getBaseURL() + "schema/role/list.json", SchemaTOs.class);
        } catch (SyncopeClientCompositeErrorException e) {

        } catch (RestClientException e) {
            e.printStackTrace();
        }

        return roleSchemas;
    }

    /**
     * Create new user schema.
     * @param schemaTO
     */
    public void createRoleSchema(SchemaTO schemaTO) {

        restClient.getRestTemplate().postForObject(restClient.getBaseURL() +
                "schema/role/create", schemaTO, SchemaTO.class);
    }

    /**
     * Load an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public SchemaTO readRoleSchema(String name) {
        SchemaTO schema = restClient.getRestTemplate().getForObject(restClient.getBaseURL()
                + "schema/role/read/" + name + ".json", SchemaTO.class);
        return schema;
    }

    /**
     * Update an already existent user schema.
     * @param schemaTO updated
     */
    public void updateRoleSchema(SchemaTO schemaTO) {

       SchemaTO updatedTO = restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                + "schema/role/update", schemaTO, SchemaTO.class);
    }

    /**
     * Delete an already existent user schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteRoleSchema(String name) {
        restClient.getRestTemplate().delete(restClient.getBaseURL() +
                "schema/role/delete/" + name + ".json");
    }

    /**
     * Get all role's derived schemas.
     * @return DerivedSchemaTOs
     */
    public DerivedSchemaTOs getAllRoleDerivedSchemas() {

        DerivedSchemaTOs roleDerivedSchemas = null;
        try {
            roleDerivedSchemas = restClient.getRestTemplate().getForObject(restClient.getBaseURL()
                    + "derivedSchema/role/list.json", DerivedSchemaTOs.class);
        } catch (SyncopeClientCompositeErrorException e) {
        }
        return roleDerivedSchemas;
    }

    /**
     * Create new user schema.
     * @param schemaTO
     */
    public void createRoleDerivedSchema(DerivedSchemaTO schemaTO) {

        restClient.getRestTemplate().postForObject(restClient.getBaseURL() +
                "derivedSchema/role/create", schemaTO, DerivedSchemaTO.class);
    }


    /**
     * Load an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return DerivedSchemaTO
     */
    public DerivedSchemaTO readRoleDerivedSchema(String name) {
        DerivedSchemaTO derivedSchemaTO = restClient.getRestTemplate().getForObject(restClient.getBaseURL()
                + "derivedSchema/role/read/" + name + ".json", DerivedSchemaTO.class);
        return derivedSchemaTO;
    }

    /**
     * Update an already existent user derived schema.
     * @param schemaTO updated
     */
    public void updateRoleDerivedSchema(DerivedSchemaTO schemaTO) {

       DerivedSchemaTO updatedTO = restClient.getRestTemplate().postForObject(restClient.getBaseURL()
                + "derivedSchema/role/update", schemaTO, DerivedSchemaTO.class);
    }

    /**
     * Delete an already existent user derived schema by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteRoleDerivedSchema(String name) {
        restClient.getRestTemplate().delete(restClient.getBaseURL()
                + "derivedSchema/role/delete/" + name + ".json");
    }
}