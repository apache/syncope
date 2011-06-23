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

import java.util.Collection;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.util.ApplicationContextManager;
import org.syncope.core.util.JexlUtil;

@MappedSuperclass
public abstract class AbstractDerAttr extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    public Long getId() {
        return id;
    }

    /**
     * @see http://commons.apache.org/jexl/reference/index.html
     * @param attributes the set of attributes against which evaluate this
     * derived attribute
     * @return the value of this derived attribute
     */
    public String getValue(
            final Collection<? extends AbstractAttr> attributes) {

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();
        JexlUtil jexlUtil = (JexlUtil) context.getBean("jexlUtil");

        // Prepare context using user attributes
        JexlContext jexlContext = new MapContext();

        jexlContext = jexlUtil.addAttributesToContext(
                attributes, jexlContext);

        // Evaluate expression using the context prepared before
        return jexlUtil.evaluateWithAttributes(
                getDerivedSchema().getExpression(), jexlContext);
    }

    public abstract <T extends AbstractAttributable> T getOwner();

    public abstract <T extends AbstractAttributable> void setOwner(T owner);

    public abstract <T extends AbstractDerSchema> T getDerivedSchema();

    public abstract <T extends AbstractDerSchema> void setDerivedSchema(
            T derivedSchema);
}
