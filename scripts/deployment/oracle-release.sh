#!/usr/bin/env bash
set -Eeuo pipefail

fail() {
    printf '%s\n' "$*" >&2
    exit 1
}

[[ "$EUID" -eq 0 ]] || fail 'Chạy script này bằng sudo.'
[[ "$#" -eq 2 ]] || fail 'Cách dùng: dnrestaurant-release deploy|rollback YYYYMMDD-HHMMSS-xxxxxxxx'
action="$1"
release_id="$2"
[[ "$action" == deploy || "$action" == rollback ]] || fail 'Thao tác không hợp lệ.'
[[ "$release_id" =~ ^[0-9]{8}-[0-9]{6}-[0-9a-f]{8}$ ]] || fail 'Mã bản triển khai không hợp lệ.'

base_dir=/srv/dnrestaurant
release_root="$base_dir/releases"
target_dir="$release_root/$release_id"
[[ -d "$release_root" ]] || fail 'Tạo /srv/dnrestaurant/releases trước theo guide triển khai.'
[[ -f /etc/systemd/system/dnrestaurant.service ]] || fail 'Cài dnrestaurant.service trước.'
[[ -f /etc/dnrestaurant/backend.env ]] || fail 'Tạo /etc/dnrestaurant/backend.env trước.'
[[ ! -e "$base_dir/current" || -L "$base_dir/current" ]] || fail 'current phải là symlink.'
[[ ! -e "$base_dir/previous" || -L "$base_dir/previous" ]] || fail 'previous phải là symlink.'

exec 9>/run/lock/dnrestaurant-deploy.lock
flock -n 9 || fail 'Một lượt triển khai khác đang chạy.'

old_dir=''
if [[ -L "$base_dir/current" ]]; then
    old_dir="$(readlink -f -- "$base_dir/current")"
    [[ "$old_dir" == "$release_root/"* && -f "$old_dir/app.jar" ]] || fail 'Bản triển khai hiện tại không hợp lệ.'
fi
[[ "$target_dir" != "$old_dir" ]] || fail 'Bản triển khai này đang được sử dụng.'

if [[ "$action" == deploy ]]; then
    upload_dir="$(dirname -- "$(readlink -f -- "$0")")"
    [[ -f "$upload_dir/release.tar.gz" && -f "$upload_dir/release.tar.gz.sha256" ]] || fail 'Thiếu gói upload hoặc file checksum.'
    [[ ! -e "$target_dir" ]] || fail 'Thư mục bản triển khai đã tồn tại. Dùng mã triển khai mới.'
    (cd -- "$upload_dir" && sha256sum --check release.tar.gz.sha256)
    install -d -m 0755 "$target_dir"
    tar --extract --gzip --no-same-owner --no-same-permissions \
        --file "$upload_dir/release.tar.gz" --directory "$target_dir"
    [[ -f "$target_dir/app.jar" && -f "$target_dir/web/index.html" ]] || fail 'Gói upload phải chứa app.jar và web/index.html.'
    chown -R root:root "$target_dir"
    find "$target_dir" -type d -exec chmod 0755 {} +
    find "$target_dir" -type f -exec chmod 0644 {} +
else
    [[ -d "$target_dir" && ! -L "$target_dir" ]] || fail 'Đích rollback phải là thư mục bản triển khai.'
    [[ -f "$target_dir/app.jar" && -f "$target_dir/web/index.html" ]] || fail 'Bản rollback thiếu file ứng dụng.'
fi

switch_link() {
    local destination="$1"
    local link_name="$2"
    local temp_link="$base_dir/.${link_name}.$BASHPID"
    ln -s -- "$destination" "$temp_link"
    mv -Tf -- "$temp_link" "$base_dir/$link_name"
}

wait_for_backend() {
    local deadline=$((SECONDS + 180))
    local response_code=''
    while (( SECONDS < deadline )); do
        response_code="$(curl --silent --output /dev/null --max-time 3 \
            --write-out '%{http_code}' http://127.0.0.1:8080/api/auth/me || true)"
        if [[ "$response_code" == 401 ]] && systemctl is-active --quiet dnrestaurant; then
            return 0
        fi
        sleep 2
    done
    return 1
}

switch_link "$target_dir" current
if systemctl restart dnrestaurant && wait_for_backend; then
    if [[ -n "$old_dir" ]]; then
        switch_link "$old_dir" previous
    fi
    if [[ "$action" == deploy ]]; then
        install -o root -g root -m 0755 "$upload_dir/oracle-release.sh" /usr/local/sbin/dnrestaurant-release
    fi
    printf 'Bản đang chạy: %s\n' "$release_id"
    printf 'Kiểm tra khởi động: /api/auth/me trả 401 đúng kỳ vọng khi chưa có JWT.\n'
else
    printf 'Backend không khởi động thành công. Xem log: sudo journalctl -u dnrestaurant -n 100 --no-pager\n' >&2
    if [[ -n "$old_dir" ]]; then
        switch_link "$old_dir" current
        if systemctl restart dnrestaurant && wait_for_backend; then
            printf 'Đã khôi phục bản ứng dụng trước: %s\n' "${old_dir##*/}" >&2
        else
            printf 'Bản ứng dụng trước cũng không khởi động được; kiểm tra log backend.\n' >&2
        fi
    else
        systemctl stop dnrestaurant || true
        unlink -- "$base_dir/current"
        printf 'Chưa có bản triển khai trước. Đã dừng backend; sửa lỗi rồi triển khai lại.\n' >&2
    fi
    fail 'Triển khai thất bại. Schema và dữ liệu database không được rollback.'
fi

