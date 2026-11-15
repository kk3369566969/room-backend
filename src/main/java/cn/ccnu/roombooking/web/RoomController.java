package cn.ccnu.roombooking.web;
import cn.ccnu.roombooking.common.*;
import cn.ccnu.roombooking.domain.Room;
import cn.ccnu.roombooking.service.RoomService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/v1/rooms")
public class RoomController {
  private final RoomService service; public RoomController(RoomService service) { this.service=service; }
  @GetMapping List<ApiDtos.RoomResponse> list() { return service.list().stream().map(this::room).toList(); }
  @GetMapping("/{roomNo}") ApiDtos.RoomResponse get(@PathVariable String roomNo) { return room(service.room(roomNo)); }
  @GetMapping("/{roomNo}/schedule") List<ApiDtos.ScheduleItem> schedule(@PathVariable String roomNo,@RequestParam LocalDate date) { return service.schedule(roomNo,date); }
  private ApiDtos.RoomResponse room(Room r) { return new ApiDtos.RoomResponse(r.getRoomNo(),r.getLocation(),r.getCapacity(),r.getEquipment(),r.getRestrictionText(),r.getInfoStatus(),r.getOpenTime(),r.getCloseTime()); }
}
