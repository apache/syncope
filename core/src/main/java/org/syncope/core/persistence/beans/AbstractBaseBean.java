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

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

public abstract class AbstractBaseBean implements Serializable {

    protected static final Logger log = LoggerFactory.getLogger(
            AbstractBaseBean.class);

    protected String[] getExcludeFields() {
        Set<String> excludeFields = new HashSet<String>();

        PropertyDescriptor[] propertyDescriptors =
                BeanUtils.getPropertyDescriptors(getClass());
        for (int i = 0; i < propertyDescriptors.length; i++) {

            if (propertyDescriptors[i].getPropertyType().isInstance(
                    Collections.EMPTY_SET)
                    || propertyDescriptors[i].getPropertyType().isInstance(
                    Collections.EMPTY_LIST)) {
                
                excludeFields.add(propertyDescriptors[i].getName());
            }
        }

        return excludeFields.toArray(new String[]{});
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, getExcludeFields());
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, getExcludeFields());
    }

    @Override
    public String toString() {
        Method method = BeanUtils.findMethod(getClass(), "getId");
        if (method == null) {
            method = BeanUtils.findMethod(getClass(), "getName");
        }

        StringBuffer result = new StringBuffer().append(
                getClass().getSimpleName()).append("[");
        if (method != null) {
            try {
                result.append(method.invoke(this));
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.error("While serializing to string", e);
                }
            }
        }
        result.append("]");

        return result.toString();
    }
}
