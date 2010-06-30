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
package org.syncope.core.persistence.beans;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.UserSchema;

@Entity
public class SchemaMapping extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * Attribute schema to be mapped.
     * Consider that we can associate tha same attribute schema more
     * than once, with different aliases, to different resource attributes.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private UserSchema userSchema;

    /**
     * Attribute schema to be mapped.
     * Consider that we can associate tha same attribute schema more
     * than once, with different aliases, to different resource attributes.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private RoleSchema roleSchema;

    /**
     * Target resource that has fields to be mapped over user attribute schemas.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private Resource resource;

    /**
     * Target resource's field to be mapped.
     */
    @Column(nullable = false)
    private String field;

    /**
     * Specify if the mapped target resource's field is the key.
     */
    @Column(nullable = false)
    private boolean accountid;

    /**
     * Specify if the mapped target resource's field is the password.
     */
    @Column(nullable = false)
    private boolean password;

    /**
     * Specify if the mapped target resource's field is nullable.
     */
    @Column(nullable = false)
    private boolean nullable;

    public SchemaMapping() {
        accountid = false;
        password = false;
        nullable = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isAccountid() {
        return accountid;
    }

    public void setAccountid(boolean accountid) {
        this.accountid = accountid;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public RoleSchema getRoleSchema() {
        return roleSchema;
    }

    public void setRoleSchema(RoleSchema roleSchema) {
        this.roleSchema = roleSchema;
    }

    public UserSchema getUserSchema() {
        return userSchema;
    }

    public void setUserSchema(UserSchema userSchema) {
        this.userSchema = userSchema;
    }
}
