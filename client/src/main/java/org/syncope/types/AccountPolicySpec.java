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

import java.util.ArrayList;
import java.util.List;
import org.syncope.client.SchemaList;

public class AccountPolicySpec extends AbstractPolicySpec {

    private static final long serialVersionUID = 3259256974414758406L;

    /**
     * Minimum length.
     */
    private int maxLength;

    /**
     * Maximum length.
     */
    private int minLength;

    /**
     * Substrings not permitted.
     */
    private List<String> wordsNotPermitted;

    /**
     * User attribute values not permitted.
     */
    @SchemaList
    private List<String> schemasNotPermitted;

    /**
     * Substrings not permitted as prefix.
     */
    private List<String> prefixesNotPermitted;

    /**
     * Substrings not permitted as suffix.
     */
    private List<String> suffixesNotPermitted;

    /**
     * Specify if one or more lowercase characters are permitted.
     */
    private boolean allUpperCase;

    /**
     * Specify if one or more uppercase characters are permitted.
     */
    private boolean allLowerCase;

    /**
     * Specify if it must be propagate suspension in case of maximum subsequent
     * failed logins reached.
     */
    private boolean propagateSuspension;

    /**
     * Number of permitted login retries.
     * 0 disabled; >0 enabled.
     * If the number of subsequent failed logins will be greater then this value
     * the account will be suspended (lock-out).
     */
    private int permittedLoginRetries;

    public boolean isAllLowerCase() {
        return allLowerCase;
    }

    public void setAllLowerCase(boolean allLowerCase) {
        this.allLowerCase = allLowerCase;
    }

    public boolean isAllUpperCase() {
        return allUpperCase;
    }

    public void setAllUpperCase(boolean allUpperCase) {
        this.allUpperCase = allUpperCase;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public List<String> getPrefixesNotPermitted() {
        if (prefixesNotPermitted == null) {
            prefixesNotPermitted = new ArrayList<String>();
        }
        return prefixesNotPermitted;
    }

    public void setPrefixesNotPermitted(List<String> prefixesNotPermitted) {
        this.prefixesNotPermitted = prefixesNotPermitted;
    }

    public List<String> getSchemasNotPermitted() {
        if (schemasNotPermitted == null) {
            schemasNotPermitted = new ArrayList<String>();
        }
        return schemasNotPermitted;
    }

    public void setSchemasNotPermitted(List<String> schemasNotPermitted) {
        this.schemasNotPermitted = schemasNotPermitted;
    }

    public List<String> getSuffixesNotPermitted() {
        if (suffixesNotPermitted == null) {
            suffixesNotPermitted = new ArrayList<String>();
        }
        return suffixesNotPermitted;
    }

    public void setSuffixesNotPermitted(List<String> suffixesNotPermitted) {
        this.suffixesNotPermitted = suffixesNotPermitted;
    }

    public List<String> getWordsNotPermitted() {
        if (wordsNotPermitted == null) {
            wordsNotPermitted = new ArrayList<String>();
        }
        return wordsNotPermitted;
    }

    public void setWordsNotPermitted(List<String> wordsNotPermitted) {
        this.wordsNotPermitted = wordsNotPermitted;
    }

    public boolean isPropagateSuspension() {
        return propagateSuspension;
    }

    public void setPropagateSuspension(boolean propagateSuspension) {
        this.propagateSuspension = propagateSuspension;
    }

    public int getPermittedLoginRetries() {
        return permittedLoginRetries;
    }

    public void setPermittedLoginRetries(int permittedLoginRetries) {
        this.permittedLoginRetries = permittedLoginRetries;
    }
}
