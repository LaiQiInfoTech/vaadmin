package dev.w0fv1.vaadmin.test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.w0fv1.vaadmin.entity.BaseManageEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.UUID.randomUUID;


@Getter
@Setter
@NoArgsConstructor
@Entity

@Table(name = "echo")
public class Echo implements BaseManageEntity<Long> {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "uuid", unique = true, length = 256)
    private String uuid = randomUUID().toString();

    @Column(name = "message")
    private String message;

    @Column(name = "long_message")
    private String longMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Echo parent;

    @Column(name = "flag")
    private Boolean flag;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private List<String> keywords = new ArrayList<>();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.NORMAL;

    @Column(name = "created_time")
    @CreationTimestamp
    private OffsetDateTime createdTime;

    @Column(name = "updated_time")
    @UpdateTimestamp
    private OffsetDateTime updatedTime;

    public Echo(String message) {
        this.message = message;
    }


    public enum Label {
        NEW,
        HOT,
        RECOMMENDED,
    }

    public enum Status {

        NORMAL("该状态将导致数据处于正常工作状态"),
        HIDDEN("该状态将导致数据隐藏,对用户不可见"),
        BANNED("该状态将导致数据对用户可见,并且显示封禁状态"),
        DELETED("该状态将导致数据被删除,会带来一系列后果!请尽量使用HIDDEN代替.");


        Status(String description) {
            this.description = description;
        }

        public Boolean isNormal() {
            return this == NORMAL;
        }

        public Boolean isNonNormal() {
            return this != NORMAL;
        }

        public Boolean isBanned() {
            return this == BANNED;
        }

        public Boolean isRecHidden() {
            return this != NORMAL;
        }

        public boolean isDeleted() {
            return this == DELETED;
        }

        public final String description;
    }

}
