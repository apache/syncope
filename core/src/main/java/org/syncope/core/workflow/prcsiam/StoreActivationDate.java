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
package org.syncope.core.workflow.prcsiam;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.WorkflowException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.validation.ValidationException;
import org.syncope.core.workflow.Constants;
import org.syncope.core.workflow.OSWorkflowComponent;

public class StoreActivationDate extends OSWorkflowComponent
        implements FunctionProvider {

    @Override
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        SyncopeUser syncopeUser = (SyncopeUser) transientVars.get(
                Constants.SYNCOPE_USER);

        SchemaDAO schemaDAO = (SchemaDAO) context.getBean("schemaDAOImpl");
        UserSchema schema = schemaDAO.find("activationDate", UserSchema.class);

        UserAttribute attribute = new UserAttribute();
        attribute.setSchema(schema);
        attribute.setOwner(syncopeUser);
        syncopeUser.addAttribute(attribute);

        SimpleDateFormat sdf = new SimpleDateFormat(
                schema.getConversionPattern());
        try {
            attribute.addValue(sdf.format(new Date()),
                    new UserAttributeValue());
        } catch (ValidationException e) {
            log.error("Could not store activation date", e);
        }
    }
}
