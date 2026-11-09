package com.fangu.middle.org;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@TableName("franchisee")
public class Franchisee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String franchiseeCode;
    private String name;
    private String contactName;
    private String contactPhone;
    private String region;
    private String address;
    private String levelCode;
    private LocalDate signDate;
    private LocalDate expireDate;
    private BigDecimal deposit;
    private String status;      // NORMAL / FROZEN / TERMINATED
    private String contractUrl;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
