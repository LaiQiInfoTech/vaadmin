package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import dev.w0fv1.vaadmin.util.JsonUtil;
import dev.w0fv1.vaadmin.util.TypeUtil;
import dev.w0fv1.vaadmin.view.ErrorMessage;
import dev.w0fv1.vaadmin.view.form.model.ConditionalDisplay;
import dev.w0fv1.vaadmin.view.form.model.FormField;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import dev.w0fv1.vaadmin.view.form.model.FormVisibilityCondition;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static dev.w0fv1.vaadmin.component.FieldValidator.validField;
import static dev.w0fv1.vaadmin.util.TypeUtil.defaultIfNull;
import static dev.w0fv1.vaadmin.util.TypeUtil.isEmpty;

/**
 * BaseFormFieldComponent
 * 所有表单字段组件的抽象父类，封装了字段绑定、数据管理、校验等通用逻辑。
 * 设计要求：
 * 1. 组件必须持有自己的数据，不依赖UI组件暂存；
 * 2. initStaticView()：初始化UI控件，只涉及静态结构，不处理数据；
 * 3. initData()：初始化组件数据，可以在子类中重写，但必须调用super.initData()；
 * 4. pushViewData()：根据当前数据刷新UI，要求幂等（相同数据多次调用不会导致UI异常）；
 * 5. getData()/setData()：只处理数据，不操作任何UI控件；getData()应该返回数据的副本；
 * 6. 支持自动初始化数据，支持清空和校验。
 *
 * @param <Type> 当前字段绑定的数据类型（如 String, List<String>）
 */
@Slf4j
@Getter
public abstract class BaseFormFieldComponent<Type> extends VerticalLayout {
    private final Field field; // 反射字段
    @Setter
    private BaseFormModel formModel; // 表单数据模型
    private final FormField formField; // 字段注解
    private ErrorMessage errorMessage = new ErrorMessage(); // 错误提示信息
    private final Boolean autoInitialize; // 是否自动初始化数据
    private List<FormVisibilityCondition> formVisibilityConditions = new ArrayList<>();

    public BaseFormFieldComponent(Field field, BaseFormModel formModel) {
        this(field, formModel, true);
    }

    public void applyFormVisibilityCondition(Field changeField, BaseFormModel baseFormModel) {
        if (formVisibilityConditions.isEmpty()) return;

        boolean shouldEvaluate = formVisibilityConditions.stream().anyMatch(cond ->
                changeField == null ||
                        cond.getDependentFieldNames().isEmpty() ||
                        cond.getDependentFieldNames().contains(changeField.getName())
        );
        if (!shouldEvaluate) return;

        boolean visible = formVisibilityConditions.stream().allMatch(cond -> cond.evaluate(baseFormModel));
        boolean wasVisible = isVisible();
        setVisible(visible);

        // 变为可见时，将“模型值”推到UI，避免脱节
        if (!wasVisible && visible) {
            pushViewData();
        }

        // 不再在隐藏时：setData(...); pushViewData(); invokeModelFileData();
    }



    public BaseFormFieldComponent(Field field, BaseFormModel formModel, Boolean autoInitialize) {
        this.field = field;
        this.formModel = formModel;
        this.formField = field.getAnnotation(FormField.class);
        this.autoInitialize = autoInitialize;
        this.setPadding(false);

        if (field.isAnnotationPresent(ConditionalDisplay.class)) {
            try {
                Class<? extends FormVisibilityCondition>[] conditionClasses =
                        field.getAnnotation(ConditionalDisplay.class).value();
                for (Class<? extends FormVisibilityCondition> clazz : conditionClasses) {
                    formVisibilityConditions.add(clazz.getDeclaredConstructor().newInstance());
                }
            } catch (InstantiationException | IllegalAccessException |
                     InvocationTargetException | NoSuchMethodException e) {
                log.error("无法创建表单可见性条件实例", e);
            }
        } else {
            // 默认一个永远返回true的条件
            formVisibilityConditions.add(new FormVisibilityCondition.DefaultFormVisibilityCondition());
        }

        buildTitle();
    }
    public void initialize() {
        logDebug("开始初始化组件");
        initStaticView();
        add(errorMessage);
        initData();
        logDebug("数据初始化后，当前值：{}", getData());
        pushViewData();
        logDebug("UI推送数据完成，当前显示值：{}", getData());
    }

    /**
     * 初始化静态UI控件，子类必须实现。
     * 只负责结构搭建，不处理数据。
     */
    abstract void initStaticView();

    /**
     * 构建表单标题、描述信息。
     */
    public void buildTitle() {
        String title = formField.title().isEmpty() ? field.getName() : formField.title();
        if (!formField.enabled()) {
            title += "(不可编辑)";
        }
        add(new H3(title));
        if (formField.description() != null && !formField.description().isEmpty()) {
            add(new Span(formField.description()));
        }
        logDebug("标题和描述信息构建完成: {}", title);
    }

    /**
     * 初始化数据。
     * 如果 autoInitialize 为 true，则根据表单模型或默认值初始化。
     * 子类可以重写，但必须调用super.initData()。
     */
    protected void initData() {
        if (this.autoInitialize) {
            logDebug("初始化数据前，当前值为空，准备设置默认值");
            setData(getFieldDefaultValue());
            logDebug("设置默认值后，当前值：{}", getData());
        }
    }

    /**
     * 将当前数据刷新到UI控件上。
     * 要求幂等，多次调用同样数据不会导致异常。
     */
    abstract public void pushViewData();

    /**
     * 获取当前组件持有的数据。
     * 不涉及UI控件。
     */
    public abstract Type getData();

