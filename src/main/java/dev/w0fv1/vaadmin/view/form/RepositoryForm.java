package dev.w0fv1.vaadmin.view.form;

import com.vaadin.flow.component.notification.NotificationVariant;
import dev.w0fv1.vaadmin.GenericRepository;
import dev.w0fv1.vaadmin.entity.BaseManageEntity;
import dev.w0fv1.vaadmin.view.form.component.*;
import dev.w0fv1.vaadmin.view.form.model.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import static dev.w0fv1.vaadmin.view.tools.Notifier.showNotification;


@Slf4j
public class RepositoryForm<
        F extends BaseEntityFormModel<E, ID>,
        E extends BaseManageEntity<ID>,
        ID> extends BaseForm<F> {

    private final Class<E> entityClass;

    private final GenericRepository genericRepository;
    private final Runnable onCancel;
    private final Save<ID> save;
    private BeforeSave<F> beforeSave;


    private final Boolean isUpdate;

    public RepositoryForm(F fromModel, Save<ID> save, Runnable onCancel, GenericRepository genericRepository) {
        super(fromModel, fromModel != null && fromModel.getId() != null);
        this.isUpdate = fromModel.getId() != null;
        this.genericRepository = genericRepository;
        this.entityClass = fromModel.getEntityClass();
        this.onCancel = onCancel;
        this.save = save;
    }

    public RepositoryForm(F fromModel, BeforeSave<F> beforeSave, Save<ID> save, Runnable onCancel, GenericRepository genericRepository) {
        super(fromModel, fromModel != null && fromModel.getId() != null);
        this.isUpdate = fromModel.getId() != null;
        this.genericRepository = genericRepository;
        this.entityClass = fromModel.getEntityClass();
        this.onCancel = onCancel;
        this.save = save;
        this.beforeSave = beforeSave;
    }

    public RepositoryForm(Class<F> fromClass, Save<ID> save, Runnable onCancel, GenericRepository genericRepository) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this(fromClass.getDeclaredConstructor().newInstance(), save, onCancel, genericRepository);
    }

    public RepositoryForm(F fromModel, GenericRepository genericRepository) {
        this(fromModel, null, null, genericRepository);
    }

    public RepositoryForm(Class<F> fromClass, GenericRepository genericRepository) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this(fromClass.getDeclaredConstructor().newInstance(), null, null, genericRepository);
    }

    @Override
    public void onCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    @Override
    BaseFormFieldComponent<?> extendMapComponent(Field field, F formModel) {

        FormField formField = field.getAnnotation(FormField.class);

        if (field.isAnnotationPresent(FormEntitySelectField.class) && Collection.class.isAssignableFrom(field.getType())) {
            return new MultiEntitySelectField<>(field, formModel, genericRepository);
        } else if (field.isAnnotationPresent(FormEntitySelectField.class) && !Collection.class.isAssignableFrom(field.getType())) {
            return new SingleEntitySelectField<>(field, formModel, genericRepository);
        } else if (field.isAnnotationPresent(CustomRepositoryFormFieldComponent.class)) {
            Class<? extends CustomRepositoryFormFieldComponentBuilder> fieldComponentBuilder = field.getAnnotation(CustomRepositoryFormFieldComponent.class).value();
            try {
                CustomRepositoryFormFieldComponentBuilder customFormFieldComponentBuilder = fieldComponentBuilder.getDeclaredConstructor().newInstance();
                BaseFormFieldComponent<?> formFieldComponent = customFormFieldComponentBuilder.build(field, formModel, genericRepository);
                return formFieldComponent;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        return null;
    }


    @Override
    public Boolean save(F fromModel) {

        log.info(fromModel.toString());

        E model = genericRepository.execute(status -> {
            E saveModel = null;
            try {
                if (this.isUpdate) {
                    saveModel = genericRepository.find(fromModel.getId(), entityClass);
                    fromModel.translate(saveModel);
                } else {
                    saveModel = fromModel.toEntity();
                }
                for (Field declaredField : fromModel.getClass().getDeclaredFields()) {
                    if (!declaredField.isAnnotationPresent(FormField.class)) {
                        continue;
                    }
                    FormField fromField = declaredField.getAnnotation(FormField.class);


                    if (!declaredField.isAnnotationPresent(FormEntitySelectField.class) && !declaredField.isAnnotationPresent(EntityField.class)) {
                        continue;
                    }
                    EntityField entityField = null;
                    if (declaredField.isAnnotationPresent(FormEntitySelectField.class)) {
                        entityField = declaredField.getAnnotation(FormEntitySelectField.class).entityField();
                    } else {
                        entityField = declaredField.getAnnotation(EntityField.class);
                    }


                    Class<? extends BaseManageEntity<?>> entityClass = entityField.entityType();
                    Object mapper = entityField.entityMapper().getDeclaredConstructor().newInstance();

                    declaredField.setAccessible(true);

                    if (declaredField.getType().equals(List.class)) {
                        List<ID> ids = (List<ID>) declaredField.get(fromModel);
                        List entities = genericRepository.findAll(ids, entityClass);
                        applyMapper(mapper, saveModel, entities);
                    } else {
                        ID id = (ID) declaredField.get(fromModel);
                        if (id == null && fromField.nullable()) {
                            applyMapper(mapper, saveModel, null);
                            continue;
                        }
                        if (id == null) {
                            throw new RuntimeException("id == null && !fromField.nullable()");
                        }

                        if (id instanceof Number && new BigDecimal(id.toString()).compareTo(BigDecimal.ZERO) == 0) {
                            continue;
                        }


                        Object entity = genericRepository.find(id, entityClass);
                        applyMapper(mapper, saveModel, entity);
                    }
                }
                saveModel = genericRepository.save(saveModel);


                for (Field declaredField : fromModel.getClass().getDeclaredFields()) {
                    if (!declaredField.isAnnotationPresent(RepositoryMapField.class)) {
                        continue;
                    }
                    RepositoryMapField repositoryMapField = declaredField.getAnnotation(RepositoryMapField.class);

                    if (repositoryMapField.mapper() == null) {
                        continue;
                    }

                    RepositoryFieldMapper repositoryFieldMapper = repositoryMapField.mapper().getDeclaredConstructor().newInstance();
                    declaredField.setAccessible(true);

                    repositoryFieldMapper.map(genericRepository, saveModel, fromModel);
                }


                if (save != null) {
                    save.run(saveModel.getId());
                }

            } catch (Exception e) {
                // 回滚事务
                status.setRollbackOnly();
                e.printStackTrace();
                showNotification("保存失败！" + e.getMessage(), NotificationVariant.LUMO_ERROR);

                return null;
            }
            return saveModel;
        });


        if (model == null || model.getId() == null) {
            return false;
        }
        showNotification("保存成功！", NotificationVariant.LUMO_SUCCESS);
        return true;

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void applyMapper(Object mapper, Object target, Object value) throws Exception {
        if (mapper == null) {
            throw new IllegalArgumentException("entityMapper instance is null");
        }

        if (mapper instanceof BiConsumer<?, ?> biConsumer) {
            ((BiConsumer) biConsumer).accept(target, value);
            return;
        }

        // Most mapper-style APIs use an `accept(target, value)` method (e.g. BiConsumer-like).
        // Use reflection to avoid coupling to a specific upstream mapper interface/package.
        Method accept;
        try {
            accept = mapper.getClass().getMethod("accept", Object.class, Object.class);
        } catch (NoSuchMethodException ignored) {
            accept = null;
        }

        if (accept == null) {
            // fallback: any "accept" method with 2 parameters
            for (Method m : mapper.getClass().getMethods()) {
                if (!m.getName().equals("accept")) continue;
                if (m.getParameterCount() != 2) continue;
                accept = m;
                break;
            }
        }

        if (accept == null) {
            throw new NoSuchMethodException("Mapper has no accept(target, value): " + mapper.getClass().getName());
        }

        accept.invoke(mapper, target, value);
    }

    public interface Save<ID> {
        /**
         * Runs this operation.
         */
        void run(ID id);
    }

    public interface BeforeSave<F> {
        /**
         * Runs this operation.
         */
        Boolean run(F f);
    }

    @Override
    protected boolean beforeSave(F data) {
        if (beforeSave == null) {
            return super.beforeSave(data);
        }
        return beforeSave.run(data);
    }
}
