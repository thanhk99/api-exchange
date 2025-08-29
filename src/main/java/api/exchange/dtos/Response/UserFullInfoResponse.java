package api.exchange.dtos.Response;

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
    private boolean isVerified;
    private String nation;
    private boolean isActive;
    private String phoneNumber;
    private String leveFee ;
    
}
