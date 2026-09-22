package net.vivans.dcim.shared.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 모든 HTTP API의 시작/종료/실패를 같은 형식으로 기록한다.
 * 요청/응답 본문과 헤더는 로그에 남기지 않는다.
 */
@Aspect
@Component
@Slf4j
public class ApiLoggingAspect {

    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "password", "passwd", "token", "secret", "authorization",
            "credential", "api_key", "apikey", "cookie"
    );

    @Around("execution(* net.vivans.dcim..api..*(..))")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        String method = request == null ? "-" : request.getMethod();
        String path = request == null ? "-" : request.getRequestURI();
        String url = request == null ? "-" : request.getRequestURL().toString();
        String query = safeQueryString(request);
        String handler = joinPoint.getSignature().toShortString();
        long startedAt = System.nanoTime();

        log.info("""
                ==================== API REQUEST START ====================
                Method  : {}
                URL     : {}
                Path    : {}
                Query   : {}
                Handler : {}
                ===========================================================""",
                method, url, path, query, handler);
        try {
            Object result = joinPoint.proceed();
            log.info("""
                    ===================== API REQUEST END =====================
                    Method  : {}
                    Path    : {}
                    Status  : {}
                    Elapsed : {} ms
                    Handler : {}
                    ===========================================================""",
                    method, path, response == null ? "-" : response.getStatus(),
                    elapsedMillis(startedAt), handler);
            return result;
        } catch (Throwable exception) {
            log.warn("""
                    ==================== API REQUEST ERROR ====================
                    Method    : {}
                    Path      : {}
                    Elapsed   : {} ms
                    Handler   : {}
                    Exception : {}
                    Message   : {}
                    ===========================================================""",
                    method, path, elapsedMillis(startedAt), handler,
                    exception.getClass().getSimpleName(), safeMessage(exception));
            throw exception;
        }
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    private static HttpServletResponse currentResponse() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getResponse();
        }
        return null;
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static String safeQueryString(HttpServletRequest request) {
        if (request == null || request.getQueryString() == null || request.getQueryString().isBlank()) {
            return "-";
        }
        return Arrays.stream(request.getQueryString().split("&"))
                .map(ApiLoggingAspect::redactQueryPart)
                .map(part -> part.replaceAll("[\\r\\n]+", " "))
                .reduce((left, right) -> left + "&" + right)
                .orElse("-");
    }

    private static String redactQueryPart(String part) {
        int separator = part.indexOf('=');
        String key = separator < 0 ? part : part.substring(0, separator);
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        boolean sensitive = SENSITIVE_QUERY_KEYS.stream().anyMatch(normalizedKey::contains);
        return sensitive ? key + "=***" : part;
    }

    private static String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "-" : message.replaceAll("[\\r\\n]+", " ");
    }
}
