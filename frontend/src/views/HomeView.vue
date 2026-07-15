<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">活动魔方</h1>
        <p class="page-subtitle">校园活动创建、报名二维码、签到二维码、名单统计和导出的 MVP 工具平台。</p>
      </div>
      <RouterLink to="/activities">
        <el-button type="primary">进入活动大厅</el-button>
      </RouterLink>
    </div>

    <div class="metric-row">
      <div class="metric">
        <span>组织流程</span>
        <strong>创建 → 报名 → 签到</strong>
      </div>
      <div class="metric">
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
