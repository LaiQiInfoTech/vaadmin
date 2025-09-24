package dev.w0fv1.vaadmin.view.table;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import dev.w0fv1.vaadmin.GenericRepository;
import dev.w0fv1.vaadmin.GenericRepository.SortOrder;
import dev.w0fv1.vaadmin.entity.BaseManageEntity;
import dev.w0fv1.vaadmin.view.BasePage;
import dev.w0fv1.vaadmin.view.InfoTable;
import dev.w0fv1.vaadmin.view.form.RepositoryForm;
import dev.w0fv1.vaadmin.view.form.model.BaseEntityFormModel;
import dev.w0fv1.vaadmin.view.table.model.BaseEntityTableModel;
import dev.w0fv1.vaadmin.view.table.model.TableField;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.transaction.support.TransactionCallback;
import com.vaadin.flow.data.provider.QuerySortOrder;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseRepositoryTablePage<
        T extends BaseEntityTableModel<E, ID>,
        F extends BaseEntityFormModel<E, ID>,
        E extends BaseManageEntity<ID>,
        ID> extends BaseTablePage<T> implements BasePage {

    @Getter
    protected GenericRepository genericRepository;

    @Getter
    protected final GenericRepository.PredicateManager<E> predicateManager = new GenericRepository.PredicateManager<>();

    private final Map<String, GenericRepository.PredicateBuilder<E>> extendPredicateBuilders = new HashMap<>();

    private final Class<E> entityClass;
    private final Class<F> formClass;
    private final Class<T> tableClass;

    private Dialog createDialog;
    private RepositoryForm<F, E, ID> createFormInstance;
    @Getter
    private F defaultFormModel;


    List<SortOrder> sortOrders = new ArrayList<>();

    public void setSortOrders(List<SortOrder> sortOrders) {
        this.sortOrders = sortOrders;
        refresh();
    }

    public BaseRepositoryTablePage(GenericRepository genericRepository, Class<T> tableClass, Class<F> formClass, Class<E> entityClass) {
        this(genericRepository, tableClass, formClass, null, entityClass);
    }

    public BaseRepositoryTablePage(GenericRepository genericRepository, Class<T> tableClass, F formModel, Class<E> entityClass) {
        this(genericRepository, tableClass, (Class<F>) formModel.getClass(), formModel, entityClass);
    }

    public BaseRepositoryTablePage(GenericRepository genericRepository, Class<T> tableClass, Class<F> formClass, F formModel, Class<E> entityClass) {
        super(tableClass);
        this.genericRepository = genericRepository;
        this.entityClass = entityClass;
        this.formClass = formClass;
        this.tableClass = tableClass;

        if (formModel == null) {
            try {
                formModel = formClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("无法实例化表单模型", e);
            }
        }
        this.defaultFormModel = formModel;
    }


    @Override
    public void initialize() {
        presetPredicate();
        predicateManager.addAllPredicates(extendPredicateBuilders);
        super.initialize(); // 构建 UI
        buildRepositoryActionColumn();
        buildCreateDialog();
        onBuild();
    }

    public void onBuild() {
        // 子类可实现页面构建后逻辑
    }

    public void onSave(ID id) {
        // 子类可在保存后处理
    }

    private void buildCreateDialog() {
        Dialog dialog = new Dialog();
        try {
            createFormInstance = new RepositoryForm<>(
                    defaultFormModel,
                    this::beforeSave,
                    id -> handleSave(id, dialog),
                    () -> handleCancel(dialog),
                    genericRepository
            );
            createFormInstance.initialize();
            dialog.add(new VerticalLayout(createFormInstance));
        } catch (Exception e) {
            throw new RuntimeException("无法创建 RepositoryForm 实例", e);
        }
        createDialog = dialog;
        add(createDialog);

    }

    private void handleSave(ID id, Dialog dialog) {
        onSave(id);
        dialog.close();
        refresh(); // 触发刷新
    }

    private void handleCancel(Dialog dialog) {
        dialog.close();
        refresh();
    }

    public void buildRepositoryActionColumn() {
        if (enableUpdate()) {
            super.extendGridComponentColumn(this::createShowDetailButton)
                    .setHeader("详情")
                    .setAutoWidth(true);

            super.extendGridComponentColumn(this::createUpdateButton)
                    .setHeader("更新")
                    .setAutoWidth(true);
        }
    }

    private Component createShowDetailButton(T t) {
        Button button = new Button("详情");
        button.addClickListener(event -> {
            Dialog updateDialog = new Dialog();

            InfoTable infoTable = InfoTable.of(t);
            updateDialog.add(infoTable);

            add(updateDialog);
            updateDialog.open();
        });
        return button;
    }

    private Component createUpdateButton(T t) {
        Button button = new Button("更新");
        button.addClickListener(event -> {
            Dialog updateDialog = new Dialog();
            RepositoryForm<F, E, ID> form = new RepositoryForm<>(
                    (F) t.toFormModel(defaultFormModel),
                    this::beforeSave,
                    id -> handleSave(id, updateDialog),
                    () -> handleCancel(updateDialog),
                    genericRepository
            );
            form.initialize();
            updateDialog.add(new VerticalLayout(form));
            add(updateDialog);
            updateDialog.open();
        });
        return button;
    }

    @Override
    public void onCreateEvent() {
        createFormInstance.setDefaultModel(defaultFormModel); // 确保是最新默认
        createDialog.open();
    }

    /**
     * 重构后的核心数据加载方法：分页 + 排序 + 模糊搜索。
     */
    @Override
    protected List<T> loadChunk(int offset, int limit, String filter, List<QuerySortOrder> querySortOrders) {
        return genericRepository.execute((TransactionCallback<List<T>>) status -> {
            try {
                buildLikeSearchPredicate(filter);
                predicateManager.addAllPredicates(extendPredicateBuilders);
                List<SortOrder> sortOrders = querySortOrders.isEmpty()
                        ? this.sortOrders.isEmpty() ? getDefaultSortOrders() : this.sortOrders                          // ← 用默认
                        : querySortOrders.stream().map(SortOrder::new).toList();

                int page = offset / limit;

                List<E> entities = genericRepository.getPage(entityClass, page, limit, predicateManager, sortOrders);
                List<T> result = entities.stream().map(this::convertToDto).collect(Collectors.toList());
                log.debug("加载 page={} limit={} filter={} 条数：{}", page, limit, filter, result.size());
                return result;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException("查询失败", e);
            }
        });
    }

    @Override
    protected Long getTotalSize(String filter) {
        return genericRepository.execute((TransactionCallback<Long>) status -> {
            try {
                buildLikeSearchPredicate(filter);
                predicateManager.addAllPredicates(extendPredicateBuilders);
                return genericRepository.getTotalSize(entityClass, predicateManager);
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException("统计失败", e);
            }
        });
    }

    /**
     * 根据 filter 构建模糊搜索谓词
     */
    /**
     * 根据 filter 构建模糊搜索谓词
     *
     * <p>支持：</p>
     * <ul>
     *   <li>varchar / text 直接 ILIKE</li>
     *   <li>numeric / enum / date 统一 cast(... as varchar) ILIKE</li>
     *   <li>json / jsonb 字段使用 column::text ILIKE</li>
     * </ul>
     *
     * <p>可通过 {@link dev.w0fv1.vaadmin.view.table.model.TableField#sqlType()}
     * 显式指定 SQL 类型；若未指定，则自动根据 Java 类型推断，
     * 且 <b>List 类型默认视为 JSONB</b>。</p>
     */
    // Add this helper method to your BaseRepositoryTablePage class
    private TableField findTableFieldAnnotation(String fieldKey) {
        return Arrays.stream(tableClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(TableField.class))
                .filter(f -> {
                    TableField tf = f.getAnnotation(TableField.class);
                    // Use the 'key' if specified, otherwise the field name.
                    String key = tf.key().isEmpty() ? f.getName() : tf.key();
                    return key.equals(fieldKey);
                })
                .map(f -> f.getAnnotation(TableField.class))
                .findFirst()
                .orElse(null);
    }
    /**
     * Constructs a fuzzy search predicate based on the provided filter text.
     *
     * <p>This method implements a universal solution for PostgreSQL by ensuring that
     * any non-string column (e.g., bigint, numeric, jsonb) is explicitly cast
     * to 'text' using the native text() function before applying the 'LOWER'
     * and 'LIKE' operations. This prevents "function lower(type) does not exist" errors.</p>
     */
    private void buildLikeSearchPredicate(String filter) {
        // 1. Remove any previous search predicate to avoid conflicts.
        predicateManager.removePredicate("likeSearch");

        // 2. If the filter is empty, there's nothing to do.
        if (filter == null || filter.isBlank()) {
            return;
        }

        // 3. Prepare the lowercase search pattern for case-insensitive matching.
        final String lowerPattern = "%" + filter.toLowerCase() + "%";

        predicateManager.putPredicate("likeSearch", (cb, root, predicates) -> {
            List<Predicate> likes = new ArrayList<>();

            // 4. Iterate over all field names designated for fuzzy search.
            for (String fieldName : super.getLikeSearchFieldNames()) {
                try {
                    Path<?> path = root.get(fieldName);
                    Class<?> javaType = path.getJavaType();

                    Expression<String> expressionForLike;

                    // 5. Determine if an explicit cast to text is needed.
                    if (String.class.isAssignableFrom(javaType)) {
                        // The column is already a text type in the database, no cast is needed.
                        expressionForLike = path.as(String.class);
                    } else {
                        // For ANY other type (Long, Integer, BigDecimal, jsonb-backed objects, etc.),
                        // we MUST explicitly cast it to text using PostgreSQL's native `text()` function.
                        // This generates SQL like: lower(text(column_name))
                        expressionForLike = cb.function("text", String.class, path);
                    }

                    // 6. Apply the LOWER function and the LIKE predicate.
                    likes.add(cb.like(cb.lower(expressionForLike), lowerPattern));

                } catch (IllegalArgumentException e) {
                    log.warn("Could not build like predicate for field '{}' in entity '{}': {}", fieldName, entityClass.getSimpleName(), e.getMessage());
                }
            }

            // 7. Combine all individual LIKE conditions with an OR.
            if (!likes.isEmpty()) {
                predicates.add(cb.or(likes.toArray(new Predicate[0])));
            }
        });
    }


    private T convertToDto(E entity) {
        try {
            T dto = tableClass.getDeclaredConstructor().newInstance();
            dto.formEntity(entity);
            return dto;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("无法转换 DTO", e);
        }
    }

    /**
     * 扩展筛选器入口
     */
    public void extendPredicate(String key, GenericRepository.PredicateBuilder<E> predicateBuilder) {
        this.extendPredicateBuilders.put(key, predicateBuilder);
        predicateManager.addAllPredicates(extendPredicateBuilders);
        refresh();
    }

    public void onResetFilterEvent() {
        predicateManager.clearPredicates();
        presetPredicate();
        predicateManager.addAllPredicates(extendPredicateBuilders);
        refresh();
    }

    protected List<SortOrder> getDefaultSortOrders() {
        return List.of(new SortOrder("id", SortOrder.Direction.DESC));
    }


    public void presetPredicate() {
        // 子类可注入默认 Predicate 逻辑
    }

    public void setDefaultFromModel(F defaultFromModel) {
        createFormInstance.setDefaultModel(defaultFromModel);
        this.defaultFormModel = defaultFromModel;

    }

    public Boolean enableUpdate() {
        return true;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        BasePage.super.beforeEnter(event);
    }

    public Boolean beforeSave(F f) {
        return true;
    }
}
