package cn.suhoan.asihavewritten.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentPath")
    String currentPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (request.getQueryString() != null) {
            path += "?" + request.getQueryString();
        }
        return path;
    }
}
