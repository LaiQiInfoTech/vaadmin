package dev.w0fv1.vaadmin.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import dev.w0fv1.vaadmin.view.table.model.TableField;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple two-column table component for Vaadin Flow with borders and padding.
 */
public class InfoTable extends VerticalLayout implements HasSize, HasStyle {

    private final VerticalLayout layout;
    private final List<Row> rows = new ArrayList<>();

    private InfoTable() {
        layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getElement().getStyle().set("border", "1px solid #ccc");
        layout.getElement().getStyle().set("border-radius", "4px");
        add(layout);
    }

    public static InfoTable of(Object... keyValuePairs) {
        InfoTable table = new InfoTable();
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Argument count must be even: key/value pairs");
        }
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = String.valueOf(keyValuePairs[i]);
            Object value = keyValuePairs[i + 1];
            table.add(key, value);
        }
        return table;
    }

    public static InfoTable of(Object obj) {
        InfoTable table = new InfoTable();
        if (obj == null) {
            return table;
        }
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type.isPrimitive() ||
                    Number.class.isAssignableFrom(type) ||
                    Boolean.class.isAssignableFrom(type) ||
                    Character.class.isAssignableFrom(type) ||
                    String.class.isAssignableFrom(type)) {
                try {
                    Object value = field.get(obj);

                    // 优先级: @TableField.displayName > @DisplayName > 字段名
                    String key;
                    TableField tableField = field.getAnnotation(TableField.class);
                    if (tableField != null && !tableField.displayName().isEmpty()) {
                        key = tableField.displayName();
                    } else {
                        DisplayName displayName = field.getAnnotation(DisplayName.class);
                        if (displayName != null && !displayName.value().isEmpty()) {
                            key = displayName.value();
                        } else {
                            key = field.getName();
                        }
                    }

                    // 使用默认值（如果配置了）
                    if ((value == null || String.valueOf(value).isBlank()) && tableField != null) {
                        value = tableField.defaultValue();
                    }

                    table.add(key, value);
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
        }
        return table;
    }

    public InfoTable add(String key, Object value) {
        Row row = new Row(key, value == null ? "" : String.valueOf(value));
        rows.add(row);
        layout.add(row.asComponent());
        return this;
    }

    private static class Row {
        private final String key;
        private final String value;

        Row(String key, String value) {
            this.key = key;
            this.value = value;
        }

        Component asComponent() {
            HorizontalLayout rowLayout = new HorizontalLayout();
            rowLayout.setWidthFull();
            rowLayout.getElement().getStyle().set("border-bottom", "1px solid #eee");
            rowLayout.getElement().getStyle().set("padding", "8px 12px");
            rowLayout.setAlignItems(Alignment.START);

            Span keySpan = new Span(key);
            keySpan.setWidthFull();
            keySpan.getElement().getStyle()
                    .set("max-width", "200px")
                    .set("font-weight", "bold")
                    .set("border-right", "1px solid #eee")
                    .set("padding-right", "8px")
                    .set("white-space", "normal")
                    .set("overflow-wrap", "break-word")
                    .set("display", "block");

            Span valueSpan = new Span(value);
            valueSpan.setWidthFull();
            valueSpan.getElement().getStyle()
                    .set("max-width", "400px")
                    .set("padding-left", "8px")
                    .set("white-space", "pre-wrap")
                    .set("overflow-wrap", "break-word")
                    .set("display", "block");

            rowLayout.add(keySpan, valueSpan);
            return rowLayout;
        }
    }

    /**
     * 标注字段的显示名称，用于 InfoTable 中显示友好的 key。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface DisplayName {
        String value();
    }
}
