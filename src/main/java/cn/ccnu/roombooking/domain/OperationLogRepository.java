package cn.ccnu.roombooking.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OperationLogRepository extends JpaRepository<OperationLog, UUID> { List<OperationLog> findTop100ByOrderByCreatedAtDesc(); }
