-- 创建数据库（UTF-8编码，支持中文）
CREATE DATABASE elderly_care
    WITH 
    ENCODING = 'UTF8'
    LC_COLLATE = 'zh_CN.UTF-8'
    LC_CTYPE = 'zh_CN.UTF-8'
    TEMPLATE = template0
    OWNER = postgres;