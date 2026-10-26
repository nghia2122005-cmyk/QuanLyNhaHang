package vn.ueh.kltn.restaurant.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Số điện thoại hoặc mật khẩu không đúng"),
    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "Tài khoản đã bị khóa"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu yêu cầu"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "Không thể trả dữ liệu theo định dạng được yêu cầu"),
    UNSUPPORTED_MEDIA_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng nội dung gửi lên không được hỗ trợ"),
    INVALID_STATE(HttpStatus.CONFLICT, "Trạng thái hiện tại không cho phép thao tác này"),
    DATA_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc không thỏa mãn ràng buộc nghiệp vụ"),
    PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng"),
    CANNOT_MODIFY_SELF(
            HttpStatus.CONFLICT, "Bạn không thể khóa, mở khóa hoặc đổi vai trò của chính mình"),
    RESERVATION_OVERLAP(HttpStatus.CONFLICT, "Bàn đã có đặt trước trùng khung giờ"),
    TABLE_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "Số khách vượt quá sức chứa của bàn"),
    TABLE_INACTIVE(HttpStatus.CONFLICT, "Bàn đã ngừng sử dụng"),
    TABLE_NOT_AVAILABLE(HttpStatus.CONFLICT, "Bàn không ở trạng thái trống"),
    SESSION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "Bàn đã có phiên đang hoạt động"),
    SESSION_NOT_ACTIVE(HttpStatus.CONFLICT, "Phiên phục vụ không ở trạng thái đang hoạt động"),
    SESSION_ALREADY_PAID(HttpStatus.CONFLICT, "Phiên phục vụ đã được thanh toán"),
    PAYMENT_BLOCKED_PENDING_ITEMS(
            HttpStatus.BAD_REQUEST,
            "Không thể thanh toán khi còn món chưa phục vụ (chờ chế biến, đang nấu hoặc hoàn"
                    + " thành)"),
    UNIT_CHANGE_BLOCKED(
            HttpStatus.CONFLICT,
            "Không thể thay đổi đơn vị tính của nguyên vật liệu đã có biến động kho"),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "Số lượng tồn kho không đủ để xuất hủy"),
    INVALID_DISPOSAL(HttpStatus.BAD_REQUEST, "Số lượng xuất hủy phải lớn hơn 0"),
    INVALID_STOCKTAKE(HttpStatus.BAD_REQUEST, "Dữ liệu kiểm kê không hợp lệ"),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "Số lượng không hợp lệ"),
    EXPIRY_DATE_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "Nguyên vật liệu có theo dõi hạn sử dụng bắt buộc phải nhập hạn sử dụng"),
    LOT_INGREDIENT_MISMATCH(
            HttpStatus.BAD_REQUEST, "Lô nguyên vật liệu không thuộc về nguyên vật liệu đã chọn"),
    RESERVATION_NOT_CONFIRMED(HttpStatus.CONFLICT, "Đặt bàn không còn ở trạng thái đã xác nhận"),
    RESERVATION_TABLE_MISMATCH(HttpStatus.CONFLICT, "Đặt bàn không thuộc bàn đã chọn"),
    RESERVATION_EXPIRED(HttpStatus.CONFLICT, "Đặt bàn đã quá giờ kết thúc"),
    DUPLICATE_RECIPE_INGREDIENT(
            HttpStatus.BAD_REQUEST, "Nguyên vật liệu bị trùng trong định lượng"),
    INGREDIENT_INACTIVE(HttpStatus.BAD_REQUEST, "Nguyên vật liệu đã ngừng sử dụng"),
    LOT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy lô nguyên vật liệu"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp sự cố, vui lòng thử lại sau");

    private final HttpStatus status;
    private final String message;
}
