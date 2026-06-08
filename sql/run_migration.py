import os
import pymysql, re

DB_HOST = os.environ.get('DB_HOST')
DB_PORT = int(os.environ.get('DB_PORT', '3306'))
DB_USER = os.environ.get('DB_USER')
DB_PASSWORD = os.environ.get('DB_PASSWORD')
DB_NAME = os.environ.get('DB_NAME', 'ai_manager')

if not all([DB_HOST, DB_USER, DB_PASSWORD]):
    raise ValueError("请设置环境变量 DB_HOST、DB_USER、DB_PASSWORD")

with open('D:/mcay/claudeCode001/sql/migration_new_types.sql', 'r', encoding='utf-8') as f:
    content = f.read()

lines = content.split('\n')
statements = []
current = ''
for line in lines:
    if line.strip().startswith('--'):
        continue
    current += line + '\n'
    if line.strip().endswith(';') and 'CREATE TABLE' in current:
        statements.append(current.strip())
        current = ''

conn = pymysql.connect(
    host=DB_HOST, port=DB_PORT, user=DB_USER,
    password=DB_PASSWORD, database=DB_NAME, charset='utf8mb4'
)

try:
    with conn.cursor() as cursor:
        count = 0
        for stmt in statements:
            print(f"Executing: {stmt[0:60]}...")
            cursor.execute(stmt)
            m = re.search(r"COMMENT='([^']+)'\)", stmt)
            name = m.group(1) if m else '?'
            print(f"  -> Created: {name}")
            count += 1
        conn.commit()
        print(f"\nDone: {count} tables created")
finally:
    conn.close()
