package dev.w0fv1.vaadmin.view.table;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.function.ValueProvider;
import dev.w0fv1.vaadmin.view.table.component.BaseFieldComponent;
import dev.w0fv1.vaadmin.view.table.component.TextTableFieldComponent;
import dev.w0fv1.vaadmin.view.table.model.BaseTableModel;
import dev.w0fv1.vaadmin.view.table.model.TableConfig;
import dev.w0fv1.vaadmin.view.table.model.TableField;
import dev.w0fv1.vaadmin.view.tools.UITimer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

import static dev.w0fv1.vaadmin.util.JsonUtil.toPrettyJson;
import static java.lang.reflect.Modifier.PRIVATE;
import static org.apache.commons.lang3.StringUtils.truncate;
import static org.reflections.ReflectionUtils.getAllFields;

@Slf4j
public abstract class BaseTablePage<T extends BaseTableModel> extends VerticalLayout {

    private final Class<T> tableClass;
    private final TableConfig tableConfig;

    protected final Grid<T> grid = new Grid<>();
    private final String gridId = "a" + UUID.randomUUID().toString().replaceAll("-", "");

    protected final TextField likeSearchInput = new TextField();

    protected ConfigurableFilterDataProvider<T, Void, String> provider;

    private boolean staticViewBuilt = false;
    private boolean dataInitialized = false;


    @Getter
    private HorizontalLayout titleBar;
    @Getter
    private HorizontalLayout secondaryAction;

    private HorizontalLayout filtersBar;          // 整体容器
    private HorizontalLayout filtersRight;        // 右侧扩展位
    @Getter
    private HorizontalLayout dataActions;
    @Getter
    private Component primaryActions;

    public BaseTablePage(Class<T> tableClass) {
        this.tableClass = tableClass;
        this.tableConfig = tableClass.getAnnotation(TableConfig.class);
        if (tableConfig == null) throw new IllegalStateException("@TableConfig not found");
    }

    // ================ 拆解后的生命周期方法 ================ //

    /**
     * 1. 初始化静态UI结构（不含数据）
     */
    public void initStaticView() {
        if (staticViewBuilt) return;

        buildTitleBar();
        buildSecondaryAction();
        buildFiltersBar();
        buildDataActions();
        buildGridColumns();

        add(grid);
        add(extendPage());

        staticViewBuilt = true;
    }


    /**
     * 2. 初始化数据组件，必须调用super.initData()
     */
    public void initData() {
        if (dataInitialized) return;


//        grid.setPageSize(tableConfig.pageSize());   // ← 关键

        if (tableConfig.allRowsVisible()) {
            grid.setAllRowsVisible(true);
        }

        provider = DataProvider.fromFilteringCallbacks(this::fetch, this::count)
                // 使用默认 FilterCombiner，避免 NPE
                .withConfigurableFilter();
        grid.setItems(provider);
        grid.setId(gridId);

        dataInitialized = true;
        if (tableConfig.autoScrollRight()) {
            scrollRightOnSizeChanged(grid);
        }
    }

    /**
     * 横向滚到最右列（仅执行一次）。
     * 依赖 Grid 内部 #scroller / .vaadin-grid__scroller 容器。
     */
    public static void scrollRightOnSizeChanged(Grid<?> grid) {
        grid.getElement().addAttachListener(e ->
                grid.getElement().executeJs(
                        """
                                const grid = this;
                                const scrollRight = () => {
                                  const s = grid.shadowRoot.querySelector('#scroller');
                                  const t = grid.shadowRoot.querySelector('#table');
                                
                                  if (t) {
                                      t.scrollLeft = t.scrollWidth;
                                  }
                                };
                                // 执行一次，万一 size-changed 已发生
                                requestAnimationFrame(scrollRight);
                                // 下次列宽变化再滚一次
                                grid.addEventListener('size-changed', scrollRight);
                                """
                )
        );
    }


