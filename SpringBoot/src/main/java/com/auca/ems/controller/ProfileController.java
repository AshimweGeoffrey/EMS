package com.auca.ems.controller;

import com.auca.ems.model.User;
import com.auca.ems.service.FileStorageService;
import com.auca.ems.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @GetMapping
    public String viewProfile(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // Get fresh user data from database
        User user = userService.findById(currentUser.getId());
        model.addAttribute("user", user);
        return "profile";
    }
    
    @PostMapping("/update")
    public String updateProfile(@RequestParam(required = false) String username,
                              @RequestParam(required = false) String currentPassword,
                              @RequestParam(required = false) String newPassword,
                              @RequestParam(required = false) MultipartFile profileImage,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // Get fresh user data
        User user = userService.findById(currentUser.getId());
        
        try {
            // Update username if provided and different from current
            if (username != null && !username.trim().isEmpty() && !username.equals(user.getUsername())) {
                // Check if username is already taken
                if (userService.findByUsername(username) != null) {
                    redirectAttributes.addFlashAttribute("error", "Username already taken");
                    return "redirect:/profile";
                }
                user.setUsername(username);
            }
            
            // Update password if provided
            if (currentPassword != null && !currentPassword.isEmpty() && 
                newPassword != null && !newPassword.isEmpty()) {
                // Verify current password
                if (!currentPassword.equals(user.getPassword())) {
                    redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
                    return "redirect:/profile";
                }
                user.setPassword(newPassword);
            }
            
            // Update profile picture if provided
            if (profileImage != null && !profileImage.isEmpty()) {
                String fileName = fileStorageService.storeFile(profileImage);
                user.setProfilePicture(fileName);
            }
            
            userService.save(user);
            
            // Update session with new user data
            session.setAttribute("user", user);
            
            redirectAttributes.addFlashAttribute("message", "Profile updated successfully!");
            return "redirect:/profile";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
            return "redirect:/profile";
        }
    }
}
