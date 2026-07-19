package com.telcocrm.identityservice.rules;

import com.telcocrm.identityservice.entity.User;
import com.telcocrm.identityservice.entity.enums.UserStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserStateRulesTest {

    private final UserStateRules userStateRules = new UserStateRules();

    @Test
    void shouldAllowRoleAssignmentWhenUserActive() {
        var user = User.builder().status(UserStatus.ACTIVE).build();

        assertThatCode(() -> userStateRules.requireActiveForRoleAssignment(user))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectRoleAssignmentWhenUserInactive() {
        var user = User.builder().status(UserStatus.INACTIVE).build();

        assertThatThrownBy(() -> userStateRules.requireActiveForRoleAssignment(user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void shouldRejectRoleAssignmentWhenUserSuspended() {
        var user = User.builder().status(UserStatus.SUSPENDED).build();

        assertThatThrownBy(() -> userStateRules.requireActiveForRoleAssignment(user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }
}
