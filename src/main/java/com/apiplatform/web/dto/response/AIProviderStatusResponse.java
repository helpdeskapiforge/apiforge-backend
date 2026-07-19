package com.apiplatform.web.dto.response;

import java.util.List;

public record AIProviderStatusResponse(
        List<Provider> providers,
        String activeProvider
) {
    public record Provider(String name, boolean available, String model) {
    }
}
