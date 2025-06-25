package api.exchange.dtos.Requset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordRequest {
    private String oldPassword;
    private String newPassword;
    private String oldLv2Password;
    private String newLv2Password;
}
