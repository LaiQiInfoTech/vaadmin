package dev.w0fv1.vaadmin.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashMap;
import java.util.Map;

// JSON工具类，用于对象转Map，自动处理@JsonIgnore字段和Hibernate6懒加载代理
public class JsonUtil {

    // 单例ObjectMapper
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 注册Hibernate6模块
        Hibernate6Module hibernateModule = new Hibernate6Module();
        objectMapper.registerModule(hibernateModule);


        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
    }

    /**
     * 将对象安全转换为HashMap，兼容Hibernate6代理对象
     *
     * @param obj 输入对象
     * @return HashMap<String, Object>
     */
    public static HashMap<String, Object> toHashMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }
        try {
            // 如果是Hibernate代理对象，先拿到真实对象
            if (obj instanceof HibernateProxy) {
                obj = Hibernate.unproxy(obj);
            }
            Map<String, Object> map = objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {
            });
            if (map == null) {
                return new HashMap<>();
            }
            return new HashMap<>(map);
        } catch (Exception e) {
            throw new RuntimeException("对象转换为HashMap失败", e);
        }
    }

    /**
     * 将HashMap转换为格式化的JSON字符串
     *
     * @param map 输入HashMap
     * @return 格式化后的JSON字符串
     */
    public static String toPrettyJson(Map<?, ?> map) {
        if (map == null) {
            return "{}";
        }
        try {
            // 临时启用缩进输出
            ObjectMapper prettyMapper = objectMapper.copy();
            prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);
            return prettyMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("HashMap转格式化JSON失败", e);
        }
    }

    public static String toJsonString(Object o) {
        String json = "";

        try {
            json = objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;
    }
}
