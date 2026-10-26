package vn.ueh.kltn.restaurant.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app")
public record AppProperties(@NotBlank String timezone, boolean seed, @Valid @NotNull Jwt jwt,
                            @Valid @NotNull Alert alert, @Valid @NotNull Reservation reservation) {
    public record Jwt(@NotBlank @Size(min = 32)
                      @Pattern(regexp = "^(?!\\s*<)[\\s\\S]*$", message = "JWT secret phải được thay bằng giá trị riêng, không dùng placeholder") String secret,
                      @Min(1) long expirationHours) {
        @Override
        public String toString() { return "Jwt[secret=REDACTED, expirationHours=" + expirationHours + "]"; }
    }
    public record Alert(@Min(0) int expiryDays) { }
    public record Reservation(@Min(0) int upcomingWarningMinutes) { }
}
