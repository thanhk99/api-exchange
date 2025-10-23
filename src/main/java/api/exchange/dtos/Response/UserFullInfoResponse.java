package api.exchange.dtos.Response;

import api.exchange.models.User.KycStatus;
import api.exchange.models.User.UserLevel;
import api.exchange.models.User.UserStatus;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserFullInfoResponse {
    
    private String uid;
    private String email;
    private String username;
    private UserStatus userStatus;
    private String nation;
    private KycStatus kycStatus;
    private UserLevel userLevel;
    private String phoneNumber;
    private String leveFee ;
    
}
