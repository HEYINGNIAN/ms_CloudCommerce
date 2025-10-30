package com.example.messageconsumer.exception;

import com.example.common.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理类
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return Result.fail(e.getMessage());
    }

    /**
     * 处理异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.fail("系统内部错误，请稍后重试");
    }
}