package cn.suhoan.asihavewritten.web;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cn.dev33.satoken.stp.StpUtil;
import cn.suhoan.asihavewritten.auth.AdminAuthService;

@Controller
public class AuthController {

    private final AdminAuthService authService;
    private final MessageSource messageSource;

    public AuthController(AdminAuthService authService, MessageSource messageSource) {
        this.authService = authService;
        this.messageSource = messageSource;
    }

    @PostMapping("/auth/login")
    public String login(@RequestParam String username, @RequestParam String password,
            @RequestParam(required = false) String redirect,
            RedirectAttributes attributes,
            Locale locale) {
        if (!authService.matches(username, password)) {
            attributes.addFlashAttribute("error", messageSource.getMessage("login.error", null, locale));
            if (isSafeRedirect(redirect)) {
                attributes.addAttribute("redirect", redirect);
            }
            return "redirect:/ui/login";
        }
        StpUtil.login(authService.loginId());
        return "redirect:" + (isSafeRedirect(redirect) ? redirect : "/ui/logs");
    }

    @PostMapping("/auth/logout")
    public String logout() {
        StpUtil.logout();
        return "redirect:/ui/login";
    }

    private boolean isSafeRedirect(String redirect) {
        return redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")
                && !redirect.contains("://");
    }
}
