package cn.ccnu.roombooking.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RoomRepository extends JpaRepository<Room, UUID> { Optional<Room> findByRoomNo(String roomNo); }
