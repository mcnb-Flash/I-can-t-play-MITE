# -*- coding: utf-8 -*-
"""全量 mixin 审计：@Mixin(目标类) 必须声明注入的方法（防 target-not-found 启动崩）。
扫描 src/main + src/client 全部 mixin 文件，用反汇编核对 vanilla 类方法表。
"""
import os, re, subprocess, sys, zipfile

ROOT = r"C:\Users\Administrator\Desktop\I can't play MITE"
DISASM = os.path.join(ROOT, "tmp_disasm")
JARS = {
    "common": r"C:\Users\Administrator\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-common\1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2\minecraft-common-1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2.jar",
    "client": r"C:\Users\Administrator\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-clientonly\1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2\minecraft-clientonly-1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2.jar",
}
zidx = {k: set(zipfile.ZipFile(j).namelist()) for k, j in JARS.items()}

def find_mixin_files():
    out = []
    for base in (os.path.join(ROOT, "src", "main", "java", "name", "icpm", "mixin"),
                 os.path.join(ROOT, "src", "client", "java", "name", "icpm", "client", "mixin")):
        if os.path.isdir(base):
            for f in sorted(os.listdir(base)):
                if f.endswith(".java"):
                    out.append(os.path.join(base, f))
    return out

def strip_comments(src):
    # 去 // 行注释与 /* */ 块注释（含 javadoc），避免误抓注释里的 @Mixin 字样
    src = re.sub(r'//.*', '', src)
    src = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    return src

def parse(src):
    src = strip_comments(src)
    imports = {}
    for m in re.finditer(r"^import\s+([\w.]+)\.(\w+);", src, re.M):
        imports[m.group(2)] = m.group(1) + "." + m.group(2)
    targets, methods = [], set()
    for mm in re.finditer(r"@Mixin\(\s*([\w.$]+)\.class", src):
        t = mm.group(1)
        fqcn = t if "." in t else imports.get(t)
        if fqcn and (fqcn.startswith("net.minecraft") or fqcn.startswith("com.mojang")):
            targets.append(fqcn)
    for m in re.finditer(r"@(?:Inject|Redirect|ModifyVariable|ModifyArgs|ModifyConstant|WrapOperation|Overwrite|WrapMethod)\(\s*method\s*=\s*\"([\w$]+)", src):
        methods.add(m.group(1))
    return targets, methods

issues = []
checked = 0
for path in find_mixin_files():
    src = open(path, encoding="utf-8-sig").read()
    targets, methods = parse(src)
    if not targets or not methods:
        continue
    for fqcn in set(targets):
        cls = fqcn.replace(".", "/") + ".class"
        jar = next((k for k, s in zidx.items() if cls in s), None)
        if jar is None:
            issues.append(f"{os.path.basename(path)}: 类 {fqcn} 不在 vanilla jar（跳过，可能为 mod 内部/接口）")
            continue
        tmp = os.path.join(DISASM, "_audit_" + os.path.basename(cls))
        with zipfile.ZipFile(JARS[jar]) as zz:
            open(tmp, "wb").write(zz.read(cls))
        out = subprocess.run(["java", "-cp", ".", "Disasm", os.path.basename(tmp)],
                             capture_output=True, text=True, cwd=DISASM, timeout=60).stdout
        declared = set(re.findall(r"=== method (\w+) \(", out))
        for m in sorted(methods):
            checked += 1
            if m not in declared:
                issues.append(f"[MISS] {os.path.basename(path)}: @Mixin({fqcn}) 注入 {m} 但该类未声明")
        try: os.remove(tmp)
        except OSError: pass

print(f"核查 {checked} 个 (target,method) 对")
if issues:
    print("发现问题:")
    for i in issues: print(" -", i)
else:
    print("全部通过，0 隐患")
sys.exit(1 if issues else 0)
