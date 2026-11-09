package com.fangu.middle.org;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("franchise_level")
public class FranchiseLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String levelCode;
    private String levelName;
    private Integer sortNo;
    private String status;      // ENABLED / DISABLED
    private String remark;
    private OffsetDateTime createdAt;
}
