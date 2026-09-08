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

import java.util.concurrent.TimeUnit;

/**
 * 모든 HTTP API의 시작/종료/실패를 같은 형식으로 기록한다.
 * 요청/응답 본문과 헤더는 로그에 남기지 않는다.
 */
@Aspect
@Component
@Slf4j
public class ApiLoggingAspect {

    @Around("execution(* net.vivans.dcim..api..*(..))")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        String method = request == null ? "-" : request.getMethod();
        String path = request == null ? "-" : request.getRequestURI();
        String handler = joinPoint.getSignature().toShortString();
        long startedAt = System.nanoTime();

        log.info("[API_START] method={} path={} handler={}", method, path, handler);
        try {
            Object result = joinPoint.proceed();
            log.info(
                    "[API_END] method={} path={} handler={} status={} elapsedMs={}",
                    method,
                    path,
                    handler,
                    response == null ? "-" : response.getStatus(),
                    elapsedMillis(startedAt)
            );
            return result;
        } catch (Throwable exception) {
            log.warn(
                    "[API_ERROR] method={} path={} handler={} elapsedMs={} exception={} message={}",
                    method,
                    path,
                    handler,
                    elapsedMillis(startedAt),
                    exception.getClass().getSimpleName(),
                    safeMessage(exception)
            );
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

    private static String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "-" : message.replaceAll("[\\r\\n]+", " ");
    }
}
