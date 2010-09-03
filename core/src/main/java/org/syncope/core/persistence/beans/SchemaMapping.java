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

import static javax.persistence.EnumType.STRING;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import org.syncope.types.SchemaType;

@Entity
public class SchemaMapping extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(nullable = false)
    private String schemaName;
    @Column(nullable = false)
    @Enumerated(STRING)
    private SchemaType schemaType;
    /**
     * Target resource that has fields to be mapped over user attribute schemas.
     */
    @ManyToOne(fetch = FetchType.EAGER,
    cascade = {CascadeType.REFRESH, CascadeType.MERGE})
    private TargetResource resource;
    /**
     * Target resource's field to be mapped.
     */
    @Column(nullable = false)
    private String field;
    /**
     * Specify if the mapped target resource's field is the key.
     */
    @Column(nullable = false)
    @Basic
    private Character accountid;
    /**
     * Specify if the mapped target resource's field is the password.
     */
    @Column(nullable = false)
    @Basic
    private Character password;
    /**
     * Specify if the mapped target resource's field is nullable.
     */
    @Column(nullable = false)
    @Basic
    private Character nullable;

    public SchemaMapping() {
        super();

        accountid = getBooleanAsCharacter(false);
        password = getBooleanAsCharacter(false);
        nullable = getBooleanAsCharacter(true);
    }

    public Long getId() {
        return id;
    }

    public boolean isAccountid() {
        return isBooleanAsCharacter(accountid);
    }

    public void setAccountid(boolean accountid) {
        this.accountid = getBooleanAsCharacter(accountid);
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public boolean isNullable() {
        return isBooleanAsCharacter(nullable);
    }

    public void setNullable(boolean nullable) {
        this.nullable = getBooleanAsCharacter(nullable);
    }

    public boolean isPassword() {
        return isBooleanAsCharacter(password);
    }

    public void setPassword(boolean password) {
        this.password = getBooleanAsCharacter(password);
    }

    public TargetResource getResource() {
        return resource;
    }

    public void setResource(TargetResource resource) {
        this.resource = resource;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public SchemaType getSchemaType() {
        return schemaType;
    }

    public void setSchemaType(SchemaType schemaType) {
        this.schemaType = schemaType;
    }
}
