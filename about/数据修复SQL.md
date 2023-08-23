# 数据修复 SQL 汇总
> 每次数据表结构变更，可能会导致数据缺失，此时需要用程序或者 SQL 进行调整

**增加请求转发详情持久化**
> add on 2023-08-23
 
```sql
ALTER TABLE terminal_log ADD host varchar(100) DEFAULT '' AFTER method;

CREATE TABLE `terminal_log_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reqHeader` text COMMENT '请求头',
  `reqBody` text COMMENT '请求主体（字符串）',
  `resHeader` text COMMENT '响应头',
  `resBody` text COMMENT '响应主体（BASE64格式，需还原成字节，然后根据响应头转码）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

**page_paunch 增加 depart 字段**
> add on 2023-08-16

```sql
update page_launch p set p.depart=(select concat(d.id,"-", d.name) from account a left join department d on a.did = d.id where a.id = p.uid) where p.depart=''
```