package com.treasure.restart.helper.exception;

import com.treasure.restart.base.BaseResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> handleBusinessException(BusinessException e) {
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * @Valid 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleValidationException(
            MethodArgumentNotValidException e
    ) {
        String message = e.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return BaseResponse.error(400, message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public BaseResponse<?> handleNoResourceException(NoResourceFoundException e) {
        return BaseResponse.error(404, e.getMessage());
    }

    /**
     * 其他异常
     */
    @ExceptionHandler(Exception.class)
    public BaseResponse<?> handleException(Exception e) {
        e.printStackTrace();
        return BaseResponse.error(500, "服务器内部错误");
    }
}