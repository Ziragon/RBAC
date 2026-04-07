package com.example.repository;

import com.example.filters.AssignmentFilter;
import com.example.assignment.RoleAssignment;
import com.example.assignment.TemporaryAssignment;
import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.entity.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {
    private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();

    @Override
    public synchronized void add(RoleAssignment item) {
        if (item == null) throw new IllegalArgumentException("Assignment cannot be null");

        if (userHasRole(item.user(), item.role())) {
            throw new IllegalStateException("User already has an active assignment for this role");
        }

        assignments.put(item.assignmentId(), item);
    }

    @Override
    public boolean remove(RoleAssignment item) {
        if (item == null) return false;
        return assignments.remove(item.assignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        return assignments.values().stream()
                .filter(a -> a.user().equals(user))
                .toList();
    }

    public List<RoleAssignment> findByRole(Role role) {
        return assignments.values().stream()
                .filter(a -> a.role().equals(role))
                .toList();
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignments.values().stream()
                .filter(RoleAssignment::isActive)
                .toList();
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return assignments.values().stream()
                .filter(a -> a instanceof TemporaryAssignment temp && temp.isExpired())
                .toList();
    }

    public boolean userHasRole(User user, Role role) {
        return assignments.values().stream()
                .anyMatch(a -> a.user().equals(user) &&
                        a.role().equals(role) &&
                        a.isActive());
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        return getUserPermissions(user).stream()
                .anyMatch(p -> p.matches(permissionName, resource));
    }

    public Set<Permission> getUserPermissions(User user) {
        return findByUser(user).stream()
                .filter(RoleAssignment::isActive)
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    // в TemporaryAssignment не было поля revoked, но было isExpired (по заданию)
    // логично было туда его добавить, чтобы TemporaryAssignment можно было легко отозвать по методу из интерфейса
    public void revokeAssignment(String assignmentId) {
        RoleAssignment assignment = findById(assignmentId)
                .orElseThrow(() -> new NoSuchElementException("Assignment not found"));
        assignment.revoke();
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        RoleAssignment assignment = findById(assignmentId)
                .orElseThrow(() -> new NoSuchElementException("Assignment not found"));

        if (assignment instanceof TemporaryAssignment temp) {
            temp.extend(newExpirationDate);
        } else {
            throw new IllegalArgumentException("Cannot extend non-temporary assignment");
        }
    }

    public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
        return assignments.values().parallelStream()
                .filter(filter::test)
                .toList();
    }

    public List<RoleAssignment> findAllParallel(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        return assignments.values().parallelStream()
                .filter(filter::test)
                .sorted(sorter)
                .toList();
    }

    public boolean hasAssignmentsForRole(Role role) {
        return assignments.values().stream()
                .anyMatch(a -> a.role().equals(role));
    }

    public int cleanupExpiredAssignments() {
        List<String> expiredIds = assignments.values().stream()
                .filter(a -> a instanceof TemporaryAssignment temp && temp.isExpired())
                .map(RoleAssignment::assignmentId)
                .toList();

        int removedCount = 0;
        for (String id : expiredIds) {
            if (assignments.remove(id) != null) {
                removedCount++;
            }
        }
        return removedCount;
    }
}