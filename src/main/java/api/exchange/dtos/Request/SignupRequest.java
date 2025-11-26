package api.exchange.dtos.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class SignupRequest {
    private String username;

    private String email;

    private String password;

    private String nation;
}