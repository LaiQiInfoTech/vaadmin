package dev.w0fv1.vaadmin.view.form;

import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 普通表单示例，不做持久化操作，填写完成后仅返回数据给上层
 *
 * @param <F> 表单数据类型，必须实现 BaseFormModel
 */
@Slf4j
public class NormalForm<F extends BaseFormModel> extends BaseForm<F> {

    /**
     * 用于处理表单取消时的回调
     */
    private final Runnable onCancel;

    /**
     * 用于处理表单保存时的回调
     */
    private final Function<F, Boolean> onSave;
    ;

    /**
     * 是否更新模式（默认根据表单里是否有ID做一个判断，如果 F 没有 id 概念，可自行决定传 true/false）
     */
    private final boolean isUpdate;

    /**
     * 构造一个普通表单
     *
     * @param formModel 初始表单数据
     * @param onSave    保存回调
     * @param onCancel  取消回调
     */
    public NormalForm(F formModel, Function<F, Boolean> onSave, Runnable onCancel) {
        // 这里简单处理一下，假如 F 有 getId(), 可根据是否为 null 来判断是不是更新模式
        // 如果没有，也可以固定填 false。
        super(formModel, false);
        this.onSave = onSave;
        this.onCancel = onCancel;
        this.isUpdate = false;

        // 若想根据 F 是否为更新模式, 可以用:
        // super(formModel, formModel != null && formModel.getId() != null);
        // this.isUpdate = (formModel != null && formModel.getId() != null);

        super.initialize();
    }

    /**
     * 点击“取消”按钮时调用
     */
    @Override
    public void onCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    /**
     * 点击“保存”按钮时调用
     *
     * @param data 校验、收集完毕的表单数据
     */
    @Override
    public Boolean save(F data) {
        log.info("NormalForm 收集到数据: {}", data.toString());
        // 将数据通过回调交给上层，让上层去处理
        if (onSave != null) {
            return onSave.apply(data);
        }
        return true;
    }
}

