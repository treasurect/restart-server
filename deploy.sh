#!/bin/bash

set -Eeuo pipefail

# =========================================================
# restart-server 自动发布脚本
# 用法：
#   ./deploy.sh deploy
#   ./deploy.sh status
#   ./deploy.sh restart
#   ./deploy.sh logs
#   ./deploy.sh rollback
# =========================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/deploy.conf"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "错误：配置文件不存在：$CONFIG_FILE"
    exit 1
fi

# 配置文件是可信的本地 Shell 配置
# shellcheck disable=SC1090
source "$CONFIG_FILE"

SSH_USER="${SSH_USER:-ubuntu}"
SSH_HOST="${SSH_HOST:-}"
SSH_PORT="${SSH_PORT:-22}"
SSH_KEY="${SSH_KEY:-}"
SERVER_DIR="${SERVER_DIR:-/opt/restart-server}"
SERVICE_NAME="${SERVICE_NAME:-restart-server}"
KEEP_BACKUPS="${KEEP_BACKUPS:-5}"
MAVEN_ARGS="${MAVEN_ARGS:--DskipTests}"

if [ -z "$SSH_HOST" ]; then
    echo "错误：请先在 deploy.conf 中设置 SSH_HOST"
    exit 1
fi

REMOTE="${SSH_USER}@${SSH_HOST}"

SSH_OPTIONS=(
    -p "$SSH_PORT"
    -o ConnectTimeout=10
    -o ServerAliveInterval=30
)

SCP_OPTIONS=(
    -P "$SSH_PORT"
    -o ConnectTimeout=10
)

if [ -n "$SSH_KEY" ]; then
    SSH_OPTIONS+=(-i "$SSH_KEY")
    SCP_OPTIONS+=(-i "$SSH_KEY")
fi

# 通过环境变量关闭 SSH 首次连接交互提示以外的隐式配置。
# 主机密钥验证仍使用 OpenSSH 默认策略，不关闭验证。

ssh_run() {
    ssh "${SSH_OPTIONS[@]}" "$REMOTE" "$@"
}

show_help() {
    cat <<'EOF'
用法：
  ./deploy.sh deploy     打包、上传、备份、部署、验证及失败回滚
  ./deploy.sh status     查看服务状态
  ./deploy.sh restart    重启服务
  ./deploy.sh logs       实时查看最近日志
  ./deploy.sh rollback   回滚到最近的历史版本
EOF
}

# 自动寻找 Spring Boot 可执行 JAR
find_jar() {
    local candidates=()
    local file

    for file in "$SCRIPT_DIR"/target/*.jar; do
        [ -f "$file" ] || continue

        case "$file" in
            *-sources.jar|*-javadoc.jar|*-tests.jar|*/original-*.jar)
                continue
                ;;
        esac

        candidates+=("$file")
    done

    if [ "${#candidates[@]}" -eq 0 ]; then
        echo "错误：target 目录中没有找到可部署的 JAR"
        exit 1
    fi

    if [ "${#candidates[@]}" -gt 1 ]; then
        echo "错误：找到多个候选 JAR，请清理 target 后重新构建："
        printf '  %s\n' "${candidates[@]}"
        exit 1
    fi

    printf '%s\n' "${candidates[0]}"
}

# 执行 Maven 打包
build_project() {
    echo
    echo "========== 1. Maven 打包 =========="

    cd "$SCRIPT_DIR"

    if [ -x "$SCRIPT_DIR/mvnw" ]; then
        MVN_CMD=("$SCRIPT_DIR/mvnw")
    elif command -v mvn >/dev/null 2>&1; then
        MVN_CMD=("$(command -v mvn)")
    else
        echo "错误：没有找到 Maven 或项目 mvnw"
        echo "请安装 Maven，或使用项目自带的 Maven Wrapper"
        exit 1
    fi

    # MAVEN_ARGS 作为空格分隔的参数列表
    local args=()
    if [ -n "$MAVEN_ARGS" ]; then
        # 配置文件中的 MAVEN_ARGS 应为可信参数，不要放不可信输入
        read -r -a args <<< "$MAVEN_ARGS"
    fi

    "${MVN_CMD[@]}" clean package "${args[@]}"

    JAR_FILE="$(find_jar)"

    if [ ! -s "$JAR_FILE" ]; then
        echo "错误：JAR 文件不存在或为空：$JAR_FILE"
        exit 1
    fi

    echo "构建成功：$JAR_FILE"
}

