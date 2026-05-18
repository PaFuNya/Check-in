package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("studentId") == null) {
            return "redirect:/login";
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/checkin")
    public String checkin(HttpSession session) {
        if (session.getAttribute("studentId") == null) {
            return "redirect:/login";
        }
        return "checkin";
    }
}
