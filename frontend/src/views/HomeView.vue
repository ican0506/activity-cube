<template>
  <section>
    <div class="hero">
      <div>
        <h1>活动魔方</h1>
        <p>面向校园活动组织的一站式轻工具，覆盖活动发布、报名收集、二维码签到和数据复盘。</p>
        <div class="hero-actions">
          <RouterLink to="/activities">
            <el-button class="hero-button">进入活动大厅</el-button>
          </RouterLink>
        </div>
      </div>
      <div class="hero-card">
        <span>校园微服务</span>
        <strong>轻量闭环</strong>
        <p>创建、报名、签到、统计一条线完成</p>
      </div>
    </div>

    <div class="metric-row">
      <div class="metric metric-accent">
        <span>组织流程</span>
        <strong>创建 → 报名 → 签到</strong>
      </div>
      <div class="metric metric-accent">
        <span>校区支持</span>
        <strong>3 校区 + 线上</strong>
      </div>
      <div class="metric">
        <span>演示账号</span>
        <strong>5 个预置用户</strong>
      </div>
    </div>

    <div class="panel">
      <el-steps :active="4" finish-status="success" align-center>
        <el-step title="负责人创建活动" />
        <el-step title="学生报名" />
        <el-step title="扫码签到" />
        <el-step title="后台统计" />
      </el-steps>
    </div>
  </section>
</template>
