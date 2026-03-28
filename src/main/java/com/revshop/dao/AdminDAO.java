package com.revshop.dao;

import com.revshop.entity.Role;
import com.revshop.entity.User;

import java.util.List;
import java.util.Optional;

public interface AdminDAO {

    long countUsers(Boolean includeDeleted);

    long countUsersByRole(Role role, Boolean includeDeleted);

    long countUsersByActive(boolean active, Boolean includeDeleted);

    long countDeletedUsers();

    long countProducts();

    long countOrders();

    long countPayments();

    List<User> searchUsers(String keyword, Role role, Boolean active, int offset, int limit);

    long countSearchUsers(String keyword, Role role, Boolean active);

    Optional<User> findUserByIdAnyState(Long userId);

    User saveUser(User user);
}
