package dev.w0fv1.vaadmin.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import dev.w0fv1.vaadmin.GenericRepository;
import dev.w0fv1.vaadmin.view.table.model.TableField;
import dev.w0fv1.vaadmin.entity.BaseManageEntity;
import lombok.extern.slf4j.Slf4j;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

import static dev.w0fv1.vaadmin.util.TypeUtil.isBaseType;
import static dev.w0fv1.vaadmin.view.tools.Notifier.showNotification;
import static org.reflections.ReflectionUtils.getAllFields;

@Slf4j
public class EntitySelectPage<
        E extends BaseManageEntity<ID>,
        ID> extends VerticalLayout {
    /**
     * 回调：完成时返回所选 ID 列表
     * 约定：传入 null 表示用户取消或仅关闭，不提交任何变更
     */
    public interface OnFinish<ID> {
        void onFinish(List<ID> selectedData);
    }

    private final GenericRepository genericRepository;
    private final Class<E> entityClass;
    private final OnFinish<ID> onFinish;

    private final Grid<E> grid;
    private Grid.Column<E> selectColumn;

    private final List<E> data = new ArrayList<>();

    private final Boolean focusMode;
    private final boolean singleSelectMode;
    private boolean browseMode = false;

    private final TextField idSearchInput = new TextField();

    /**
     * 搜索区域（含 ID 精确搜索 + 自定义筛选）
     */
    private HorizontalLayout searchLayout;

    private final Button searchButton = new Button("搜索");
    private final Button resetButton = new Button("重置");

    private final Button finishButton = new Button("完成");
    private final Button cancelButton = new Button("取消");

    private int page = 0;
    private final int pageSize = 10;
    private final Span pageInfo = new Span();
    private final NumberField pageInput = new NumberField();

    private final GenericRepository.PredicateManager<E> predicateManager = new GenericRepository.PredicateManager<>();

    private final Set<ID> selectedItems = new HashSet<>();

    private final Set<String> permanentPredicateKeys = new HashSet<>();
    private final List<CustomFilter<E>> customFilters = new ArrayList<>();

    public EntitySelectPage(
            Class<E> entityClass,
            GenericRepository genericRepository,
            OnFinish<ID> onFinish,
            boolean singleSelectMode,
            boolean focusOnlyMode,
            boolean browseMode,
            List<CustomFilter<E>> extraFilters
    ) {
        this(entityClass, genericRepository, onFinish, singleSelectMode, focusOnlyMode, browseMode, extraFilters, null);
    }

    public EntitySelectPage(
            Class<E> entityClass,
            GenericRepository genericRepository,
            OnFinish<ID> onFinish,
            boolean singleSelectMode,
            boolean focusOnlyMode,
            List<CustomFilter<E>> extraFilters,
            GenericRepository.PredicateBuilder<E> initPredicate
    ) {
        this(entityClass, genericRepository, onFinish, singleSelectMode, focusOnlyMode, false, extraFilters, initPredicate);
    }

    public EntitySelectPage(
            Class<E> entityClass,
            GenericRepository genericRepository,
            OnFinish<ID> onFinish,
            boolean singleSelectMode,
            boolean focusMode,
            boolean browseMode,
            List<CustomFilter<E>> extraFilters,
            GenericRepository.PredicateBuilder<E> initPredicate
    ) {
        this.entityClass = entityClass;
        this.genericRepository = genericRepository;
        this.onFinish = onFinish;
        this.singleSelectMode = singleSelectMode;
        this.focusMode = focusMode;
        this.browseMode = browseMode;

        if (extraFilters != null) this.customFilters.addAll(extraFilters);
        if (initPredicate != null) predicateManager.putPredicate("init", initPredicate);

        this.grid = new Grid<>(entityClass, false);
        configureTitle();
        configureSearchFields();
        configureActionButtons();
        configureDataGrid();
        configureSelectionModel();

        configurePaginationComponent();
        add(grid, createPaginationLayout(), createActionButtonLayout());

        if (focusMode || initPredicate != null) applyFilters();

        // 确保初始 browseMode 的文案/列状态被正确设置
        setBrowseMode(this.browseMode);
    }

    private void configureTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        H1 title = new H1(getTitle());
        Button refreshButton = new Button(VaadinIcon.REFRESH.create(), event -> refresh());
        titleLayout.add(title, refreshButton);
        titleLayout.setAlignItems(Alignment.CENTER);
        add(titleLayout);
    }

    public void setBrowseMode(boolean browseMode) {
        boolean changed = this.browseMode != browseMode;
        this.browseMode = browseMode;

        finishButton.setText(browseMode ? "关闭" : "完成");

        if (browseMode) {
            // 禁用选择
            grid.deselectAll();
            selectedItems.clear();
            grid.setSelectionMode(Grid.SelectionMode.NONE);
            pushViewData();
        } else {
            // 恢复选择模型
            configureSelectionModel();
        }
    }


    private void configureSearchFields() {
        searchLayout = new HorizontalLayout();
        resetButton.addClickListener(e -> resetFilters());

        if (!focusMode) {
            idSearchInput.setLabel("ID 精确搜索");
            searchButton.addClickListener(e -> applyFilters());
            searchLayout.add(idSearchInput, searchButton);
        }
        for (CustomFilter<E> filter : customFilters) {
            Component c = filter.getComponent();
            searchLayout.add(c);
            if (focusMode) attachAutoRefresh(c);
        }
        searchLayout.add(resetButton);
        searchLayout.setWidthFull();
        searchLayout.setSpacing(true);
        searchLayout.setAlignItems(Alignment.END);
        add(searchLayout);
    }

    private void attachAutoRefresh(Component c) {
        if (c instanceof com.vaadin.flow.component.HasValue<?, ?> hv) {
            hv.addValueChangeListener(ev -> applyFilters());
        }
        c.getChildren().forEach(this::attachAutoRefresh);
    }

    private void configureActionButtons() {
        finishButton.addClickListener(event -> {
            if (browseMode) {
                // 仅关闭浏览，不提交
                onFinish.onFinish(null);
                return;
            }
            if (singleSelectMode) {
                E sel = grid.asSingleSelect().getValue();
                onFinish.onFinish(sel != null ? List.of(sel.getId()) : Collections.emptyList());
            } else {
                // 用全局 selectedItems，避免分页丢失
                onFinish.onFinish(new ArrayList<>(selectedItems));
            }
        });

        // 取消：不提交，不清空
        cancelButton.addClickListener(event -> onFinish.onFinish(null));
    }


    private HorizontalLayout createActionButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout(finishButton, cancelButton);
        layout.setJustifyContentMode(JustifyContentMode.END);
        layout.setWidthFull();
        return layout;
    }

    private void configureDataGrid() {
        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addClassName("entity-select-grid");
        grid.setColumnReorderingAllowed(true);

        List<Field> fieldList = new ArrayList<>(getAllFields(entityClass));
        fieldList.sort(Comparator.comparingInt(field -> {
            TableField annotation = field.getAnnotation(TableField.class);
            return annotation != null ? annotation.order() : 100;
        }));

        for (Field field : fieldList) {
            // 跳过 static 字段
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (!isBaseType(field.getType())) continue;

            field.setAccessible(true);
            TableField tableFieldInfo = field.getAnnotation(TableField.class);
            String displayName = (tableFieldInfo != null && !tableFieldInfo.displayName().isEmpty())
                    ? tableFieldInfo.displayName()
                    : field.getName();

            grid.addColumn(data -> getFieldStringValue(data, field))
                    .setHeader(displayName)
                    .setAutoWidth(true)
                    .setSortable(true);
        }
    }

    private void configureSelectionModel() {
        if (singleSelectMode) {
            grid.setSelectionMode(Grid.SelectionMode.SINGLE);
            grid.asSingleSelect().addValueChangeListener(e -> {
                selectedItems.clear();
                if (e.getValue() != null) {
                    selectedItems.add(e.getValue().getId());
                    if (!e.isFromClient()) return;
                    showNotification("已选择：ID = " + e.getValue().getId());
                }
            });
        } else {
            grid.setSelectionMode(Grid.SelectionMode.MULTI);
            grid.asMultiSelect().addValueChangeListener(e -> {
                if (!e.isFromClient()) return;

                Set<E> oldSelection = e.getOldValue() != null ? e.getOldValue() : Collections.emptySet();
                Set<E> newSelection = e.getValue() != null ? e.getValue() : Collections.emptySet();

                // 计算新增
                newSelection.stream()
                        .filter(v -> !oldSelection.contains(v))
                        .forEach(v -> selectedItems.add(v.getId()));

                // 计算移除
                oldSelection.stream()
                        .filter(v -> !newSelection.contains(v))
                        .forEach(v -> selectedItems.remove(v.getId()));

                showNotification("已选择：" + selectedItems);
            });
        }
    }


    private void configurePaginationComponent() { /* 保留空实现以备后续扩展 */ }

    private HorizontalLayout createPaginationLayout() {
        Button previousButton = new Button("上一页", event -> previousPage());
        Button nextButton = new Button("下一页", event -> nextPage());
        Button jumpButton = new Button("跳转", event -> {
            Integer targetPage = pageInput.getValue() != null ? pageInput.getValue().intValue() - 1 : 0;
            jumpPage(targetPage);
        });

        pageInput.setPlaceholder("页码");
        pageInput.setMin(1d);
        pageInput.setMax((double) getTotalPages());
        pageInput.setStep(1);
        pageInput.setWidth("100px");

        updatePageInfo();

        HorizontalLayout paginationLayout = new HorizontalLayout(previousButton, pageInfo, nextButton, pageInput, jumpButton);
        paginationLayout.setAlignItems(Alignment.CENTER);
        paginationLayout.setWidthFull();
        return paginationLayout;
    }

    private void updatePageInfo() {
        int totalPages = getTotalPages();
        pageInfo.setText(String.format("第 %d 页，共 %d 页", Math.min(page + 1, Math.max(totalPages, 1)), totalPages));
        pageInput.setMax((double) Math.max(totalPages, 1));
    }

    public void initialize() {
        loadData();
        pushViewData();
    }

    public void loadData() {
        List<E> fetchedData = genericRepository.execute(status -> {
            List<E> list = new ArrayList<>();
            try {
                // 按 id 升序
                List<GenericRepository.SortOrder> sortOrders = List.of(
                        new GenericRepository.SortOrder("id", GenericRepository.SortOrder.Direction.ASC)
                );

                List<E> entities = genericRepository.getPage(entityClass, page, pageSize, predicateManager, sortOrders);
                list.addAll(entities);
            } catch (Exception e) {
                log.error("数据加载失败", e);
                status.setRollbackOnly();
                throw new RuntimeException("数据加载失败", e);
            }
            return list;
        });

        if (fetchedData != null) {
            setData(fetchedData);
            pushViewData();
        }
    }


    public void setData(List<E> data) {
        this.data.clear();
        if (data != null) this.data.addAll(data);
    }

    /**
     * 外部设置需要“被勾选”的 ID 列表
     */
    public void setSelectedData(List<ID> data) {
        selectedItems.clear();
        if (data != null && !data.isEmpty()) {
            if (singleSelectMode) selectedItems.add(data.getFirst());
            else selectedItems.addAll(data);
        }
        // 仅对“当前页”做回显；其他页由 selectedItems 统一托管
        applySelectionToGrid();
    }

    private void applySelectionToGrid() {
        if (browseMode) return;
        if (singleSelectMode) {
            grid.deselectAll();
            if (!selectedItems.isEmpty()) {
                ID target = selectedItems.iterator().next();
                // 仅在当前页数据中进行回显
                this.data.stream()
                        .filter(e -> Objects.equals(e.getId(), target))
                        .findFirst()
                        .ifPresent(e -> grid.asSingleSelect().setValue(e));
            }
        } else {
            grid.deselectAll();
            if (!selectedItems.isEmpty()) {
                this.data.stream()
                        .filter(e -> selectedItems.contains(e.getId()))
                        .forEach(grid::select);
            }
        }
    }

    public void pushViewData() {
        grid.setItems(this.data);
        applySelectionToGrid();
    }

    public void refresh() {
        loadData();      /* 避免二次 jumpPage(0) */
    }

    private void nextPage() {
        if (page < getTotalPages() - 1) {
            page++;
            jumpPage(page);
        }
    }

    private void previousPage() {
        if (page > 0) {
            page--;
            jumpPage(page);
        }
    }

    private void jumpPage(int targetPage) {
        int totalPages = getTotalPages();
        if (totalPages == 0) {
            this.page = 0;
            setData(Collections.emptyList());
            pushViewData();
            updatePageInfo();
            return;
        }
        if (targetPage < 0) targetPage = 0;
        if (targetPage >= totalPages) targetPage = totalPages - 1;
        this.page = targetPage;
        loadData();
        updatePageInfo();
    }

    private void resetFilters() {
        if (!focusMode) {
            idSearchInput.clear();
            customFilters.forEach(CustomFilter::clear);
        }
        predicateManager.clearPredicatesWithOut(
                Stream.concat(Stream.of("init"), permanentPredicateKeys.stream()).toArray(String[]::new)
        );
        selectedItems.clear();
        if (focusMode) customFilters.forEach(f -> f.apply(predicateManager));
        jumpPage(0);
    }

    private void applyFilters() {
        predicateManager.clearPredicatesWithOut(
                Stream.concat(Stream.of("init"), permanentPredicateKeys.stream()).toArray(String[]::new)
        );
        if (!focusMode) {
            String idValue = idSearchInput.getValue();
            if (idValue != null && !idValue.trim().isEmpty()) {
                Object parsed = tryParseId(idValue.trim());
                if (parsed != null) {
                    predicateManager.putPredicate("idSearch",
                            (cb, root, p) -> p.add(cb.equal(root.get("id"), parsed)));
                }
            }
        }
        customFilters.forEach(f -> f.apply(predicateManager));
        jumpPage(0);
    }

    private String getTitle() {
        String suffix = singleSelectMode ? "(单选)" : "(多选)";
        return entityClass.getSimpleName() + "选择数据" + suffix;
    }

    private String getFieldStringValue(E data, Field field) {
        try {
            Object value = field.get(data);
            return value != null ? value.toString() : "N/A";
        } catch (IllegalAccessException e) {
            log.error("无法访问字段: {}", field.getName(), e);
            return "Error";
        }
    }

    private int getTotalPages() {
        long totalSize = genericRepository.getTotalSize(entityClass, predicateManager);
        return (int) Math.ceil((double) totalSize / pageSize);
    }

    /* -------------------- 外部新增永久过滤器 -------------------- */
    public void addPermanentFilter(String key, GenericRepository.PredicateBuilder<E> builder) {
        if (builder == null) return;
        predicateManager.putPredicate(key, builder);
        permanentPredicateKeys.add(key);
        log.debug("已添加永久过滤器 key={}", key);
        applyFilters();
    }

    public interface CustomFilter<E> {
        Component getComponent();

        void apply(GenericRepository.PredicateManager<E> predicateManager);

        void clear();
    }

    public void clear() {
        data.clear();
        idSearchInput.clear();
        predicateManager.clearPredicatesWithOut(Stream.concat(Stream.of("init"), permanentPredicateKeys.stream()).toArray(String[]::new));
        selectedItems.clear();
        if (singleSelectMode) grid.asSingleSelect().clear();
        else grid.deselectAll();
        pageInput.clear();
        jumpPage(0);
    }


    /*** —— 辅助：按实体字段类型解析 ID —— ***/
    private Object tryParseId(String text) {
        Class<?> idType = resolveIdFieldType();
        try {
            if (idType == Long.class || idType == long.class) return Long.parseLong(text);
            if (idType == Integer.class || idType == int.class) return Integer.parseInt(text);
            if (idType == java.util.UUID.class) return java.util.UUID.fromString(text);
            // 其他类型：直接按字符串匹配
            return text;
        } catch (Exception ex) {
            return null; // 解析失败则不添加谓词
        }
    }

    private Class<?> resolveIdFieldType() {
        Field f = findFieldRecursively(entityClass, "id");
        return f != null ? f.getType() : Object.class;
    }

    private Field findFieldRecursively(Class<?> type, String name) {
        Class<?> cls = type;
        while (cls != null && cls != Object.class) {
            try {
                return cls.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }
}