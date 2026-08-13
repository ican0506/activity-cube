package com.activitycube.mapper;

import com.activitycube.entity.Feedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FeedbackMapper extends BaseMapper<Feedback> {
    @Select("""
            <script>
            SELECT DISTINCT activity_id
            FROM feedback
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
