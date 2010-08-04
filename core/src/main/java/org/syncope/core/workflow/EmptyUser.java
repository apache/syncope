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
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserDerivedAttribute;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.DerivedAttributeDAO;
import org.syncope.core.persistence.dao.MembershipDAO;

public class EmptyUser extends OSWorkflowComponent
        implements FunctionProvider {

    @Override
    @Transactional
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        SyncopeUser syncopeUser = (SyncopeUser) transientVars.get(
                Constants.SYNCOPE_USER);

        AttributeDAO attributeDAO =
                (AttributeDAO) context.getBean("attributeDAOImpl");
        for (AbstractAttribute attribute : syncopeUser.getAttributes()) {
            attributeDAO.delete(attribute.getId(), UserAttribute.class);
        }

        DerivedAttributeDAO derivedAttributeDAO =
                (DerivedAttributeDAO) context.getBean(
                "derivedAttributeDAOImpl");
        for (AbstractDerivedAttribute derivedAttribute :
                syncopeUser.getDerivedAttributes()) {

            derivedAttributeDAO.delete(derivedAttribute.getId(),
                    UserDerivedAttribute.class);
        }

        MembershipDAO membershipDAO =
                (MembershipDAO) context.getBean("membershipDAOImpl");
        for (Membership membership : syncopeUser.getMemberships()) {
            membershipDAO.delete(membership.getId());
        }

        transientVars.put(Constants.SYNCOPE_USER, syncopeUser);
    }
}
