package dev.w0fv1.vaadmin.test;

import dev.w0fv1.vaadmin.view.form.model.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@FormConfig(title = "Echo 表单", description = "用于编辑 Echo 数据")
public class EchoF extends BaseEntityFormModel<Echo, Long> {

    @FormField(id = true, onlyUpdate = true, enabled = false)
    private Long id;

    @FormField(enabled = false, onlyUpdate = true)
    private String uuid;

    @Size(min = 1, message = "消息不能为空")
    @FormField(title = "消息", description = "简短消息内容", nullable = false)
    private String message;

    @FormField(title = "长消息", longText = true)
    private String longMessage;

    @FormField(title = "标记", description = "是否标记此条 Echo")
    private Boolean flag;

    @FormField(title = "关键词", description = "以英文分号分隔的关键词", subType = String.class)
    private List<String> keywords = new ArrayList<>();

    @FormEntitySelectField(entityField =@EntityField(entityType = Echo.class, entityMapper = EchoEntityFieldMapper.ParentFieldMapper.class))
    @FormField(title = "父 Echo ID", description = "可为空，表示父节点")
    private Long parentId;

    @FormField(title = "状态", defaultValue = "NORMAL")
    private Echo.Status status = Echo.Status.NORMAL;

    @FormField(title = "创建时间", enabled = false, onlyUpdate = true)
    private OffsetDateTime createdTime;

    @FormField(title = "更新时间", enabled = false, onlyUpdate = true)
    private OffsetDateTime updatedTime;


    public EchoF() {
    }

    public EchoF(String message) {
        this.message = message;
    }

    @Override
    public Echo toEntity() {
        Echo echo = new Echo();
        echo.setMessage(message);
        echo.setLongMessage(longMessage);
        echo.setFlag(flag);
        echo.setKeywords(keywords);
        echo.setStatus(status);
        return echo;
    }

    @Override
    public void translate(Echo echo) {
        echo.setMessage(message);
        echo.setLongMessage(longMessage);
        echo.setFlag(flag);
        echo.setKeywords(keywords);
        echo.setStatus(status);
    }

}
