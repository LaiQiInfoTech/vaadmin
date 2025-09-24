package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;

import java.lang.reflect.Field;

/**
 * SampleDialogFormFieldComponent
 * 示例弹窗表单字段，绑定String数据。
 */
public class SampleDialogFormFieldComponent extends BaseDialogFormFieldComponent<String> {

    private TextField textField;   // 主界面只读文本框
    private TextField dialogTextField; // 弹窗内可编辑文本框
    private String data = "";      // 内部持有的数据

    public SampleDialogFormFieldComponent(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        textField = new TextField();
        textField.setId(getField().getName());
        textField.setPlaceholder("点击按钮选择内容");
        textField.setWidthFull();
        textField.setReadOnly(true);

        add(textField);

        super.initStaticView(); // 初始化Dialog和打开按钮
    }



    @Override
    public void pushViewData() {
        super.pushViewData();
        if (textField != null) {
            String currentUIValue = textField.getValue();
            if (!data.equals(currentUIValue)) {
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
        this.data = (data == null) ? "" : data;
    }

    @Override
    public void clearData() {
        this.data = "";
    }

    @Override
    public void clearUI() {
        if (textField != null) {
            textField.clear();
        }
    }

    @Override
    protected VerticalLayout createStaticDialogContent() {
        VerticalLayout dialogLayout = new VerticalLayout();

        dialogTextField = new TextField("请输入内容");
        dialogTextField.setWidthFull();
        dialogLayout.add(dialogTextField);

        Button confirmButton = new Button("确认", event -> {
            String value = dialogTextField.getValue();
            setData(value);
            pushViewData(); // 更新界面显示
            dialog.close();
        });
        confirmButton.setWidthFull();

        dialogLayout.add(confirmButton);
        return dialogLayout;
    }

    /**
     * Builder 类，标准化建造方式。
     */
    public static class SampleDialogFormFieldComponentBuilder implements CustomFormFieldComponentBuilder {

        @Override
        public BaseFormFieldComponent<?> build(Field field, BaseFormModel formModel) {
            return new SampleDialogFormFieldComponent(field, formModel);
        }
    }
}
