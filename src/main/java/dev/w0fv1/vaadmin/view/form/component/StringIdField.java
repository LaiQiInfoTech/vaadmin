package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.textfield.TextField;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * StringIdField
 * 只读的字符串ID字段，绑定 String 数据。
 */
@Slf4j
public class StringIdField extends BaseFormFieldComponent<String> {

    private TextField textField; // UI控件
    private String data = "";    // 内部持有数据

    public StringIdField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        this.textField = new TextField();
        this.textField.setId(getField().getName()); // 唯一ID
        this.textField.setPlaceholder("请输入 " + getFormField().title()); // 占位符
        this.textField.setWidthFull();
        this.textField.setReadOnly(true); // ID字段默认只读
        add(this.textField);
    }



    @Override
    public void pushViewData() {
        if (data != null && !data.equals(textField.getValue())) {
            textField.setValue(data);
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
