# Frontend — Quản lý quán ăn

React + TypeScript + Vite, dùng **Bun**; Ant Design, TanStack Query, React Router,
Axios, dayjs và @microsoft/fetch-event-source. Không thêm UI/state framework mới.

```powershell
bun install
bun run dev
bun run test
bun run build
bun run lint
bun run preview
```

Vite http://localhost:5173 proxy `/api` → backend8080. Ant Design/dayjs dùng tiếng Việt.
Build gồm type-check. Giữ bun.lock trong Git; không đưa credentials/secret vào frontend.

## Route và phân quyền

| Route | Quyền UI | Hiện trạng |
|---|---|---|
| `/accounts` | OWNER | Tìm tên/SĐT không dấu; lọc vai trò/trạng thái; quản lý tài khoản |
| `/menu` | OWNER, HEAD_CHEF | Thực đơn/định lượng, thao tác theo role; món mới nhất trước, đang bán lên trước |
| `/floor-plan` | WAITER, MANAGER, CASHIER, OWNER | Sơ đồ bàn/mở phiên đúng quyền; OWNER thêm nút **Quản lý khu vực và bàn** (Drawer) |
| `/reservations` | MANAGER, WAITER, OWNER | Đặt bàn/tra cứu, thao tác theo role |
| `/sessions/:id` | WAITER, MANAGER | Chi tiết/gọi món; CASHIER dùng payment riêng |
| `/kitchen` | KITCHEN_STAFF, HEAD_CHEF | Queue/trạng thái món |
| `/awaiting-service` | WAITER | Redirect /floor-plan; notifications on table cards |
| `/payment` | CASHIER | Danh sách/preview/payment |
| `/invoices` | CASHIER, MANAGER, OWNER | Lịch sử hóa đơn |
| `/inventory` | OWNER, HEAD_CHEF | Tồn/danh mục/lô/biến động, thao tác theo role |
| `/inventory/receipts`, `/inventory/stocktake` | HEAD_CHEF | Nhập/kiểm kê |
| `/reconciliation` | CASHIER, MANAGER | Lập/preview/list/confirm theo role |
| `/reports` | OWNER | Doanh thu + kho hiện tại, OWNER landing |

Phân quyền trên giao diện chỉ để điều hướng; quyền thực sự được kiểm ở backend.

## Quy ước cần giữ

- api/types theo domain, page lazy trong router.tsx, navigation/landing ở auth/roles.ts.
- labels.ts + StatusTag, tiền qua MoneyText, time.ts theo Asia/Ho_Chi_Minh.
- useApiFeedback cho message/fieldErrors; giữ cơ chế stale401/403 và không retry4xx.
- PageResponse chỉ có một định nghĩa trong types/tables.ts; inventoryApi không import type từ paymentApi.
- Giảm chuyển động dùng `transition: none`; không duration dương global trên left/top popup.
- Recipe giữ dòng inactive đã có; lookup có status filter. Walk-in độc lập query reservation;
  picker đặt trước dùng tableId/unexpired=true/status=CONFIRMED phía server.
- Stocktake actual ban đầu trống, chỉ gửi dòng đã đếm. Xóa ô bỏ dòng, 0 chỉ gửi khi nhập0.
  Ghi chú đơn thuần không đánh dấu đã đếm; server tính lại systemQuantity, UI chỉ tính tạm.
  Nút Khớp tồn hệ thống từng dòng điền số đang hiển thị sau thao tác người dùng; không sao chép tồn âm.
- Payment không gửi amount; PENDING/COOKING/READY chặn, SERVED/UNAVAILABLE/CANCELLED hợp lệ; giữ phiên 0đ. Invoice/stock nghiệp vụ ở backend.

## Query keys và realtime

| Nhóm | Prefix hiện có |
|---|---|
| Danh mục | `['accounts']`, `['menu-items']`, `['ingredients']` (recipe lookup) |
| Bán hàng live | `['floor-plan']`, `['reservations']`, `['session', numericId]`, `['kitchen']`, `['awaiting-service']`, `['available-tables', params]` |
| Quản lý khu vực/bàn (OWNER) | `['floor-plan','management','areas']`, `['floor-plan','management','tables']` (nằm dưới prefix `floor-plan`) |
| Thanh toán/lịch sử | `['payment','open-sessions']`, `['payment','preview',id]`, `['invoices',filters]` |
| Kho | `['inventory','ingredients',filters]`, `ingredients-active`, `ingredients-lookup`, `lots-attention`, `movements`, `receipts-list`, `stocktakes-list`, `stocktake-template` dưới prefix inventory |

FLOOR_PLAN invalidate floor-plan/payment/session/awaiting-service; khi event không có `sessionId` (đổi khu vực/bàn) còn invalidate
`session`, `reservations`, `available-tables`, `invoices`. ORDER invalidate
kitchen/session/awaiting-service/payment/inventory; RESERVATION invalidate reservations/floor-plan.
useRealtime gắn một lần trong AppLayout, bearer header, cleanup theo token,
invalidate toàn bộ live prefixes khi reconnect. Live screens polling30s/focus;
inventory nhận invalidation ORDER khi giữ/giải phóng/tiêu hao, danh sách tồn polling30s. Mutation kho invalidate prefix inventory; payment invalidate các key liên quan.
