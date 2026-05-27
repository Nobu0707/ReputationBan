#!/usr/bin/env bash
set -euo pipefail

fail() { echo "[FAIL] $*" >&2; exit 1; }
pass() { echo "[PASS] $*"; }

SRC="src/main/java"
PLUGIN_YML="src/main/resources/plugin.yml"
BUILD_GRADLE="build.gradle.kts"

[[ -d "$SRC" ]] || fail "Missing source directory: $SRC"
[[ -f "$PLUGIN_YML" ]] || fail "Missing $PLUGIN_YML"
[[ -f "$BUILD_GRADLE" ]] || fail "Missing $BUILD_GRADLE"

if grep -R "^[[:space:]]*import[[:space:]]\\+net\\.luckperms\\." "$SRC" >/dev/null; then
  fail "LuckPerms API direct import detected in src/main/java"
fi
pass "No LuckPerms direct imports"

if grep -R "^[[:space:]]*import[[:space:]]\\+net\\.coreprotect\\." "$SRC" >/dev/null; then
  fail "CoreProtect API direct import detected in src/main/java"
fi
pass "No CoreProtect direct imports"

if grep -R "^[[:space:]]*import[[:space:]]\\+com\\.sk89q\\.worldguard\\." "$SRC" >/dev/null; then
  fail "WorldGuard API direct import detected in src/main/java"
fi
pass "No WorldGuard direct imports"

if grep -R "^[[:space:]]*import[[:space:]]\\+com\\.sk89q\\.worldedit\\." "$SRC" >/dev/null; then
  fail "WorldEdit API direct import detected in src/main/java"
fi
pass "No WorldEdit direct imports"

if grep -R "^[[:space:]]*import[[:space:]]\\+me\\.ryanhamshire\\.GriefPrevention\\." "$SRC" >/dev/null; then
  fail "GriefPrevention API direct import detected in src/main/java"
fi
if grep -R "^[[:space:]]*import[[:space:]]\\+me\\.ryanhamshire\\.griefprevention\\." "$SRC" >/dev/null; then
  fail "GriefPrevention API direct import detected in src/main/java"
fi
if grep -R "^[[:space:]]*import[[:space:]]\\+com\\.griefprevention\\." "$SRC" >/dev/null; then
  fail "GriefPrevention API direct import detected in src/main/java"
fi
pass "No GriefPrevention direct imports"

if grep -R "CoreProtectAPI\\|net\\.coreprotect\\.CoreProtect" "$SRC" >/dev/null; then
  fail "CoreProtect API type direct reference detected in src/main/java"
fi
pass "No CoreProtect API type direct references"

if grep -R "net\\.luckperms\\.api\\.model\\.user\\.User" "$SRC" >/dev/null; then
  fail "LuckPerms User type direct reference detected in src/main/java"
fi
pass "No LuckPerms User type direct references"

if grep -R "net\\.luckperms\\.api\\.LuckPerms" "$SRC" | grep -v "LuckPermsReflectionAdapter.java" >/dev/null; then
  fail "LuckPerms API class name escaped LuckPermsReflectionAdapter"
fi
pass "LuckPerms API class lookup is isolated"

if grep -R "com\\.sk89q\\.worldguard\\|com\\.sk89q\\.worldedit\\|BukkitAdapter\\|ApplicableRegionSet\\|ProtectedRegion\\|RegionContainer\\|RegionQuery\\|LocalPlayer\\|BlockVector3" "$SRC" \
    | grep -v "WorldGuardReflectionAdapter.java" >/dev/null; then
  fail "WorldGuard/WorldEdit API class name escaped WorldGuardReflectionAdapter"
fi
pass "WorldGuard API class lookup is isolated"

if grep -R "me\\.ryanhamshire\\.GriefPrevention\\|me\\.ryanhamshire\\.griefprevention\\|com\\.griefprevention\\|DataStore" "$SRC" \
    | grep -v "GriefPreventionReflectionAdapter.java" >/dev/null; then
  fail "GriefPrevention API class name escaped GriefPreventionReflectionAdapter"
fi
pass "GriefPrevention API class lookup is isolated"

[[ -f "$SRC/dev/modplugin/reputationban/integration/luckperms/LuckPermsReflectionAdapter.java" ]] \
  || fail "LuckPermsReflectionAdapter is missing"
[[ -f "$SRC/dev/modplugin/reputationban/integration/coreprotect/CoreProtectReflectionAdapter.java" ]] \
  || fail "CoreProtectReflectionAdapter is missing"
[[ -f "$SRC/dev/modplugin/reputationban/integration/worldguard/WorldGuardReflectionAdapter.java" ]] \
  || fail "WorldGuardReflectionAdapter is missing"
[[ -f "$SRC/dev/modplugin/reputationban/integration/griefprevention/GriefPreventionReflectionAdapter.java" ]] \
  || fail "GriefPreventionReflectionAdapter is missing"
pass "Reflection adapters are present"

grep -A8 "^softdepend:" "$PLUGIN_YML" | grep -q "LuckPerms" || fail "plugin.yml softdepend missing LuckPerms"
grep -A8 "^softdepend:" "$PLUGIN_YML" | grep -q "CoreProtect" || fail "plugin.yml softdepend missing CoreProtect"
grep -A8 "^softdepend:" "$PLUGIN_YML" | grep -q "WorldEdit" || fail "plugin.yml softdepend missing WorldEdit"
grep -A8 "^softdepend:" "$PLUGIN_YML" | grep -q "WorldGuard" || fail "plugin.yml softdepend missing WorldGuard"
grep -A8 "^softdepend:" "$PLUGIN_YML" | grep -q "GriefPrevention" || fail "plugin.yml softdepend missing GriefPrevention"
pass "plugin.yml softdepend includes LuckPerms, CoreProtect, WorldEdit, WorldGuard, and GriefPrevention"

grep -q 'compileOnly("net.luckperms:api:' "$BUILD_GRADLE" || fail "LuckPerms compileOnly dependency missing"
grep -q 'compileOnly("net.coreprotect:coreprotect:' "$BUILD_GRADLE" || fail "CoreProtect compileOnly dependency missing"
grep -q 'compileOnly("com.sk89q.worldguard:worldguard-bukkit:' "$BUILD_GRADLE" || fail "WorldGuard compileOnly dependency missing"
pass "Optional dependencies are compileOnly"

if grep -R "performRollback\\|performRestore\\|performPurge" "$SRC" >/dev/null; then
  fail "CoreProtect destructive rollback/restore/purge usage detected"
fi
pass "No CoreProtect destructive calls"

if grep -R "saveUser\\|data()\\.add\\|data()\\.remove\\|setPermission" "$SRC" >/dev/null; then
  fail "LuckPerms write API usage detected"
fi
pass "No LuckPerms write calls"

if grep -R "addRegion\\|removeRegion\\|saveChanges\\|setFlag\\|setPriority\\|setOwners\\|setMembers" "$SRC" >/dev/null; then
  fail "WorldGuard region/flag mutation usage detected"
fi
pass "No WorldGuard region or flag mutation calls"

if grep -R "createClaim\\|deleteClaim\\|resizeClaim\\|changeClaimOwner\\|setOwner\\|setManagers\\|setBuilders\\|setContainers\\|setAccessors" "$SRC" >/dev/null; then
  fail "GriefPrevention claim/trust mutation usage detected"
fi
pass "No GriefPrevention claim or trust mutation calls"

pass "Optional dependency safety checks completed"
