package org.ninng.businesssvc.version;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.constant.HttpConstant;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 版本匹配条件，实现两阶段匹配逻辑：
 * <ol>
 * <li>基线匹配：精确版本精确比对，{@code +} 版本做范围匹配并检查是否被更具体的固定版本阻塞</li>
 * <li>优先级排序：固定版本优先于范围版本，高版本优先于低版本</li>
 * </ol>
 */
public class ApiVersionRequestCondition implements RequestCondition<ApiVersionRequestCondition> {

    private static final Set<ApiVersionRequestCondition> REGISTRY = ConcurrentHashMap.newKeySet();
    private final int major;
    private final int minor;
    private final boolean minVersion;
    private final String source;
    private final boolean deprecated;
    private final String sunset;

    public ApiVersionRequestCondition(String version, boolean deprecated, String sunset) {
        version = version.toLowerCase()
                .startsWith("v") ? version.substring(1) : version;
        this.source = version;
        this.deprecated = deprecated;
        this.sunset = sunset;
        this.minVersion = version.endsWith("+");
        String v = this.minVersion ? version.substring(0, version.length() - 1) : version;
        String[] parts = v.split("\\.");
        this.major = Integer.parseInt(parts[0]);
        this.minor = Integer.parseInt(parts[1]);

        REGISTRY.add(this);
    }

    static void clearRegistry() {
        REGISTRY.clear();
    }

    /**
     * 解析版本号字符串，支持 "v1.2", "1.2", "1.2+" 等格式。
     */
    private static int[] parseVersion(String v) {
        String trimmed = v.trim();
        if (trimmed.toLowerCase()
                .startsWith("v")) {
            trimmed = trimmed.substring(1);
        }
        // 去掉可能存在的 + 后缀
        if (trimmed.endsWith("+")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        String[] parts = trimmed.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return new int[]{major, minor};
    }

    /**
     * 两阶段匹配：
     * <ul>
     * <li>未携带 {@code API-Version} 请求头 → 回退匹配（向后兼容），让最高优先级的处理器处理</li>
     * <li>携带版本头 → 严格比对，并检查是否被更具体的固定版本阻塞</li>
     * </ul>
     */
    @Override
    public ApiVersionRequestCondition getMatchingCondition(HttpServletRequest request) {
        String header = request.getHeader(HttpConstant.API_VERSION);
        if (header == null || header.isBlank()) {
            // 未指定版本，不进行版本过滤，匹配此处理器
            return this;
        }

        int[] parsed;
        try {
            parsed = parseVersion(header);
        } catch (Exception e) {
            return null;
        }
        int reqMajor = parsed[0], reqMinor = parsed[1];

        if (minVersion) {
            // 范围匹配：同主版本号且请求版本 >= 最低版本
            if (reqMajor != major || reqMinor < minor) {
                return null;
            }
            // 检查是否被更具体的固定版本阻塞
            for (ApiVersionRequestCondition other : REGISTRY) {
                if (other == this || other.minVersion) {
                    continue;
                }
                if (other.major == major && other.minor > minor && other.minor == reqMinor) {
                    return null;
                }
            }
            return this;
        } else {
            // 精确匹配
            if (reqMajor == major && reqMinor == minor) {
                return this;
            }
            return null;
        }
    }

    @Override
    public @NonNull ApiVersionRequestCondition combine(@NonNull ApiVersionRequestCondition other) {
        // 方法级注解优先于类级注解
        return other;
    }

    /**
     * 优先级：固定版本 > 范围版本，高版本 > 低版本。
     */
    @Override
    public int compareTo(ApiVersionRequestCondition other, @NonNull HttpServletRequest request) {
        // 固定版本优先于范围版本
        if (this.minVersion != other.minVersion) {
            return this.minVersion ? -1 : 1;
        }
        // 版本号越高优先级越高
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        return Integer.compare(this.minor, other.minor);
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public String getSunset() {
        return sunset;
    }

    public String getSource() {
        return source;
    }
}
