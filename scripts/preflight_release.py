# -*- coding: utf-8 -*-
"""ICPM 发版前 preflight（发布到 GitHub / CF 前必跑）：
  1) 全量 mixin 声明位审计（@Mixin 目标类必须声明注入方法，剥注释防误抓）
  2) 数据 JSON 合法性（全部可解析）
  3) 资源闭合审计（equipment->armor png / blockstate->model / model->texture /
     worldgen state->blockstate / loot+recipe item->items 模型）
用法: python scripts/preflight_release.py    （无输出=通过；任何问题列文件+退出码 1）
"""
import json, os, re, subprocess, sys, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
DISASM = os.path.join(ROOT, "tmp_disasm")
JARS = {
    "common": r"C:\Users\Administrator\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-common\1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2\minecraft-common-1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2.jar",
    "client": r"C:\Users\Administrator\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-clientonly\1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2\minecraft-clientonly-1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2.jar",
}
zidx = {k: set(zipfile.ZipFile(j).namelist()) for k, j in JARS.items()}
issues = []

# ---------- 1. mixin 声明位 ----------
def strip_comments(src):
    src = re.sub(r"//.*", "", src)
    return re.sub(r"/\*.*?\*/", "", src, flags=re.S)

def mixin_files():
    out = []
    for base in (os.path.join(ROOT, "src", "main", "java", "name", "icpm", "mixin"),
                 os.path.join(ROOT, "src", "client", "java", "name", "icpm", "client", "mixin")):
        if os.path.isdir(base):
            out += [os.path.join(base, f) for f in sorted(os.listdir(base)) if f.endswith(".java")]
    return out

n_checked = 0
for path in mixin_files():
    src = strip_comments(open(path, encoding="utf-8-sig").read())
    imports = {simple: fq + "." + simple for fq, simple in re.findall(r"^import\s+([\w.]+)\.(\w+);", src, re.M)}
    targets = []
    for t in re.findall(r"@Mixin\(\s*([\w.$]+)\.class", src):
        fq = t if "." in t else imports.get(t, "")
        if fq.startswith("net.minecraft") or fq.startswith("com.mojang"):
            targets.append(fq)
    methods = set(re.findall(r"@(?:Inject|Redirect|ModifyVariable|ModifyArgs|ModifyConstant|WrapOperation|Overwrite|WrapMethod)\(\s*method\s*=\s*\"(\w+)", src))
    if not targets or not methods:
        continue
    for fq in set(targets):
        cls = fq.replace(".", "/") + ".class"
        jar = next((k for k, s in zidx.items() if cls in s), None)
        if jar is None:
            continue
        tmp = os.path.join(DISASM, "_pf_" + os.path.basename(cls))
        with zipfile.ZipFile(JARS[jar]) as zz:
            open(tmp, "wb").write(zz.read(cls))
        out = subprocess.run(["java", "-cp", ".", "Disasm", os.path.basename(tmp)],
                             capture_output=True, text=True, cwd=DISASM, timeout=60).stdout
        declared = set(re.findall(r"=== method (\w+) \(", out))
        for m in methods:
            n_checked += 1
            if m not in declared:
                issues.append(f"MIXIN {os.path.basename(path)}: @Mixin({fq}) 注入 {m} 但目标类未声明")
        try: os.remove(tmp)
        except OSError: pass

# ---------- 2. 数据 JSON 合法性 ----------
for dp, _, fs in os.walk(RES):
    for f in fs:
        if f.endswith(".json"):
            p = os.path.join(dp, f)
            try:
                json.load(open(p, encoding="utf-8-sig"))
            except Exception as e:
                issues.append(f"JSON {os.path.relpath(p, RES)}: {e}")

def chk(cond, msg):
    if cond:
        issues.append(msg)

# ---------- 3. 资源闭合 ----------
for f in os.listdir(os.path.join(RES, "assets", "icpm", "equipment")):
    j = json.load(open(os.path.join(RES, "assets", "icpm", "equipment", f), encoding="utf-8-sig"))
    for arr in (j.get("layers") or {}).values():
        for l in arr:
            t = l.get("texture", "").split(":")[-1]
            for L in (1, 2):
                chk(not os.path.exists(os.path.join(RES, "assets", "icpm", "textures", "models", "armor", f"{t}_layer_{L}.png")),
                    f"equipment {f}: {t}_layer_{L}.png 缺失")
for f in os.listdir(os.path.join(RES, "assets", "icpm", "blockstates")):
    d = open(os.path.join(RES, "assets", "icpm", "blockstates", f), encoding="utf-8-sig").read()
    for m in re.finditer(r'"model"\s*:\s*"(icpm:[a-z_0-9/]+)"', d):
        chk(not os.path.exists(os.path.join(RES, "assets", "icpm", "models", m.group(1).split(":", 1)[1] + ".json")),
            f"blockstate {f}: model {m.group(1)} 文件缺失")
for f in os.listdir(os.path.join(RES, "assets", "icpm", "models", "block")):
    try: j = json.load(open(os.path.join(RES, "assets", "icpm", "models", "block", f), encoding="utf-8-sig"))
    except Exception: continue
    for t in (j.get("textures") or {}).values():
        if t.startswith("icpm:"):
            chk(not os.path.exists(os.path.join(RES, "assets", "icpm", "textures", t.split(":", 1)[1] + ".png")),
                f"model block/{f}: texture {t} 缺失")
for f in os.listdir(os.path.join(RES, "data", "icpm", "worldgen", "configured_feature")):
    d = open(os.path.join(RES, "data", "icpm", "worldgen", "configured_feature", f), encoding="utf-8-sig").read()
    for m in re.finditer(r'"Name"\s*:\s*"(icpm:[a-z_0-9]+)"', d):
        chk(not os.path.exists(os.path.join(RES, "assets", "icpm", "blockstates", m.group(1).split(":")[1] + ".json")),
            f"worldgen {f}: block {m.group(1)} 无 blockstate")

# loot/recipe 引用 icpm 物品必须有模型（items/ 或兼容 models/item/）
def walk(p):
    for dp, _, fs in os.walk(p):
        for f in fs:
            if f.endswith(".json"):
                yield os.path.join(dp, f)
for f in walk(os.path.join(RES, "data")):
    if "/recipe/" not in f and "/loot_table/" not in f:
        continue
    d = open(f, encoding="utf-8-sig").read()
    for m in re.finditer(r'"(?:item|name|result|id)"\s*:\s*"(icpm:[a-z_0-9]+)"', d):
        iid = m.group(1).split(":")[1]
        has = (os.path.exists(os.path.join(RES, "assets", "icpm", "items", iid + ".json"))
               or os.path.exists(os.path.join(RES, "assets", "icpm", "models", "item", iid + ".json")))
        if not has:
            issues.append(f"数据 {os.path.relpath(f, RES)}: 引用物品 {iid} 无 items/model 模型")

print(f"[preflight] mixin 对 {n_checked}；数据 JSON {sum(1 for dp,_,fs in os.walk(RES) for f in fs if f.endswith('.json'))} 文件")
if issues:
    print(f"[preflight] 发现 {len(issues)} 个问题:")
    for i in issues:
        print(" -", i)
    sys.exit(1)
print("[preflight] 全部通过，0 问题")