# 检查 SSH 连接及服务器 Java
check_server() {
    echo
    echo "========== 检查服务器 =========="

    ssh_run "set -e; command -v java; java -version 2>&1; systemctl cat '$SERVICE_NAME' >/dev/null"

    echo "SSH 连接、Java 和 systemd 服务检查通过"
}

# 上传 JAR 到临时文件，再在服务器端执行原子替换
deploy_project() {
    local stamp remote_tmp

    stamp="$(date '+%Y%m%d_%H%M%S')"
    remote_tmp="/tmp/restart-server-${stamp}-$$.jar"

    echo
    echo "========== 2. 检查服务器 =========="
    check_server

    echo
    echo "========== 3. 上传 JAR =========="
    echo "目标：${REMOTE}:${remote_tmp}"

    scp "${SCP_OPTIONS[@]}" "$JAR_FILE" "${REMOTE}:${remote_tmp}"

    echo
    echo "========== 4. 备份、部署、重启与验证 =========="

    # 参数仅来自本地配置文件；目录、服务名应使用普通安全字符。
    ssh_run "sudo bash -s -- '$remote_tmp' '$SERVER_DIR' '$SERVICE_NAME' '$KEEP_BACKUPS'" <<'REMOTE_SCRIPT'
set -Eeuo pipefail

REMOTE_TMP="$1"
SERVER_DIR="$2"
SERVICE_NAME="$3"
KEEP_BACKUPS="$4"

BACKUP_DIR="${SERVER_DIR}/backups"
CURRENT_JAR="${SERVER_DIR}/restart-server.jar"
STAMP="$(date '+%Y%m%d_%H%M%S')"
BACKUP_JAR="${BACKUP_DIR}/restart-server-${STAMP}.jar"
HAD_OLD=0
DEPLOY_OK=0

cleanup() {
    rm -f "$REMOTE_TMP" || true
}
trap cleanup EXIT

rollback_on_error() {
    local code=$?
    if [ "$DEPLOY_OK" -eq 1 ]; then
        return
    fi

    echo "部署或启动验证失败，尝试回滚……"

    if [ "$HAD_OLD" -eq 1 ] && [ -s "$BACKUP_JAR" ]; then
        cp -f "$BACKUP_JAR" "${CURRENT_JAR}.rollback"
        mv -f "${CURRENT_JAR}.rollback" "$CURRENT_JAR"
        chmod 644 "$CURRENT_JAR"

        if systemctl restart "$SERVICE_NAME"; then
            echo "旧版本已恢复，服务重启命令已执行"
        else
            echo "严重错误：旧版本已恢复，但服务重启失败"
            systemctl status "$SERVICE_NAME" --no-pager || true
        fi
    else
        echo "没有可用的旧版本，无法自动回滚"
    fi

    systemctl status "$SERVICE_NAME" --no-pager || true
    journalctl -u "$SERVICE_NAME" -n 100 --no-pager || true
    exit "$code"
}
trap rollback_on_error ERR

if [ ! -s "$REMOTE_TMP" ]; then
    echo "上传文件不存在或为空：$REMOTE_TMP"
    exit 1
fi

mkdir -p "$SERVER_DIR" "$BACKUP_DIR"

if [ -f "$CURRENT_JAR" ]; then
    cp -p "$CURRENT_JAR" "$BACKUP_JAR"
    HAD_OLD=1
    echo "旧版本已备份：$BACKUP_JAR"
fi

# 先复制到同目录临时文件，再通过 mv 替换，避免半截 JAR 被启动
install -m 644 "$REMOTE_TMP" "${CURRENT_JAR}.new"
mv -f "${CURRENT_JAR}.new" "$CURRENT_JAR"
rm -f "$REMOTE_TMP"

echo "新版本已放置：$CURRENT_JAR"

systemctl restart "$SERVICE_NAME"

# 等待服务进入 active 状态，最长约 30 秒
ACTIVE=0
for i in $(seq 1 15); do
    sleep 2
    if systemctl is-active --quiet "$SERVICE_NAME"; then
        ACTIVE=1
        break
    fi
done

if [ "$ACTIVE" -ne 1 ]; then
    echo "服务在等待期内没有进入 active 状态"
    systemctl status "$SERVICE_NAME" --no-pager || true
    journalctl -u "$SERVICE_NAME" -n 100 --no-pager || true
    exit 1
fi

# 再等待几秒，尽量发现启动后立即退出的情况
sleep 3
if ! systemctl is-active --quiet "$SERVICE_NAME"; then
    echo "服务启动后退出"
    systemctl status "$SERVICE_NAME" --no-pager || true
    journalctl -u "$SERVICE_NAME" -n 100 --no-pager || true
    exit 1
fi

DEPLOY_OK=1
trap - ERR

echo "部署成功！"
systemctl status "$SERVICE_NAME" --no-pager

# 清理过期备份，保留最近 KEEP_BACKUPS 个版本
if [[ "$KEEP_BACKUPS" =~ ^[0-9]+$ ]]; then
    mapfile -t backups < <(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'restart-server-*.jar' -printf '%T@ %p\n' | sort -nr | awk '{ $1=""; sub(/^ /, ""); print }')
    count=0
    for file in "${backups[@]}"; do
        count=$((count + 1))
        if [ "$count" -gt "$KEEP_BACKUPS" ]; then
            rm -f -- "$file"
        fi
    done
fi

echo
echo "最近服务日志："
journalctl -u "$SERVICE_NAME" -n 40 --no-pager || true
REMOTE_SCRIPT

    echo
    echo "发布流程执行完成。"
}

