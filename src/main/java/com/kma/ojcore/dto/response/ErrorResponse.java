package com.kma.ojcore.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
public class ErrorResponse {
    private int status;
    private String message;
    private String error;
    private String path;
    private Date timestamp;
}
