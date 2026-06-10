f = r'D:\mcay\claudeCode001\backend\src\main\java\com\example\aimanager\config\PublicPathsProperties.java'
raw = open(f, 'rb').read()
# Fix the doubled quote
raw = raw.replace(b'"/api/dict/**""', b'"/api/dict/**"')
open(f, 'wb').write(raw)
# Verify
lines = open(f, 'rb').read().split(b'\n')
print('Line 19:', lines[19])
