package api.exchange.dtos.Response;

import api.exchange.models.User.KycStatus;
import api.exchange.models.User.UserLevel;
import api.exchange.models.User.UserStatus;
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
public class UserInfoResponse {
    private String uid;
    private String email;
    private String username;
    private String nation;
    private KycStatus kycStatus;
    private String phone ;
    private UserLevel userLevel;
    private UserStatus userStatus;
}
