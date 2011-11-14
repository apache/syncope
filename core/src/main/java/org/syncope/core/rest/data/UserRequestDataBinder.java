/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.rest.data;

import javassist.NotFoundException;
import javax.persistence.RollbackException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserRequestTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.UserRequest;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.rest.controller.UnauthorizedRoleException;

@Component
public class UserRequestDataBinder {

    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private UserDAO userDAO;

    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public SyncopeUser getUserFromId(final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        if (userId == null) {
            throw new NotFoundException("Null user id");
        }

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        SyncopeUser authUser = userDAO.find(
                SecurityContextHolder.getContext().
                getAuthentication().getName());

        if (authUser == null || !authUser.equals(user)) {
            throw new UnauthorizedRoleException(-1L);
        }

        return user;
    }

    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO getAuthUserTO()
            throws NotFoundException {

        SyncopeUser authUser = userDAO.find(
                SecurityContextHolder.getContext().
                getAuthentication().getName());
        return userDataBinder.getUserTO(authUser.getId());
    }

    public UserRequestTO getUserRequestTO(final UserRequest request) {
        UserRequestTO result = new UserRequestTO();
        BeanUtils.copyProperties(request, result);

        return result;
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void testCreate(final UserTO userTO) {
        SyncopeUser user = new SyncopeUser();
        userDataBinder.create(user, userTO);
        userDAO.save(user);

        throw new RollbackException();
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void testUpdate(final UserMod userMod)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = getUserFromId(userMod.getId());
        userDataBinder.update(user, userMod);
        userDAO.save(user);

        throw new RollbackException();
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void testDelete(final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = getUserFromId(userId);
        userDAO.delete(user);

        throw new RollbackException();
    }
}
