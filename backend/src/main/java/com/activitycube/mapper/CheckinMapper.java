package com.activitycube.mapper;

import com.activitycube.entity.Checkin;
import com.activitycube.vo.ActivityCountVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CheckinMapper extends BaseMapper<Checkin> {
    @Select("""
            <script>
            SELECT activity_id AS activityId, COUNT(*) AS count
            FROM checkin
            WHERE activity_id IN
            <foreach collection="activityIds" item="activityId" open="(" separator="," close=")">
                #{activityId}
            </foreach>
            GROUP BY activity_id
            </script>
            """)
    List<ActivityCountVO> countByActivityIds(@Param("activityIds") List<Long> activityIds);

    @Select("""
            <script>
            SELECT DISTINCT activity_id
            FROM checkin
            WHERE user_id = #{userId}
              AND activity_id IN
            <foreach collection="activityIds" item="activityId" open="(" separator="," close=")">
                #{activityId}
            </foreach>
            </script>
            """)
    List<Long> findActivityIdsByUserAndActivityIds(@Param("userId") Long userId,
                                                   @Param("activityIds") List<Long> activityIds);
}
