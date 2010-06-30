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
package org.syncope.types;

public enum SyncopeClientExceptionType {

    NotFound(
    "Syncope.NotFound",
    ""),
    InvalidSchemas(
    "Syncope.User.InvalidSchema",
    "Syncope.User.InvalidSchema.attributeName"),
    UserRequiredValuesMissing(
    "Syncope.User.RequiredValuesMissing",
    "Syncope.User.RequiredValuesMissing.attributeName"),
    UserInvalidValues(
    "Syncope.User.InvalidValues",
    "Syncope.User.InvalidValues.attributeName");
    private String exceptionTypeHeaderValue;
    private String attributeNameHeaderName;

    private SyncopeClientExceptionType(String exceptionTypeHeaderValue,
            String attributeNameHeaderName) {

        this.exceptionTypeHeaderValue = exceptionTypeHeaderValue;
        this.attributeNameHeaderName = attributeNameHeaderName;
    }

    public static SyncopeClientExceptionType getFromHeaderValue(
            String exceptionTypeHeaderValue) {

        SyncopeClientExceptionType result = null;
        for (SyncopeClientExceptionType syncopeClientExceptionType : values()) {
            if (exceptionTypeHeaderValue.equals(
                    syncopeClientExceptionType.getExceptionTypeHeaderValue())) {
                result = syncopeClientExceptionType;
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Unexpected header value: "
                    + exceptionTypeHeaderValue);
        }

        return result;
    }

    public String getAttributeNameHeaderName() {
        return attributeNameHeaderName;
    }

    public String getExceptionTypeHeaderValue() {
        return exceptionTypeHeaderValue;
    }
}
