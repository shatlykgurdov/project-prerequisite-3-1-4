package com.UserManagement.service.Impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.UserManagement.Dto.UserDto;
import com.UserManagement.Entity.Role;
import com.UserManagement.Entity.User;
import com.UserManagement.Repository.RoleRepository;
import com.UserManagement.Repository.UserRepository;
import com.UserManagement.service.UserService;

import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void saveUser(UserDto userDto) {
        User user = new User();
        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setAge(userDto.getAge());
        Role role = roleRepository.findByName(userDto.getRole());
        user.setRoles(Arrays.asList(role));
        userRepository.save(user);
    }

    @Override
    public void deleteUserById(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        userOptional.ifPresent(user -> {
            user.getRoles().clear();
            userRepository.delete(user);
        });
    }

    @Override
    public boolean doesUserExist(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.isPresent();
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserDto findUserById(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if(userOptional.isPresent()){
            return mapToUserDto(userOptional.get());
        }
        return null;
    }

    @Override
    public void editUser(UserDto updatedUserDto, Long userId) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        existingUser.setName(updatedUserDto.getFirstName() + " " + updatedUserDto.getLastName());
        existingUser.setAge(updatedUserDto.getAge());

        if (!updatedUserDto.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUserDto.getPassword()));
        }

        Role role = roleRepository.findByName(updatedUserDto.getRole());
        if (role == null) {
            throw new EntityNotFoundException("Role not found");
        }

        // Ensure the roles collection is mutable
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        existingUser.setRoles(roles);

        userRepository.save(existingUser);
    }


    @Override
    public List<UserDto> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    private UserDto mapToUserDto(User user){
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        String[] str = user.getName().split(" ");
        if (str.length > 1) {
            userDto.setFirstName(str[0]);
            userDto.setLastName(str[1]);
        } else {
            userDto.setFirstName(user.getName());
            userDto.setLastName("");
        }
        userDto.setEmail(user.getEmail());
        userDto.setAge(user.getAge());
        userDto.setRole(user.getRoles().get(0).getName());
        return userDto;
    }
}
