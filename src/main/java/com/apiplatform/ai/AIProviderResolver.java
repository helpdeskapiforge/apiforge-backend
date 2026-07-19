package com.apiplatform.ai;

import com.apiplatform.config.AIProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Picks the {@link AIProvider} to use for the current request.
 * <p>
 * Adding a fifth provider means writing one new {@link AIProvider} implementation and
 * adding its name to {@code ai.provider-priority} -- nothing here or in any feature
 * service changes.
 */
@Component
public class AIProviderResolver {

    private final Map<String, AIProvider> providersByName;
    private final AIProperties properties;

    public AIProviderResolver(List<AIProvider> providers, AIProperties properties) {
        this.providersByName = providers.stream()
                .collect(java.util.stream.Collectors.toMap(AIProvider::getProviderName, p -> p));
        this.properties = properties;
    }

    /**
     * @return the first available provider in configured priority order
     * @throws AIProviderException if none are available
     */
    public AIProvider resolve() {
        List<AIProvider> chain = resolveChain();
        if (chain.isEmpty()) {
            throw AIProviderException.noProviderAvailable();
        }
        return chain.get(0);
    }

    /**
     * Every provider that currently reports itself available, in priority order. A
     * provider reporting "available" only means it's reachable/configured
     * ({@link AIProvider#isAvailable()}) -- the actual completion call can still fail
     * (e.g. Ollama is up but the requested model was never pulled). Callers should try
     * these in order and fall back on failure rather than trusting the first one
     * unconditionally; see {@code AIGenerationService#callProviderAndPersist}.
     */
    public List<AIProvider> resolveChain() {
        return properties.getProviderPriority().stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .distinct()
                .map(providersByName::get)
                .filter(java.util.Objects::nonNull)
                .filter(AIProvider::isAvailable)
                .toList();
    }

    /** Best-effort peek, used by the provider-status endpoint. Never throws. */
    public Optional<AIProvider> tryResolve() {
        try {
            return Optional.of(resolve());
        } catch (AIProviderException e) {
            return Optional.empty();
        }
    }

    /** Status of every known provider, in priority order, for the frontend's settings/status panel. */
    public List<ProviderStatus> statusOfAll() {
        return properties.getProviderPriority().stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .distinct()
                .map(name -> {
                    AIProvider provider = providersByName.get(name);
                    boolean available = provider != null && provider.isAvailable();
                    String model = provider != null ? provider.getModel() : null;
                    return new ProviderStatus(name, available, model);
                })
                .toList();
    }

    public record ProviderStatus(String name, boolean available, String model) {
    }
}