    /**
     * 3. 将数据推送至UI展示层，需幂等
     */
    public void pushViewData() {
        refresh();
    }

    public void refresh() {
        provider.refreshAll();
    }

    public void applyFilter(String keyword) {
        provider.setFilter(keyword == null || keyword.isBlank() ? null : keyword.trim());
        refresh();
    }


    /**
     * 4. 完整的初始化逻辑（子类控制调用时机）
     */
    public void initialize() {
        initStaticView();
        initData();
        pushViewData();
    }

    // ================ 原有的数据加载方法 ================ //

    private Stream<T> fetch(Query<T, String> q) {
        return loadChunk(q.getOffset(), q.getLimit(), q.getFilter().orElse(null), q.getSortOrders()).stream();
    }

    private int count(Query<T, String> q) {
        return getTotalSize(q.getFilter().orElse(null)).intValue();
    }

    // ================ 以下代码保留原有逻辑不变 ================ //

    private void buildGridColumns() {
        List<Field> fields = new ArrayList<>(getAllFields(tableClass, ReflectionUtils.withModifier(PRIVATE)).stream().toList());
        fields.sort(Comparator.comparingDouble(f -> {
            TableField tableField = f.getAnnotation(TableField.class);

            // 设置冻结列的排序值为最小（确保它排在最前面）
            if (tableField != null && tableField.frozen()) {
                return -1;  // 冻结列排最前面
            }

            // 非冻结列按原来的 order 排序
            return Optional.ofNullable(tableField).map(TableField::order).orElse(100);
        }));
        for (Field f : fields) {
            f.setAccessible(true);
            TableField tf = f.getAnnotation(TableField.class);
            String header = tf != null && !tf.displayName().isEmpty() ? tf.displayName() : f.getName();
            String columnKey = tf != null && !tf.key().isEmpty() ? tf.key() : f.getName();
            Grid.Column<T> col;  // <—— 把列句柄留下，后面统一处理冻结

            if (tf != null && tf.sortable()) {
                col = grid.addColumn(item -> getComparableFieldValue(item, f))
                        .setHeader(header).setSortable(true).setKey(columnKey).setAutoWidth(true);
            } else {
                col = grid.addComponentColumn(item -> buildSpanCell(item, f))
                        .setHeader(header).setKey(columnKey).setAutoWidth(true);
            }
            /* ---------- 新增逻辑：根据注解决定是否冻结 ---------- */
            if (tf != null && (tf.frozen() || tf.id())) {
                col.setFrozen(true);      // → 冻结到左边
                // 如需禁止用户拖动改变顺序，可再加：col.setReorderable(false);
            }
            if (tf != null && (tf.sortable() || tf.id())) {
                col.setSortable(true);
            }

        }
        extendGridColumns();
        grid.addItemClickListener(e -> onItemClicked(e.getItem()));
        grid.addItemDoubleClickListener(e -> onItemDoubleClicked(e.getItem()));
        grid.setColumnReorderingAllowed(true);
    }

    private Component buildSpanCell(T item, Field f) {
        try {
            Object value = f.get(item);
            String displayValue;

            if (value == null) {
                displayValue = "-";
            } else if (value instanceof Collection<?> coll) {
                // 检查是否为枚举集合
                if (!coll.isEmpty() && coll.iterator().next() instanceof Enum<?>) {
                    displayValue = coll.stream()
                            .map(v -> ((Enum<?>) v).name())  // 或 getDisplayName() 如果实现接口
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("-");
                } else {
                    displayValue = coll.toString(); // 非枚举集合
                }
            } else if (value.getClass().isEnum()) {
                displayValue = ((Enum<?>) value).name();
            } else {
                displayValue = value.toString();
            }

            Span span = new Span(truncate(displayValue, 25));
            span.getStyle().set("cursor", "pointer");
            span.addClickListener(ev -> onFieldClick(item, f, value));
            return span;

        } catch (IllegalAccessException e) {
            return new Span("Error");
        }
    }

