package api.exchange.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import api.exchange.models.User;
import api.exchange.models.UserDevice;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserAndDeviceId(User user, String deviceId);

    Optional<UserDevice> findByDeviceId(String deviceId);

    Optional<UserDevice> findByIpAddressAndBrowserNameAndUser_UidAndIsActive(String ipAdress, String browserName,
            String i, boolean isActive);

    List<UserDevice> findByUser_UidAndIsActive(String userId, Boolean isActive);

}
