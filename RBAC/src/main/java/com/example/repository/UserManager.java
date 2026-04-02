package com.example.repository;

import com.example.entity.User;
import com.example.filters.UserFilter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager implements Repository<User> {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public void add(User item) {
        if (item == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (users.putIfAbsent(item.username(), item) != null) {
            throw new IllegalArgumentException("User with username " + item.username() + " already exists");
        }
    }

    @Override
    public boolean remove(User item) {
        if (item == null) return false;
        return users.remove(item.username()) != null;
    }

    // у user нету id - поиск по username
    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return users.values().stream()
                .filter(u -> u.email().equalsIgnoreCase(email))
                .findFirst();
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }

    // record неизменяем, создаем новую сущность
    public void update(String username, String newFullName, String newEmail) {
        User updatedUser = new User(username, newFullName, newEmail);

        if (users.replace(username, updatedUser) == null) {
            throw new NoSuchElementException("User not found: " + username);
        }
    }

    public List<User> findByFilterParallel(UserFilter filter) {
        return users.values().parallelStream()
                .filter(filter::test)
                .toList();
    }

    public List<User> findAllParallel(UserFilter filter, Comparator<User> sorter) {
        return users.values().parallelStream()
                .filter(filter::test)
                .sorted(sorter)
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserManager that = (UserManager) o;
        return Objects.equals(users, that.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(users);
    }
}