    private Comparable<?> getComparableFieldValue(T item, Field f) {
        try {
            Object v = f.get(item);
            return v instanceof Comparable<?> c ? c : (v == null ? "" : v.toString());
        } catch (IllegalAccessException e) {
            return "";
        }
    }


    private void buildTitleBar() {
        primaryActions = extendPrimaryAction();
        titleBar = new HorizontalLayout(new H1(getTitle()), new Button(VaadinIcon.REFRESH.create(), v -> refresh()), primaryActions);
        titleBar.setAlignItems(Alignment.END);
        add(titleBar);
        if (!getDescription().isEmpty()) add(new Span(getDescription()));
    }

    private void buildSecondaryAction() {
        secondaryAction = new HorizontalLayout(extendSecondaryAction());
        add(secondaryAction);
    }

    // ① 新扩展方法（默认空实现）
    public Component extendDataFilters() {
        return new Div();   // 子类可返回任意组件或布局
    }

    public void addDataFilters(Component... components) {
        if (filtersRight != null) {
            filtersRight.add(components);
        }
    }

    /**
     * 构建搜索 + 其它过滤器的总栏
     */
    private void buildFiltersBar() {

        // 根容器：撑满一行
        filtersBar = new HorizontalLayout();
        filtersBar.setWidthFull();
        filtersBar.setAlignItems(Alignment.CENTER);
        filtersBar.setJustifyContentMode(JustifyContentMode.BETWEEN); // 左右对齐

        /* ---------- 左侧：关键字搜索 ---------- */
        if (tableConfig.likeSearch()) {
            HorizontalLayout likeSearchBar = new HorizontalLayout();
            likeSearchBar.setAlignItems(Alignment.CENTER);

            Span label = new Span("关键字搜索：");

            likeSearchInput.setPlaceholder("搜索 " + getLikeSearchFieldNames());
            likeSearchInput.addValueChangeListener(
                    e -> applyFilter(e.getValue())
            );

            Button searchButton = new Button(
                    VaadinIcon.SEARCH.create(),
                    e -> applyFilter(likeSearchInput.getValue())
            );
            searchButton.getElement().setAttribute("title", "搜索");

            likeSearchBar.add(label, likeSearchInput, searchButton);

            filtersBar.add(likeSearchBar);          // ← 左侧加入
        }

        /* ---------- 右侧：扩展过滤组件 ---------- */
        filtersRight = new HorizontalLayout();
        filtersRight.add(extendDataFilters());     // 子类自定义内容
        filtersBar.add(filtersRight);

        add(filtersBar);                           // 整体挂到页面
    }


    private void buildDataActions() {
        dataActions = new HorizontalLayout();
        dataActions.setWidthFull(); // 关键：让 HorizontalLayout 占满宽度

        dataActions.add(extendDataAction());
        if (enableCreate()) dataActions.add(new Button("创建", e -> onCreateEvent()));
        dataActions.setJustifyContentMode(JustifyContentMode.END);
        add(dataActions);
    }

    private String getFieldStringValue(T item, Field f, int max) {
        try {
            Object v = f.get(item);
            switch (v) {
                case null -> {
                    return "-";
                }
                case Map<?, ?> m -> {
                    return toPrettyJson(m);
                }
                case Collection<?> coll when !coll.isEmpty() && coll.iterator().next() instanceof Enum<?> -> {
                    String combined = coll.stream().map(e -> ((Enum<?>) e).name()).reduce((a, b) -> a + ", " + b).orElse("-");
                    return truncate(combined, max);
                }
                default -> {
                }
            }

            if (v.getClass().isEnum()) return ((Enum<?>) v).name();

            String str = v.toString();
            return truncate(str, max);
        } catch (IllegalAccessException e) {
            return "Error";
        }
    }

