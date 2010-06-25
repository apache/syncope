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
package org.syncope.identityconnectors.bundles.commons.staticwebservice.to;

public class WSAttribute {

    /**
     * The name of the attribute.
     */
    private String name = null;

    /**
     * The type of the attribute:
     * String, Long, Double, Boolean, Character, Float, Integer, URI,
     * File, GuardedByteArray, GuardedString, Date.
     */
    private String type = "String";

    /**
     * Specifies if the attribute is a key.
     */
    private boolean key = false;

    /**
     * Specifies if the attribute is the password.
     */
    private boolean password = false;

    /**
     * Specifies if the attribute is nullable.
     */
    private boolean nullable = true;

    public WSAttribute() {
    }

    /**
     * Constructor: default attribute is a string nullable.
     * 
     * @param name defines the name of the attribute.
     */
    public WSAttribute(String name) {
        this.name = name;
    }

    /**
     * Constructor: default attribute is nullable.
     *
     * @param name defines the name of the attribute.
     * @param type defines the type of the attribute.
     */
    public WSAttribute(String name, String type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Constructor: default attribute is not a key nor a password.
     *
     * @param name defines the name of the attribute.
     * @param type defines the type of the attribute.
     * @param isNullable defines if the attribute is nullable.
     */
    public WSAttribute(String name, String type, Boolean nullable) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isKey() {
        return key;
    }

    public void setKey(boolean key) {
        this.key = key;
        this.nullable = false;
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
}
