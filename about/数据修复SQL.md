# 数据修复 SQL 汇总
> 每次数据表结构变更，可能会导致数据缺失，此时需要用程序或者 SQL 进行调整

**page_paunch 增加 depart 字段**
> add on 2023-08-16

```sql
update page_launch p set p.depart=(select concat(d.id,"-", d.name) from account a left join department d on a.did = d.id where a.id = p.uid) where p.depart=''
```