    public List<String> getLikeSearchFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        Set<Field> fields = getAllFields(tableClass, ReflectionUtils.withModifier(PRIVATE));
        for (Field f : fields) {
            TableField annotation = f.getAnnotation(TableField.class);
            if (annotation != null && annotation.likeSearch()) {
                String key = annotation.key();
                if (key != null && !key.isBlank()) {
                    fieldNames.add(key);
                } else {
                    fieldNames.add(f.getName());
                }
            }
        }
        return fieldNames;
    }

    // ======= 抽象方法保留原接口 ======= //

    protected abstract List<T> loadChunk(int offset, int limit, String filter, List<QuerySortOrder> sortOrders);

    protected abstract Long getTotalSize(String filter);

    public abstract void onCreateEvent();

    // ======= 扩展点，保留原有方法 ======= //

    public String getTitle() {
        return tableConfig.title();
    }

    public String getDescription() {
        return Optional.ofNullable(tableConfig.description()).orElse("");
    }

    public Component extendPrimaryAction() {
        return new Div();
    }

    public Component extendSecondaryAction() {
        return new Div();
    }

    public Component extendDataAction() {
        return new Div();
    }

    public Component extendPage() {
        return new Div();
    }

    // ======= 组件添加扩展方法 ======= //

    /**
     * 向 titleBar 添加组件
     *
     * @param components 要添加的组件
     */
    public void addTitleBar(Component... components) {
        if (titleBar != null) {
            titleBar.add(components);
        }
    }

    /**
     * 向 secondaryAction 添加组件
     *
     * @param components 要添加的组件
     */
    public void addSecondaryAction(Component... components) {
        if (secondaryAction != null) {
            secondaryAction.add(components);
        }
    }

    /**
     * 向 dataActions 添加组件
     *
     * @param components 要添加的组件
     */
    public void addDataActions(Component... components) {
        if (dataActions != null) {
            dataActions.add(components);
        }
    }

    /**
     * 向 primaryActions 添加组件，仅当其是 Composite 类型容器时有效
     *
     * @param components 要添加的组件
     */
    public void addPrimaryActions(Component... components) {
        if (primaryActions instanceof HasComponents) {
            ((HasComponents) primaryActions).add(components);
        } else {
            log.warn("primaryActions 不是容器类型，不能添加组件: {}", primaryActions.getClass().getSimpleName());
        }
    }


    public void extendGridColumns() {
    }

    public Grid.Column<T> extendGridColumn(ValueProvider<T, ?> valueProvider) {
        return this.grid.addColumn(valueProvider);
    }

    public <V extends Component> Grid.Column<T> extendGridComponentColumn(ValueProvider<T, V> componentProvider) {
        return this.grid.addComponentColumn(componentProvider);
    }

    public void onItemClicked(T item) {
    }

    public void onItemDoubleClicked(T item) {
    }

    public void onFieldClick(T item, Field field, Object value) {
        // 判断是否有 @TableField 注解
        TableField tableField = field.getAnnotation(TableField.class);
        BaseFieldComponent<?> fieldComponent = null;

        // 默认仅处理 String 类型
        if (value instanceof String || tableField == null) {
            fieldComponent = new TextTableFieldComponent(field, getFieldStringValue(item, field, 200000));
        }

        if (fieldComponent == null) {
            return;
        }

        // 创建 Dialog
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidthFull();
        dialog.setMaxWidth("600px");
        dialog.setMaxHeight("80vh");

        // 处理标题
        String label;
        if (tableField != null && tableField.displayName() != null && !tableField.displayName().isEmpty()) {
            label = tableField.displayName();
        } else {
            label = field.getName();
        }
        dialog.setHeaderTitle("字段详情: " + label);

        // 创建滚动内容区域
        Div contentWrapper = new Div(fieldComponent);
        contentWrapper.getStyle()
                .set("overflow", "auto")
                .set("max-height", "60vh");

        dialog.add(contentWrapper);

        // 添加底部关闭按钮
        Button close = new Button("关闭", e -> dialog.close());
        dialog.getFooter().add(close);

        dialog.open();
    }


    public Boolean enableCreate() {
        return true;
    }
}
