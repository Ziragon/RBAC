package com.example.repository;

import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.filters.RoleFilter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RoleManager implements Repository<Role> {

    private final Map<String, Role> rolesById = new ConcurrentHashMap<>();
    private final Map<String, Role> rolesByName = new ConcurrentHashMap<>();

    // нужен для проверки назначений при удалении
    private final AssignmentManager assignmentManager;

    public RoleManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public synchronized void add(Role role) {
        if (role == null) throw new IllegalArgumentException("Role cannot be null");
        if (exists(role.getName())) {
            throw new IllegalArgumentException("Role with name " + role.getName() + " already exists");
        }
        rolesById.put(role.getId(), role);
        rolesByName.put(role.getName(), role);
    }

    @Override
    public synchronized boolean remove(Role role) {
        if (role == null || !rolesById.containsKey(role.getId())) return false;

        if (assignmentManager != null && assignmentManager.hasAssignmentsForRole(role)) {
            throw new IllegalStateException("Cannot remove role: it is currently assigned to users");
        }

        rolesById.remove(role.getId());
        rolesByName.remove(role.getName());
        return true;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(rolesById.get(id));
    }

    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesByName.get(name));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public synchronized void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    public boolean exists(String name) {
        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Role role = findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleName));
        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Role role = findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleName));
        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        return rolesById.values().stream()
                .filter(role -> role.hasPermission(permissionName, resource)) //
                .toList();
    }

    public List<Role> findByFilter(RoleFilter filter) {
        return rolesById.values().stream()
                .filter(filter::test)
                .toList();
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        return rolesById.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .toList();
    }
}