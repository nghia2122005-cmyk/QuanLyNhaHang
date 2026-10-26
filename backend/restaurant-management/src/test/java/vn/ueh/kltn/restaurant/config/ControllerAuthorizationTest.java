package vn.ueh.kltn.restaurant.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ueh.kltn.restaurant.controller.AuthController;
import vn.ueh.kltn.restaurant.dto.request.LoginRequest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class ControllerAuthorizationTest {
    @Test
    void everyProductionHandlerDeclaresAuthorizationExceptTheExactPublicLogin() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Method publicLogin = AuthController.class.getMethod("login", LoginRequest.class);
        List<String> missing = new ArrayList<>();
        int handlers = 0;
        for (BeanDefinition definition :
                scanner.findCandidateComponents("vn.ueh.kltn.restaurant.controller")) {
            Class<?> controller = Class.forName(definition.getBeanClassName());
            for (Method method : controller.getMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) continue;
                handlers++;
                if (!method.equals(publicLogin) && !isProtected(controller, method)) {
                    missing.add(controller.getSimpleName() + "." + method.getName());
                }
            }
        }
        assertTrue(handlers >= 21, "The production handler scan must not be empty or partial");
        assertTrue(missing.isEmpty(), "Missing @PreAuthorize: " + missing);
    }

    @Test
    void guardDetectsMissingAnnotationsAndAcceptsClassOrMethodAuthorization() throws Exception {
        assertFalse(isProtected(Unprotected.class, Unprotected.class.getMethod("handler")));
        assertTrue(isProtected(ClassProtected.class, ClassProtected.class.getMethod("handler")));
        assertTrue(isProtected(MethodProtected.class, MethodProtected.class.getMethod("handler")));
    }

    private boolean isProtected(Class<?> controller, Method method) {
        return AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class)
                || AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class);
    }

    static class Unprotected {
        @GetMapping
        public void handler() {}
    }

    @PreAuthorize("isAuthenticated()")
    static class ClassProtected {
        @GetMapping
        public void handler() {}
    }

    static class MethodProtected {
        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public void handler() {}
    }
}
