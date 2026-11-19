package cn.ccnu.roombooking.controller;

import cn.ccnu.roombooking.dto.response.RoomResponse;
import cn.ccnu.roombooking.dto.response.ScheduleItem;
import cn.ccnu.roombooking.service.RoomService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

  private final RoomService roomService;

  @GetMapping
  public List<RoomResponse> list() {
    return roomService.list();
  }

  @GetMapping("/{roomNo}")
  public RoomResponse get(@PathVariable String roomNo) {
    return roomService.get(roomNo);
  }

  @GetMapping("/{roomNo}/schedule")
  public List<ScheduleItem> schedule(@PathVariable String roomNo, @RequestParam LocalDate date) {
    return roomService.schedule(roomNo, date);
  }
}
