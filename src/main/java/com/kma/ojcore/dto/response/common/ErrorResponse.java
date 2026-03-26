package com.kma.ojcore.dto.response.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kma.ojcore.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Getter
@Setter
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    int status;
    String message;
    String error;
    String errorCode;
    String path;
    Date timestamp;
}
