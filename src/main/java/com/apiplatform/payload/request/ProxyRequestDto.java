package com.apiplatform.payload.request;

import lombok.Data;
import java.util.Map;

@Data
public class ProxyRequestDto {
    private String url;
    private String method; // GET, POST, etc.
    private Map<String, String> headers;
    private String body;
}