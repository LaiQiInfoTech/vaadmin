package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.textfield.TextArea;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;

import java.lang.reflect.Field;

/**
 * LongTextInputField
 * 多行文本输入框，绑定 String 类型数据。
 * 遵循BaseFormFieldComponent规范。
 */
public class LongTextInputField extends BaseFormFieldComponent<String> {

    private TextArea textField; // UI控件
    private String data = "";   // 内部持有的数据

    public LongTextInputField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        this.textField = new TextArea();
        this.textField.setId(getField().getName()); // 唯一ID
        this.textField.setPlaceholder("请输入 " + getFormField().title()); // 占位符
        this.textField.setWidthFull();
        this.textField.setEnabled(getFormField().enabled());
        this.textField.setMinHeight("150px"); // 适配长文本需求

        this.textField.addValueChangeListener(event -> {
            setData(event.getValue());
        });

        add(this.textField);
    }



    @Override
    public void pushViewData() {
        if (this.textField != null) {
            if (data == null || data.isEmpty()) {
                textField.clear();
            } else if (!data.equals(textField.getValue())) {
                textField.setValue(data);
            }
        }
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setInternalData(String data) {
        this.data = data == null ? "" : data;
    }

    @Override
    public void clearData() {
        this.data = "";
    }

    @Override
    public void clearUI() {
        if (this.textField != null) {
            this.textField.clear();
        }
    }
}
