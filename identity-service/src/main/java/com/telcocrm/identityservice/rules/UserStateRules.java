package com.telcocrm.identityservice.rules;

import com.telcocrm.identityservice.entity.User;
import com.telcocrm.identityservice.entity.enums.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserStateRules {

    public void requireActiveForRoleAssignment(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only ACTIVE users can be assigned roles. User: " + user.getId() + " is " + user.getStatus());
        }
    }
}
