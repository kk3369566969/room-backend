package cn.ccnu.roombooking.mapper;

import cn.ccnu.roombooking.entity.Reservation;
import cn.ccnu.roombooking.enums.ReservationStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

/**
 * 预约 Mapper。
 *
 * <p>单表增删改查由 {@link BaseMapper} 提供；只有「查询并加行锁」需要手写 SQL，
 * 定义见 {@code resources/mapper/ReservationMapper.xml}。
 */
public interface ReservationMapper extends BaseMapper<Reservation> {

  /**
   * 查询与指定时段重叠的预约，并对结果集加悲观写锁，防止并发超订。
   */
  List<Reservation> selectOverlappingForUpdate(@Param("roomId") UUID roomId,
                                               @Param("date") LocalDate date,
                                               @Param("startMinute") int startMinute,
                                               @Param("endMinute") int endMinute,
                                               @Param("statuses") Collection<ReservationStatus> statuses);
}
