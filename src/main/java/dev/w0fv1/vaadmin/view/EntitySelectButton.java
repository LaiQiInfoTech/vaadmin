package dev.w0fv1.vaadmin.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import dev.w0fv1.vaadmin.GenericRepository;
import dev.w0fv1.vaadmin.entity.BaseManageEntity;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dev.w0fv1.vaadmin.view.EntitySelectPage.CustomFilter;   // 新增

/**
 * EntitySelectButton
 * 实体选择按钮，支持单选/多选实体，并监听选中变化。
 */
@Slf4j
public class EntitySelectButton<
        E extends BaseManageEntity<ID>,
        ID> extends Button {

    private Dialog dialog;
    private String title;
    private EntitySelectPage<E, ID> selectPage;
    private List<ID> selectedItems = new ArrayList<>();

    private Class<E> entityType;
    private Boolean singleSelectMode;

    @Setter
    private Consumer<List<ID>> onValueChangeListener; // 监听器

    /**
     * 浏览模式（true=只浏览，禁止选择，返回空列表）
     */
    private boolean browseMode = false;


    public EntitySelectButton(
            Boolean enabled,
            String title,
            Class<E> entityClass,
            GenericRepository genericRepository,
            Boolean singleSelectMode,
            Boolean focusOnlyMode,
            Boolean browseMode,

            GenericRepository.PredicateBuilder<E> initPredicate,
            List<CustomFilter<E>> extraFilters

    ) {
        super(title);
        this.title = title;
        this.entityType = entityClass;
        this.singleSelectMode = singleSelectMode;
        this.setEnabled(enabled);
        this.browseMode = browseMode != null && browseMode;
        if (genericRepository != null) {
            setGenericRepository(genericRepository, initPredicate, extraFilters, focusOnlyMode);
        }
    }

    /**
     * ② 阅读模式构造：快速创建“只浏览”按钮（无选择）
     */
    public EntitySelectButton(
            String title,
            Class<E> entityClass,
            GenericRepository genericRepository,
            Boolean focusOnlyMode,
            GenericRepository.PredicateBuilder<E> initPredicate,
            List<CustomFilter<E>> extraFilters
    ) {
        this(true, title, entityClass, genericRepository, true, focusOnlyMode, false, initPredicate, extraFilters);
    }

    public EntitySelectButton(
            String title,
            Class<E> entityClass,
            GenericRepository genericRepository,
            Boolean singleSelectMode,
            Boolean focusOnlyMode,
            GenericRepository.PredicateBuilder<E> initPredicate,
            List<CustomFilter<E>> extraFilters
    ) {
        this(true, title, entityClass, genericRepository, singleSelectMode, focusOnlyMode,false , initPredicate, extraFilters);
    }


    public EntitySelectButton(
            String title,
            Class<E> entityClass,
            Boolean singleSelectMode
    ) {
        this(true, title, entityClass, null, singleSelectMode, false, null, null, null);
    }

    public EntitySelectButton(
            String title,
            Class<E> entityClass
    ) {
        this(true, title, entityClass, null, true, false, null, null, null);
    }

    public EntitySelectButton(
            String title,
            Class<E> entityClass,
            GenericRepository genericRepository,

            Boolean singleSelectMode
    ) {
        this(true, title, entityClass, genericRepository, singleSelectMode, false, null, null, null);
    }
    /* -------------------- 配置仓库/页面 -------------------- */

    public void setGenericRepository(
            GenericRepository genericRepository,
            GenericRepository.PredicateBuilder<E> predicateBuilder,
            List<CustomFilter<E>> extraFilters,
            Boolean focusOnlyMode
    ) {
        this.dialog = new Dialog();

        EntitySelectPage.OnFinish<ID> onFinishCallback = selectedData -> {
            if (selectedData == null){
                dialog.close();
                return;
            }
            if (!selectedData.isEmpty()) {
                selectedItems.clear();
                selectedItems.addAll(new ArrayList<>(selectedData));
                setText("ID为" + selectedItems + "的" + selectedData.size() + "条数据(点击重选)");
            } else {
                selectedItems.clear();
                setText(title);
            }

            // 通知监听器
            if (onValueChangeListener != null) {
                onValueChangeListener.accept(new ArrayList<>(selectedItems));
            }

            dialog.close();
        };

        // 使用带 browseMode 的构造函数
        selectPage = new EntitySelectPage<>(
                this.entityType,
                genericRepository,
                onFinishCallback,
                this.singleSelectMode != null && this.singleSelectMode,

                focusOnlyMode != null && focusOnlyMode,
                this.browseMode,
                extraFilters
        );

        dialog.add(selectPage);

        this.addClickListener(event -> {
            if (!isEnabled()) return;
            // 浏览模式下 setSelectedData 会被页面忽略；保留调用以保持一致性
            selectPage.setSelectedData(selectedItems);
            dialog.open();
        });

        selectPage.addPermanentFilter("preset", predicateBuilder);
        selectPage.initialize();
    }

    /* -------------------- 公开方法 -------------------- */

    /**
     * 运行时切换浏览模式（同步到已创建的页面）
     */
    public void setBrowseMode(boolean browseMode) {
        this.browseMode = browseMode;
        if (selectPage != null) {
            selectPage.setBrowseMode(browseMode);
        }
    }

    public boolean isBrowseMode() {
        return browseMode;
    }

    public void clear() {
        selectedItems.clear();
        setText(title);
        if (selectPage != null) {
            selectPage.clear();
        }
        if (onValueChangeListener != null) {
            onValueChangeListener.accept(new ArrayList<>(selectedItems));
        }
    }

    public List<ID> getValue() {
        return selectedItems;
    }

    public void setValue(List<ID> selectedItems) {
        if (selectedItems == null || selectedItems.isEmpty()) {
            return;
        }

        ID first = selectedItems.getFirst(); // 如果你用的是 Java 8 之前，请改为 selectedItems.get(0)

        if ((first instanceof Number && ((Number) first).longValue() == 0L)) {
            return;
        }

        if ((first instanceof String && ((String) first).isEmpty())) {
            return;
        }

        this.selectedItems.clear();
        this.selectedItems.addAll(selectedItems);

        if (selectPage != null) {
            selectPage.setSelectedData(selectedItems);
        }

        String text = "ID为" + selectedItems + "的" + selectedItems.size() + "条数据(点击重选)";
        setText(text);

        // 主动通知监听器
        if (onValueChangeListener != null) {
            onValueChangeListener.accept(new ArrayList<>(this.selectedItems));
        }
    }
}