    /**
     * 设置当前组件持有的数据（模板方法）。
     * 此方法被设为 final，以确保所有子类的设值操作都会触发监听器。
     * 子类不应重写此方法，而应实现 setInternalData() 方法。
     *
     * @param data 要设置的数据
     */
    public final void setData(Type data) {
        String oldDataJson = JsonUtil.toJsonString(getData()); // Optional: for logging
        setInternalData(data); // 1. 调用子类实现的具体设值逻辑
        String newDataJson = JsonUtil.toJsonString(getData()); // Optional: for logging
        logDebug("setData: 数据由 [{}] 更新为 [{}]", oldDataJson, newDataJson);
        invokeModelFileData();
        notifyListener();      // 2. 自动通知监听器
    }

    /**
     * 抽象方法，由子类实现，用于具体设置内部持有的数据。
     * 不涉及UI控件或通知逻辑。
     *
     * @param data 要设置的数据
     */
    protected abstract void setInternalData(Type data);

    /**
     * 获取字段默认值。
     * 优先取模型已有值，如果没有则用注解配置的defaultValue，
     * 最后如果还没有，返回一个类型安全的默认值（如null、0等）。
     */
    @SuppressWarnings("unchecked")
    public Type getFieldDefaultValue() {
        Type data = getModelFieldData();

        if (data != null) {
            logDebug("从表单模型读取到已有数据：{}", data);
            return data;
        }

        FormField formField = getFormField();
        if (!formField.defaultValue().isEmpty()) {
            logDebug("从FormField注解读取到默认值：{}", formField.defaultValue());
            return (Type) TypeUtil.convert(formField.defaultValue(), field.getType(), formField.subType());
        }

        logDebug("无模型值、无注解默认值，使用类型安全默认值");
        return (Type) defaultIfNull(null, field.getType());
    }

    /**
     * 从表单模型中反射获取当前字段的值。
     */
    @SuppressWarnings("unchecked")
    public Type getModelFieldData() {
        getField().setAccessible(true);
        try {
            return (Type) getField().get(this.getFormModel());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将当前组件数据推回表单模型。
     */
    public void invokeModelFileData() {
        getField().setAccessible(true);
        try {
            logDebug("将当前数据推回模型：{}", getData());
            getField().set(this.getFormModel(), getData());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 清空数据并刷新UI。
     * 如果autoInitialize为true，会重新初始化数据。
     */
    public void clear() {
        logDebug("开始清空组件数据和UI");
        clearData();
        clearUI();
        clearValid();
        if (autoInitialize) {
            logDebug("启用autoInitialize，重新初始化数据");
            initData();
            pushViewData();
        }
    }

    /**
     * 清空数据，不操作UI。
     */
    abstract public void clearData();

    /**
     * 清空UI控件显示，不操作数据。
     */
    public abstract void clearUI();

    public void clearValid() {
        if (errorMessage != null) {
            logDebug("清除错误提示信息");
            errorMessage.setText("");
            errorMessage.setVisible(false);
        }
    }

    /**
     * 校验当前字段数据。
     * 如果校验失败，显示错误信息。
     * 成功时清除错误信息。
     */
    public Boolean valid() {
        logDebug("开始执行字段校验");
        String validMessage = "";

        if (field.isAnnotationPresent(FormField.class)) {
            FormField formField = field.getAnnotation(FormField.class);
            if (formField != null && !formField.nullable() && isEmpty(getData())) {
                validMessage = "值为空，该字段不允许为空";
                logDebug("字段不允许为空校验失败");
            }
        }

        if (validMessage.isEmpty()) {
            validMessage = validField(field, formModel, getData());
        }

        if (validMessage != null && !validMessage.isEmpty()) {
            log.warn("字段 [{}] 校验失败: {}，当前为：{}", field.getName(), validMessage, getData());
            if (errorMessage != null) {
                errorMessage.setText(validMessage);
                errorMessage.setVisible(true);
            }
            return false;
        } else {
            logDebug("字段校验通过，数据为：{}", getData());
            if (errorMessage != null) {
                errorMessage.setText("");
                errorMessage.setVisible(false);
            }
            return true;
        }
    }

    /**
     * 统一的 debug 日志方法，自动附加字段名。
     *
     * @param message 日志信息模板
     * @param args    参数列表
     */
    protected void logDebug(String message, Object... args) {
        if (log.isDebugEnabled()) {
            // 创建新的数组，fieldName + 原来的 args
            Object[] newArgs = new Object[args.length + 1];
            newArgs[0] = field.getName();
            System.arraycopy(args, 0, newArgs, 1, args.length);

            log.debug("[{}] " + message, newArgs);
        }
    }

    public interface FieldValueChangeListener<Type> {

        void valueChanged(Field field, BaseFormModel formModel, Type data);
    }


    public interface FormDataChangeListener {

        void valueChanged(Field field, BaseFormModel formModel);
    }

    public List<FieldValueChangeListener<Type>> fieldValueChangeListeners = new ArrayList<>();

    public List<FormDataChangeListener> formDataChangeListeners = new ArrayList<>();

    /**
     * 为组件内部的 Vaadin HasValue 控件添加值变更监听器。
     *
     * @param listener 值变更监听器
     */
    public void addFieldValueChangeListener(FieldValueChangeListener<Type> listener) {
        fieldValueChangeListeners.add(listener);
    }

    public void addFormDataChangeListener(FormDataChangeListener listener) {
        formDataChangeListeners.add(listener);
    }

    void notifyListener() {

        fieldValueChangeListeners.forEach(listener -> listener.valueChanged(field, formModel, getData()));
        formDataChangeListeners.forEach(listener -> listener.valueChanged(field, formModel));
    }

}
