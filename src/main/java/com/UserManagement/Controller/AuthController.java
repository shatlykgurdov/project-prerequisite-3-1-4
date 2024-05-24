package com.UserManagement.Controller;

import com.UserManagement.Dto.UserDto;
import com.UserManagement.Entity.User;
import com.UserManagement.service.UserService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.userdetails.UserDetails;

@Controller
public class AuthController {

	@Autowired
	private UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@ModelAttribute
	public void addAttributes(Model model, Principal principal, Authentication auth) {
		if (principal != null) {
			String username = principal.getName();
			model.addAttribute("username", username);
			Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
			String roles = authorities.stream()
					.map(GrantedAuthority::getAuthority)
					.collect(Collectors.joining(", "));
			model.addAttribute("roles", roles);
		}
	}


	@GetMapping("/")
	public String home() {
		return "redirect:/login";
	}

	@GetMapping("/login")
	public String loginForm() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal())) {
			// Check user roles and redirect accordingly
			Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
			String roles = authorities.stream()
					.map(GrantedAuthority::getAuthority)
					.collect(Collectors.joining(", "));
			if (roles.contains("ADMIN")) {
				return "redirect:/admin";
			} else {
				return "redirect:/users";
			}
		}
		return "login";
	}


	@GetMapping("/register")
	public String showRegistrationForm(Model model) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDto user = new UserDto();
		model.addAttribute("user", user);
		if (authentication != null
				&& authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal())) {
			return "redirect:/users";
		} else {

			return "register";
		}
	}

	@PostMapping("/register/save")
	public String registration(
			@Valid @ModelAttribute("user") UserDto userDto,
			BindingResult result,
			Model model) {

		User existingUser = userService.findUserByEmail(userDto.getEmail());

		if (existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()) {
			result.rejectValue("email", null,
					"There is already an account registered with the same email");
		}

		if (!userDto.getPassword().isEmpty()) {
			if (userDto.getPassword().length() < 7) {
				result.rejectValue("password", "field.min.length", "Password should have at least 7 characters");
			}
		}else{
			result.rejectValue("password", "field.min.length", "Password should not be empty.");
		}

		if (result.hasErrors()) {
			model.addAttribute("user", userDto);
			return "register";
		}

		userService.saveUser(userDto);
		return "redirect:/register?success=true";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/add/save")
	public String addUser(
			@Valid @ModelAttribute("user") UserDto userDto,
			BindingResult result,
			Model model) {
		User existingUser = userService.findUserByEmail(userDto.getEmail());

		if (existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()) {
			result.rejectValue("email", null,
					"There is already an account registered with the same email");
		}

		if (!userDto.getPassword().isEmpty()) {
			if (userDto.getPassword().length() < 7) {
				result.rejectValue("password", "field.min.length", "Password should have at least 7 characters");
			}
		}else{
			result.rejectValue("password", "field.min.length", "Password should not be empty.");
		}

		if (result.hasErrors()) {
			model.addAttribute("user", userDto);
			return "add";
		}

		userService.saveUser(userDto);
		return "redirect:/admin?success=true";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/add")
	public String showUserAddForm(Model model) {
		UserDto user = new UserDto();
		model.addAttribute("user", user);
		return "add";
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
	@GetMapping("/users")
	public String users(Model model) {
		List<UserDto> users = userService.findAllUsers();
		model.addAttribute("users", users);
		return "user";
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
	@GetMapping("/admin")
	public String admin(Model model) {
		List<UserDto> users = userService.findAllUsers();
		model.addAttribute("users", users);
		return "admin";
	}


	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/edit/{id}")
	public String editUser(
			@PathVariable Long id,
			Model model) {
		UserDto user = userService.findUserById(id);
		model.addAttribute("user", user);
		return "edit";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/edit/{id}")
	public String updateUserById(
			@Valid @ModelAttribute("user") UserDto updatedUserDto,
			BindingResult result,
			@PathVariable Long id,
			Model model) {

		// Password validation if not empty
		if (!updatedUserDto.getPassword().isEmpty() && updatedUserDto.getPassword().length() < 7) {
			result.rejectValue("password", "field.min.length", "Password should have at least 7 characters");
		}

		if (result.hasErrors()) {
			model.addAttribute("user", updatedUserDto);
			return "edit";
		}

		try {
			// Update the user
			userService.editUser(updatedUserDto, id);
		} catch (EntityNotFoundException e) {
			result.reject("globalError", e.getMessage());
			model.addAttribute("user", updatedUserDto);
			return "edit";
		} catch (Exception e) {
			result.reject("globalError", "An unexpected error occurred: " + e.getMessage());
			model.addAttribute("user", updatedUserDto);
			return "edit";
		}

		return "redirect:/admin";
	}



	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/delete/{id}")
	public String deleteUser(
			@RequestParam(name = "error", required = false) String error,
			@RequestParam(name = "success", required = false) String success,
			RedirectAttributes redirectAttributes,
			@PathVariable Long id,
			Principal principal,
			Model model) {

		String loggedInUsername = principal.getName();

		User loggedInUser = userService.findUserByEmail(loggedInUsername);

		if (loggedInUser != null && loggedInUser.getId().equals(id)) {
			if (error != null) {
				redirectAttributes.addFlashAttribute("error", "You cannot delete yourself.");
			}
		} else {
			if (userService.doesUserExist(id)) {
				userService.deleteUserById(id);
				if (success != null) {
					redirectAttributes.addFlashAttribute("success", "User has been deleted successfully");
				}
			} else {
				if (error != null) {
					redirectAttributes.addFlashAttribute("error", "User does not exist");
				}
			}
		}
		return "redirect:/admin";
	}

}
