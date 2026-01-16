package com.apiplatform.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class ProxyResponseDto {
    private int status;
    private long timeMs; // Execution time
    private Map<String, String> headers;
    private Object body; // Can be JSON object or String
}