show_status() {
    ssh_run "sudo systemctl status '$SERVICE_NAME' --no-pager"
}

restart_service() {
    ssh_run "sudo systemctl restart '$SERVICE_NAME' && sleep 2 && sudo systemctl status '$SERVICE_NAME' --no-pager"
}

show_logs() {
    ssh_run "sudo journalctl -u '$SERVICE_NAME' -n 100 -f"
}

rollback_service() {
    echo "警告：即将回滚到最近的备份版本。"
    printf "输入 YES 确认："
    read -r answer
    if [ "$answer" != "YES" ]; then
        echo "已取消"
        exit 0
    fi

    ssh_run "sudo bash -s -- '$SERVER_DIR' '$SERVICE_NAME'" <<'REMOTE_ROLLBACK'
set -Eeuo pipefail

SERVER_DIR="$1"
SERVICE_NAME="$2"
BACKUP_DIR="${SERVER_DIR}/backups"
CURRENT_JAR="${SERVER_DIR}/restart-server.jar"

BACKUP_JAR="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'restart-server-*.jar' -printf '%T@ %p\n' | sort -nr | head -n 1 | cut -d' ' -f2-)"

if [ -z "$BACKUP_JAR" ] || [ ! -s "$BACKUP_JAR" ]; then
    echo "错误：没有找到可用的备份 JAR"
    exit 1
fi

STAMP="$(date '+%Y%m%d_%H%M%S')"
if [ -f "$CURRENT_JAR" ]; then
    cp -p "$CURRENT_JAR" "${BACKUP_DIR}/restart-server-before-rollback-${STAMP}.jar"
fi

cp -f "$BACKUP_JAR" "${CURRENT_JAR}.rollback"
chmod 644 "${CURRENT_JAR}.rollback"
mv -f "${CURRENT_JAR}.rollback" "$CURRENT_JAR"

systemctl restart "$SERVICE_NAME"
sleep 3

if ! systemctl is-active --quiet "$SERVICE_NAME"; then
    echo "回滚后服务未正常运行"
    systemctl status "$SERVICE_NAME" --no-pager || true
    journalctl -u "$SERVICE_NAME" -n 100 --no-pager || true
    exit 1
fi

echo "已回滚至：$BACKUP_JAR"
systemctl status "$SERVICE_NAME" --no-pager
REMOTE_ROLLBACK
}

COMMAND="${1:-deploy}"

case "$COMMAND" in
    deploy)
        build_project
        deploy_project
        ;;
    status)
        show_status
        ;;
    restart)
        restart_service
        ;;
    logs)
        show_logs
        ;;
    rollback)
        rollback_service
        ;;
    -h|--help|help)
        show_help
        ;;
    *)
        echo "未知命令：$COMMAND"
        show_help
        exit 1
        ;;
esac
