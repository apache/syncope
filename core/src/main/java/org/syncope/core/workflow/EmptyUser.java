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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

        SyncopeUser user = (SyncopeUser) transientVars.get(
                Constants.SYNCOPE_USER);

        final AttributeDAO attributeDAO =
                (AttributeDAO) context.getBean("attributeDAOImpl");
        for (AbstractAttribute attribute : user.getAttributes()) {
            attributeDAO.delete(attribute.getId(), UserAttribute.class);
        }
        user.getAttributes().clear();

        final DerivedAttributeDAO derivedAttributeDAO =
                (DerivedAttributeDAO) context.getBean(
                "derivedAttributeDAOImpl");
        for (AbstractDerivedAttribute derivedAttribute :
                user.getDerivedAttributes()) {

            derivedAttributeDAO.delete(derivedAttribute.getId(),
                    UserDerivedAttribute.class);
        }
        user.getDerivedAttributes().clear();

        final MembershipDAO membershipDAO =
                (MembershipDAO) context.getBean("membershipDAOImpl");
        final Set<Long> membershipIds =
                new HashSet<Long>(user.getMemberships().size());
        for (Membership membership : user.getMemberships()) {
            membershipIds.add(membership.getId());
        }
        for (Long membershipId : membershipIds) {
            membershipDAO.delete(membershipId);
        }

        user.setPassword(null);

        transientVars.put(Constants.SYNCOPE_USER, user);
    }
}
