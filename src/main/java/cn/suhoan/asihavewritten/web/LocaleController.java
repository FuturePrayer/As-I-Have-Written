package cn.suhoan.asihavewritten.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LocaleController {

    @GetMapping("/ui/language")
    public String switchLanguage(@RequestParam(defaultValue = "/ui/logs") String redirect) {
        if (!isSafeRedirect(redirect)) {
            return "redirect:/ui/logs";
        }
        return "redirect:" + redirect;
    }

    private boolean isSafeRedirect(String redirect) {
        return redirect.startsWith("/") && !redirect.startsWith("//") && !redirect.contains("://");
    }
}
