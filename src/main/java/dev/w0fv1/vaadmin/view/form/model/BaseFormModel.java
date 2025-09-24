package dev.w0fv1.vaadmin.view.form.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class BaseFormModel {

    // 深拷贝

    /**
     * Deep-copy this form model, including its vaadminFormConfig map.
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseFormModel> T copy() {
        try {
            // Create new instance of the same runtime class
            T copyInstance = (T) this.getClass().getDeclaredConstructor().newInstance();

            // Copy all declared fields (including private) from this to the copy
            Class<?> cls = this.getClass();
            while (cls != null && !cls.equals(BaseFormModel.class.getSuperclass())) {
                for (Field field : cls.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(FormIgnore.class)) {
                        continue;
                    }
                    field.set(copyInstance, field.get(this));
                }
                cls = cls.getSuperclass();
            }
            // Copy the vaadminFormConfig map
            copyInstance.getVaadminFormConfig().putAll(this.vaadminFormConfig);

            return copyInstance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy form model", e);
        }
    }

    // 配置字段
    @FormIgnore
    private final Map<String, Object> vaadminFormConfig = new HashMap<>();

    @JsonIgnore
    public void setVaadminFormConfig(Map<String, Object> config) {
        this.vaadminFormConfig.clear();
        if (config != null) {
            this.vaadminFormConfig.putAll(config);
        }
    }

    /**
     * Load config entries from another BaseFormModel instance.
     * This will overwrite existing keys if there's a conflict.
     *
     * @param other the source BaseFormModel from which to load config
     */
    @JsonIgnore
    public void loadConfig(BaseFormModel other) {
        if (other != null && other.getVaadminFormConfig() != null) {
            this.vaadminFormConfig.putAll(other.getVaadminFormConfig());
        }
    }

    @JsonIgnore
    public void addVaadminFormConfig(String key, Object value) {
        this.vaadminFormConfig.put(key, value);
    }

    // 🔽 通用类型安全 get 方法 🔽

    @JsonIgnore
    public String getConfigString(String key) {
        Object value = vaadminFormConfig.get(key);
        return value instanceof String ? (String) value : null;
    }

    @JsonIgnore
    public Long getConfigLong(String key) {
        Object value = vaadminFormConfig.get(key);
        if (value instanceof Long) return (Long) value;
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }


    @JsonIgnore
    public Integer getConfigInteger(String key) {
        Object value = vaadminFormConfig.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    @JsonIgnore
    public Boolean getConfigBoolean(String key) {
        Object value = vaadminFormConfig.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    @JsonIgnore
    public Double getConfigDouble(String key) {
        Object value = vaadminFormConfig.get(key);
        if (value instanceof Double) return (Double) value;
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
