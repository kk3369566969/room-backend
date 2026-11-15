package cn.ccnu.roombooking.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByExternalId(String externalId);
  Optional<User> findByEmployeeNo(String employeeNo);
}
