package dev.w0fv1.vaadmin.test;

import dev.w0fv1.vaadmin.view.table.model.BaseEntityTableModel;
import dev.w0fv1.vaadmin.view.table.model.TableConfig;
import dev.w0fv1.vaadmin.view.table.model.TableField;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@RequiredArgsConstructor
@TableConfig(title = "Echo 管理", description = "用于管理 Echo 实体", likeSearch = true)
public class EchoT implements BaseEntityTableModel<Echo, Long> {

    @TableField(displayName = "ID", order = 1, id = true)
    private Long id;

    @TableField(displayName = "UUID", order = 2)
    private String uuid;

    @TableField(displayName = "消息", order = 3)
    private String message;

    @TableField(displayName = "长消息", order = 4)
    private String longMessage;

    @TableField(displayName = "标记", order = 5)
    private Boolean flag;

    @TableField(displayName = "关键词", order = 6, likeSearch = true, sqlType = TableField.SqlType.JSONB)
    private List<String> keywords;

    @TableField(displayName = "状态", order = 7)
    private Echo.Status status;

    @TableField(displayName = "创建时间", order = 8)
    private OffsetDateTime createdTime;

    @TableField(displayName = "更新时间", order = 9)
    private OffsetDateTime updatedTime;

    @TableField(displayName = "父节点ID", order = 10)
    private Long parentId;

    @Override
    public void formEntity(Echo echo) {
        this.id = echo.getId();
        this.uuid = echo.getUuid();
        this.message = echo.getMessage();
        this.longMessage = echo.getLongMessage();
        this.flag = echo.getFlag();
        this.keywords = echo.getKeywords();
        this.status = echo.getStatus();
        this.createdTime = echo.getCreatedTime();
        this.updatedTime = echo.getUpdatedTime();
        this.parentId = echo.getParent() != null ? echo.getParent().getId() : null;
    }

    @Override
    public EchoF toFormModel() {
        EchoF form = new EchoF("默认内容");
        form.setId(id);
        form.setUuid(uuid);
        form.setMessage(message);
        form.setLongMessage(longMessage);
        form.setFlag(flag);
        form.setKeywords(keywords);
        form.setStatus(status);
        form.setCreatedTime(createdTime);
        form.setUpdatedTime(updatedTime);
        form.setParentId(parentId);
        return form;
    }
}
