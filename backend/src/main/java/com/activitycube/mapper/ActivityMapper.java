package com.activitycube.mapper;

import com.activitycube.entity.Activity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ActivityMapper extends BaseMapper<Activity> {
    @Update("""
            UPDATE activity
            SET registered_count = registered_count + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{activityId}
              AND (
                  max_participants IS NULL
                  OR max_participants <= 0
                  OR registered_count < max_participants
              )
            """)
    int tryReserveRegistrationSlot(@Param("activityId") Long activityId);

    @Update("""
            UPDATE activity
            SET registered_count = registered_count - 1,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{activityId}
              AND registered_count > 0
            """)
    int releaseRegistrationSlot(@Param("activityId") Long activityId);
}
