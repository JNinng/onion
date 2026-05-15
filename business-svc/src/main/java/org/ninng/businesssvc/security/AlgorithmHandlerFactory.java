package org.ninng.businesssvc.security;

import org.ninng.businesssvc.component.I18nUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AlgorithmHandlerFactory {

    private final Map<String, AlgorithmHandler> algorithms;
    private final I18nUtil i18nUtil;

    public AlgorithmHandlerFactory(List<AlgorithmHandler> algorithmHandlers, I18nUtil i18nUtil) {
        this.i18nUtil = i18nUtil;
        algorithms = new HashMap<>();
        for (AlgorithmHandler algorithmHandler : algorithmHandlers) {
            if (algorithms.containsKey(algorithmHandler.support())) {
                throw new IllegalArgumentException(i18nUtil.getMessage("security.algorithmRepeat",
                        new String[]{algorithmHandler.support()}));
            }
            algorithms.put(algorithmHandler.support(), algorithmHandler);
        }
    }

    public AlgorithmHandler getAlgorithm(String algorithmName) {
        if (!algorithms.containsKey(algorithmName)) {
            throw new IllegalArgumentException(i18nUtil.getMessage("security.algorithmNotExists",
                    new String[]{algorithmName}));
        }
        return algorithms.get(algorithmName);
    }

    public List<String> getAlgorithmNames() {
        return algorithms.keySet()
                .stream()
                .toList();
    }
}
