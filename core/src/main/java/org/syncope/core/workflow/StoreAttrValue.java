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
package org.syncope.core.workflow;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.WorkflowException;
import java.util.Date;
import java.util.Map;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.util.AttributableUtil;
import org.syncope.types.SchemaType;

public class StoreAttrValue extends OSWorkflowComponent
        implements FunctionProvider {

    private AttributableUtil attributableUtil;

    private AbstractAttr getAttribute(Map transientVars, Map args)
            throws WorkflowException {

        String schemaName = (String) args.get("schema");
        if (schemaName == null || schemaName.length() == 0) {
            throw new WorkflowException("Must specify schema name");
        }

        String kind = (String) args.get("kind");
        if (kind == null || kind.length() == 0) {
            kind = Constants.SYNCOPE_USER;
        }

        AbstractAttributable attributable = null;
        if (Constants.SYNCOPE_USER.equals(kind)) {
            attributableUtil = AttributableUtil.USER;
            attributable = (SyncopeUser) transientVars.get(
                    Constants.SYNCOPE_USER);
        } else if (Constants.SYNCOPE_ROLE.equals(kind)) {
                attributableUtil = AttributableUtil.ROLE;
                attributable = (SyncopeRole) transientVars.get(
                        Constants.SYNCOPE_ROLE);
            } else if (Constants.MEMBERSHIP.equals(kind)) {
                    attributableUtil = AttributableUtil.MEMBERSHIP;
                    attributable = (Membership) transientVars.get(
                            Constants.MEMBERSHIP);
                } else {
                    throw new WorkflowException(
                            "Invalid attributable specified: " + kind);
                }
        if (attributable == null) {
            throw new WorkflowException("Could not find instance "
                    + attributableUtil);
        }

        SchemaDAO schemaDAO = (SchemaDAO) context.getBean("schemaDAOImpl");
        AbstractSchema schema = schemaDAO.find(schemaName,
                attributableUtil.schemaClass());
        if (schema == null) {
            throw new WorkflowException("Invalid schema: " + schemaName);
        }

        AbstractAttr attribute = attributable.getAttribute(schemaName);
        if (attribute == null) {
            attribute = attributableUtil.newAttribute();
            attribute.setSchema(schema);
            attribute.setOwner(attributable);
            attributable.addAttribute(attribute);
        }

        return attribute;
    }

    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        AbstractAttr attribute = getAttribute(transientVars, args);

        String val = (String) transientVars.get(args.get("schema"));

        if (val != null && !val.isEmpty()) {
            attribute.addValue(val, attributableUtil);
        } else if (attribute.getSchema().getType() == SchemaType.Date) {
                attribute.addValue(
                        attribute.getSchema().getFormatter().format(
                        new Date()), attributableUtil);
            }
    }
}
