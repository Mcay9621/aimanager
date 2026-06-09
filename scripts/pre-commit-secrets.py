#!/usr/bin/env python3
"""
Git pre-commit hook: 扫描暂存区中的代码/SQL/YML 文件，
检查是否包含密码、AKSK、API Key 等敏感数据，包含则替换为 ****。

使用方式（在项目根目录）:
  python scripts/pre-commit-secrets.py

Git 钩子自动调用:
  由 .git/hooks/pre-commit 脚本触发
"""

import re
import sys
import os
import subprocess

# ── 敏感键名列表（YAML/properties 中 key 匹配） ──
SENSITIVE_KEYS_PATTERN = (
    r'(password|passwd|pwd|secret[_-]?key|secret|'
    r'api[_-]?key|apikey|'
    r'access[_-]?key|accesskey|'
    r'aksk|'
    r'token|auth_token|refresh_token|'
    r'jd_aks|jd_sk|'
    r'private_key|private-key)'
)

# ── YAML 行替换模式：  key: value  →  key: **** ──
YAML_KEY = re.compile(
    r'^\s*(?:' + SENSITIVE_KEYS_PATTERN + r')\s*:\s*',
    re.IGNORECASE
)

# ── SQL 敏感模式 ──
SQL_PATTERNS = [
    # PASSWORD = 'xxx' / PASSWORD='xxx'
    (re.compile(r"(PASSWORD\s*=\s*['\"])([^'\"]+)(['\"])", re.IGNORECASE),
     r"\1****\3"),
    # IDENTIFIED BY 'xxx'
    (re.compile(r"(IDENTIFIED\s+BY\s*['\"])([^'\"]+)(['\"])", re.IGNORECASE),
     r"\1****\3"),
    # CREDENTIALS 'xxx'
    (re.compile(r"(CREDENTIALS\s+['\"])([^'\"]+)(['\"])", re.IGNORECASE),
     r"\1****\3"),
    # mysql -pXxx  (password after -p without space)
    (re.compile(r"(-p)(\S+)"),
     r"\1****"),
    # ENCRYPTED_PASSWORD / DECRYPT_KEY
    (re.compile(r"(ENCRYPTED_PASSWORD\s*=\s*['\"])([^'\"]+)(['\"])", re.IGNORECASE),
     r"\1****\3"),
]

# ── 代码中字符串赋值的敏感模式（仅警告，不自动替换） ──
CODE_WARN_PATTERNS = [
    re.compile(
        r'(?i)(password|apiKey|api_key|apikey|secret|secretKey|secret_key|token|accessKey|access_key)'
        r'\s*[=:]\s*["\'][^"\']+["\']'
    ),
]

# 文件扩展名分类
YAML_EXT = {'.yml', '.yaml'}
SQL_EXT = {'.sql'}
PROPS_EXT = {'.properties', '.env', '.conf'}
CODE_EXT = {'.java', '.js', '.ts', '.vue', '.py', '.xml', '.json', '.cfg'}
ALL_EXT = YAML_EXT | SQL_EXT | PROPS_EXT | CODE_EXT


def get_staged_files():
    """获取暂存区中需要检查的文件列表"""
    result = subprocess.run(
        ['git', 'diff', '--cached', '--name-only', '--diff-filter=ACMR'],
        capture_output=True, text=True, cwd=get_git_root()
    )
    if result.returncode != 0:
        print("ERROR: 无法获取暂存区文件列表", file=sys.stderr)
        sys.exit(1)
    return [f for f in result.stdout.strip().split('\n') if f]


def get_git_root():
    """获取 git 根目录"""
    result = subprocess.run(
        ['git', 'rev-parse', '--show-toplevel'],
        capture_output=True, text=True
    )
    return result.stdout.strip()


def read_staged_file(filepath):
    """读取暂存区中的文件内容"""
    result = subprocess.run(
        ['git', 'show', ':' + filepath],
        capture_output=True, text=True, cwd=get_git_root()
    )
    if result.returncode != 0:
        return None
    return result.stdout


def restage_file(filepath):
    """重新暂存（替换后的）文件"""
    result = subprocess.run(
        ['git', 'add', filepath],
        capture_output=True, text=True, cwd=get_git_root()
    )
    return result.returncode == 0


