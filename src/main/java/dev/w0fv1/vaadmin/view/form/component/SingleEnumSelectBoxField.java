package dev.w0fv1.vaadmin.view.form.component;

import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import com.vaadin.flow.component.combobox.ComboBox;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * SingleEnumSelectBoxField
 * 用于选择单个枚举值的下拉框，绑定 {@code Enum<?>} 数据。
 */
public class SingleEnumSelectBoxField extends BaseFormFieldComponent<Enum<?>> {

    private ComboBox<Enum<?>> comboBox; // UI控件
    private Enum<?> data;                // 内部持有数据

    public SingleEnumSelectBoxField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        comboBox = new ComboBox<>();

        Class<?> enumType = getField().getType();
        comboBox.setItems((Enum<?>[]) enumType.getEnumConstants());

        // 关键：告诉 ComboBox 该怎么把枚举转换成文字
        comboBox.setItemLabelGenerator(this::buildLabel);

        comboBox.setPlaceholder("请选择 " + getFormField().title());
        comboBox.setId(getField().getName());
        comboBox.setWidthFull();
        comboBox.setEnabled(getFormField().enabled());

        comboBox.addValueChangeListener(e -> setData(e.getValue()));
        add(comboBox);
    }

    /** 把枚举常量转换成 “NAME - 字段1, 字段2 ...” */
    private String buildLabel(Enum<?> e) {
        if (e == null) return "";

        StringBuilder sb = new StringBuilder(e.name());           // ① 先放常量名

        for (Field f : e.getClass().getDeclaredFields()) {        // ② 反射拿所有非 static 字段
            if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            try {
                Object v = f.get(e);
                if (v != null) {                                  // ③ 追加字段值
                    sb.append(" - ").append(v);
                }
            } catch (IllegalAccessException ignored) {}
        }
        return sb.toString();
    }

    @Override
    public void pushViewData() {
        if (comboBox != null) {
            if (data == null) {
                comboBox.clear();
            } else if (!data.equals(comboBox.getValue())) {
                comboBox.setValue(data);
            }
        }
    }

    @Override
    public Enum<?> getData() {
        return data;
    }

    @Override
    public void setInternalData(Enum<?> data) {
        this.data = data;
    }

    @Override
    public void clearData() {
        this.data = null;
    }

    @Override
    public void clearUI() {
        if (comboBox != null) {
            comboBox.clear();
        }
    }
}
