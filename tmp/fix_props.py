f = r"D:\mcay\claudeCode001\backend\src\main\java\com\example\aimanager\config\PublicPathsProperties.java"
c = open(f, "r", encoding="utf-8").read()
c = c.replace(
    '"/api/chat,\n            "/api/dict/**"',
    '"/api/chat",\n            "/api/dict/**"'
)
open(f, "w", encoding="utf-8").write(c)
print("Fixed")
