package controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import models.accounts.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserJsonStore {
    private static final Path USERS_FILE = Path.of("data", "users.json");
    private static final TypeReference<List<User>> USER_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public UserJsonStore() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<User> getAllUsers() throws IOException {
        ensureStorageExists();
        return new ArrayList<>(objectMapper.readValue(USERS_FILE.toFile(), USER_LIST_TYPE));
    }

    public Optional<User> authenticate(String phoneNumber, String password) throws IOException {
        return getAllUsers().stream()
                .filter(user -> user.getPhoneNumber().equals(phoneNumber) && user.getPassword().equals(password))
                .findFirst();
    }

    public boolean phoneNumberExists(String phoneNumber) throws IOException {
        return getAllUsers().stream().anyMatch(user -> user.getPhoneNumber().equals(phoneNumber));
    }

    public void saveUser(User user) throws IOException {
        List<User> users = getAllUsers();
        users.add(user);
        objectMapper.writeValue(USERS_FILE.toFile(), users);
    }

    private void ensureStorageExists() throws IOException {
        Path parent = USERS_FILE.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        if (Files.notExists(USERS_FILE)) {
            objectMapper.writeValue(USERS_FILE.toFile(), new ArrayList<User>());
        }
    }
}