def scan_yaml(content, filepath):
    """扫描并替换 YAML/properties 文件中的敏感值"""
    lines = content.split('\n')
    changed = False
    warnings = []
    new_lines = []

    for i, line in enumerate(lines):
        if YAML_KEY.match(line):
            # 提取前缀（key: 部分）
            m = YAML_KEY.match(line)
            prefix = m.group()
            after = line[m.end():].strip()
            # 跳过已经是 **** 的情况
            if after == '****' or after.startswith('****'):
                new_lines.append(line)
                continue
            # 跳过空值
            if not after:
                new_lines.append(line)
                continue
            # 跳过环境变量占位符 ${...}、{{...}} 等
            if '${' in after or '{{' in after:
                new_lines.append(line)
                continue
            # 替换
            indentation = line[:len(line) - len(line.lstrip())]
            new_line = prefix + '****'
            new_lines.append(new_line)
            changed = True
            warnings.append(f"  {filepath}:{i+1}  {line.strip()[:60]}  →  {prefix}****")
        else:
            new_lines.append(line)

    return '\n'.join(new_lines), changed, warnings


def scan_sql(content, filepath):
    """扫描并替换 SQL 文件中的敏感值"""
    changed = False
    warnings = []
    new_content = content

    for pattern, replacement in SQL_PATTERNS:
        matches = pattern.findall(new_content)
        if matches:
            for match in matches:
                if match[1] == '****':
                    continue
            new_content = pattern.sub(replacement, new_content)
            changed = True
            warnings.append(f"  {filepath}: 匹配到 {pattern.pattern[:40]}... 已替换")

    return new_content, changed, warnings


def scan_code(content, filepath):
    """扫描代码文件，仅警告不自动替换"""
    warnings = []
    for pattern in CODE_WARN_PATTERNS:
        for match in pattern.finditer(content):
            line_num = content[:match.start()].count('\n') + 1
            val = match.group()
            # 跳过已是 **** 的
            if '****' in val:
                continue
            warnings.append(f"  {filepath}:{line_num} ⚠ {val[:60]}")
    return content, False, warnings


def main():
    git_root = get_git_root()
    files = get_staged_files()

    if not files:
        sys.exit(0)

    total_warnings = []
    auto_fix_count = 0

    for filepath in files:
        ext = os.path.splitext(filepath)[1].lower()

        if ext not in ALL_EXT:
            continue

        content = read_staged_file(filepath)
        if content is None:
            continue

        if ext in YAML_EXT:
            new_content, changed, warns = scan_yaml(content, filepath)
            if changed:
                # 写入工作区文件
                full_path = os.path.join(git_root, filepath)
                try:
                    with open(full_path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    restage_file(filepath)
                    auto_fix_count += 1
                except IOError as e:
                    print(f"ERROR: 无法写入 {filepath}: {e}", file=sys.stderr)
            total_warnings.extend(warns)

        elif ext in SQL_EXT:
            new_content, changed, warns = scan_sql(content, filepath)
            if changed:
                full_path = os.path.join(git_root, filepath)
                try:
                    with open(full_path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    restage_file(filepath)
                    auto_fix_count += 1
                except IOError as e:
                    print(f"ERROR: 无法写入 {filepath}: {e}", file=sys.stderr)
            total_warnings.extend(warns)

        elif ext in PROPS_EXT:
            new_content, changed, warns = scan_yaml(content, filepath)
            if changed:
                full_path = os.path.join(git_root, filepath)
                try:
                    with open(full_path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    restage_file(filepath)
                    auto_fix_count += 1
                except IOError as e:
                    print(f"ERROR: 无法写入 {filepath}: {e}", file=sys.stderr)
            total_warnings.extend(warns)

        elif ext in CODE_EXT:
            _, _, warns = scan_code(content, filepath)
            total_warnings.extend(warns)

    if total_warnings or auto_fix_count > 0:
        print("\n" + "=" * 60)
        print("  [SECURITY] 敏感数据扫描结果")
        print("=" * 60)

    if total_warnings:
        for w in total_warnings:
            print(w)

    if auto_fix_count > 0:
        print(f"\n  [OK] 已自动替换 {auto_fix_count} 个文件中的敏感值 -> ****")
        print("  已重新暂存，可直接提交。")

    if total_warnings:
        # 有代码中的警告（未自动修复的）
        code_warns = [w for w in total_warnings if '⚠' in w]
        if code_warns:
            print(f"\n  [WARN] 发现 {len(code_warns)} 处代码中可能包含敏感数据（未自动替换）")
            print("  请手动检查上述警告行，确认是否需要排除在提交之外。\n")
            sys.exit(1)  # 阻止提交

    sys.exit(0)


if __name__ == '__main__':
    main()
