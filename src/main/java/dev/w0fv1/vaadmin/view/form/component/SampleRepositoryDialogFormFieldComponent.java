package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import dev.w0fv1.vaadmin.GenericRepository;
import dev.w0fv1.vaadmin.test.Echo;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;

import java.lang.reflect.Field;
import java.util.function.Supplier;

public class SampleRepositoryDialogFormFieldComponent extends BaseDialogFormFieldComponent<Long> {


    private Long id;
    private TextField textField;
    private GenericRepository genericRepository;

    public SampleRepositoryDialogFormFieldComponent(Field field, BaseFormModel formModel, GenericRepository genericRepository) {
        super(field, formModel);
        this.genericRepository = genericRepository;
        super.initialize();

    }

    /**
     * 在主界面上添加一个只读文本框用于显示数据，然后调用父类 init View() 初始化对话框及按钮
     */
    @Override
    public void pushViewData() {
        // 添加主界面显示组件（只读文本框）
        textField = new TextField();
        textField.setId(getField().getName());
        textField.setPlaceholder("点击按钮选择内容");
        textField.setReadOnly(true);
        add(textField);
        // 初始化对话框及打开按钮
        super.pushViewData();

        if (id != null && id > 0) {
            var that = this;
            genericRepository.execute(new Runnable() {
                @Override
                public void run() {
                    if (!genericRepository.exist(id, Echo.class)) {
                        return;
                    }
                    Echo echo = genericRepository.find(id, Echo.class);
                    that.id = echo.getId();
                    textField.setValue(that.id + ": " + echo.getMessage());
                }
            });

        }
    }

    /**
     * 定制对话框内部内容：包含一个输入框和确认按钮
     */
    @Override
    protected VerticalLayout createStaticDialogContent() {
        VerticalLayout dialogLayout = new VerticalLayout();
        // 对话框内的输入框
        TextField dialogTextField = new TextField("请输入内容");
        dialogLayout.add(dialogTextField);
        // 确认按钮：更新主界面文本框、保存数据并关闭对话框
        Button confirmButton = new Button("确认", event -> {
            String value = dialogTextField.getValue();

            Echo echo = new Echo(value);

            Echo save = genericRepository.execute(new Supplier<Echo>() {
                @Override
                public Echo get() {
                    return genericRepository.save(echo);
                }
            });

            if (save != null) {
                this.id = save.getId();

                textField.setValue(this.id + ": " + echo.getMessage());
                setData(this.id);
            }


            dialog.close();
        });
        dialogLayout.add(confirmButton);
        return dialogLayout;
    }

    @Override
    public Long getData() {
        return id;
    }

    @Override
    public void setInternalData(Long id) {
        this.id = id;
    }

    @Override
    public void clearData() {
        this.id = null;
    }

    @Override
    public void clearUI() {
        textField.clear();
    }

    public static class SampleRepositoryDialogFormFieldComponentBuilder implements CustomRepositoryFormFieldComponentBuilder {

        @Override
        public BaseFormFieldComponent<?> build(Field field, BaseFormModel formModel, GenericRepository genericRepository) {
            return new SampleRepositoryDialogFormFieldComponent(field, formModel, genericRepository);
        }
    }
}
