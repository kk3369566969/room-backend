package cn.ccnu.roombooking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 会议室 */
@Getter
@Setter
@NoArgsConstructor
@TableName("room")
public class Room {

  @TableId(type = IdType.INPUT)
  private UUID id = UUID.randomUUID();

  private String roomNo;
  private String location;
  private Integer capacity;
  private String equipment;
  private String restrictionText;
  private String infoStatus;
  private LocalTime openTime;
  private LocalTime closeTime;
}
