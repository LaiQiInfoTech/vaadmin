package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.textfield.NumberField;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * NumberInputField
 * 用于输入数值型字段，支持多种数字类型（如 Integer、Long、Double 等）。
 */
@Slf4j
public class NumberInputField<T extends Number> extends BaseFormFieldComponent<T> {

    private NumberField numberField; // UI控件
    private Class<T> type;            // 目标数字类型
    private T data;                   // 内部持有的数据

    public NumberInputField(Field field, BaseFormModel formModel, Class<T> type) {
        super(field, formModel);
        this.type = type;
        super.initialize();

    }

    @Override
    void initStaticView() {
        this.numberField = new NumberField();
        this.numberField.setId(getField().getName());
        this.numberField.setPlaceholder("请输入 " + getFormField().title());
        this.numberField.setWidthFull();
        this.numberField.setEnabled(getFormField().enabled());

        // 设置步长
        if (type.equals(Integer.class) || type.equals(Long.class)) {
            this.numberField.setStep(1);
        } else {
            this.numberField.setStep(0.1);
        }

        this.numberField.addValueChangeListener(event -> {
            Double value = event.getValue();
            if (value == null) {
                setData(null);
            } else {
                try {
                    if (type.equals(Double.class)) {
                        setData(type.cast(value));
                    } else if (type.equals(Float.class)) {
                        setData(type.cast(value.floatValue()));
                    } else if (type.equals(Long.class)) {
                        setData(type.cast(value.longValue()));
                    } else if (type.equals(Integer.class)) {
                        setData(type.cast(value.intValue()));
                    } else if (type.equals(Byte.class)) {
                        setData(type.cast(value.byteValue()));
                    } else if (type.equals(Short.class)) {
                        setData(type.cast(value.shortValue()));
                    } else {
                        throw new UnsupportedOperationException("Unsupported number type: " + type.getName());
                    }
                } catch (ClassCastException e) {
                    log.error("Type casting failed for type: {}", type.getName(), e);
                    setData(null);
                }
            }
        });

        add(this.numberField);
    }



    @Override
    public void pushViewData() {
        if (this.numberField != null) {
            if (data == null) {
                numberField.clear();
            } else {
                Double currentUIValue = numberField.getValue();
                if (currentUIValue == null || !currentUIValue.equals(data.doubleValue())) {
                    numberField.setValue(data.doubleValue());
                }
            }
        }
    }

    @Override
    public T getData() {
        return data;
    }

    @Override
    public void setInternalData(T data) {
        this.data = data;
    }

    @Override
    public void clearData() {
        this.data = null;
    }

    @Override
    public void clearUI() {
        if (this.numberField != null) {
            this.numberField.clear();
        }
    }
}
