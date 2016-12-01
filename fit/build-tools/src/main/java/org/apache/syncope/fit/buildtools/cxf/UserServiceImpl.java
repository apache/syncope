/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.fit.buildtools.cxf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private static final Map<UUID, User> USERS = new HashMap<UUID, User>();

    @Override
    public List<User> list() {
        return new ArrayList<>(USERS.values());
    }

    @Override
    public User read(final UUID key) {
        User user = USERS.get(key);
        if (user == null) {
            throw new NotFoundException(key.toString());
        }
        return user;
    }

    @Override
    public void create(final User user) {
        if (user.getKey() == null) {
            user.setKey(UUID.randomUUID());
        }
        if (USERS.containsKey(user.getKey())) {
            throw new IllegalArgumentException("User already exists: " + user.getKey());
        }
        USERS.put(user.getKey(), user);
    }

    @Override
    public void update(final UUID key, final User updatedUser) {
        if (!USERS.containsKey(key)) {
            throw new NotFoundException(updatedUser.getKey().toString());
        }
        User user = USERS.get(key);
        if (updatedUser.getUsername() != null) {
            user.setUsername(updatedUser.getUsername());
        }
        if (updatedUser.getPassword() != null) {
            user.setPassword(updatedUser.getPassword());
        }
        if (updatedUser.getFirstName() != null) {
            user.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getSurname() != null) {
            user.setSurname(updatedUser.getSurname());
        }
        if (updatedUser.getEmail() != null) {
            user.setEmail(updatedUser.getEmail());
        }
    }

    @Override
    public void delete(final UUID key) {
        if (!USERS.containsKey(key)) {
            throw new NotFoundException(key.toString());
        }
        USERS.remove(key);
    }

    @Override
    public User authenticate(final String username, final String password) {
        User user = null;
        for (User entry : USERS.values()) {
            if (username.equals(entry.getUsername())) {
                user = entry;
            }
        }
        if (user == null) {
            throw new NotFoundException(username);
        }
        if (!password.equals(user.getPassword())) {
            throw new ForbiddenException();
        }

        return user;
    }

    @Override
    public void clear() {
        USERS.clear();
    }

}
