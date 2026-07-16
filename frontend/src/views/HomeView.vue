<template>
  <section>
    <div class="hero campus-orbit">
      <div>
        <h1>活动魔方</h1>
        <p>面向河南农业大学校园活动组织的一站式轻工具平台，覆盖活动发布、报名收集、二维码签到、名单导出、随机抽签分组和反馈复盘。</p>
        <div class="hero-actions">
          <RouterLink to="/activities">
            <el-button class="hero-button" :icon="Grid">进入活动大厅</el-button>
          </RouterLink>
          <RouterLink to="/my/registrations">
            <el-button class="hero-button" plain :icon="Tickets">我的报名</el-button>
          </RouterLink>
        </div>
      </div>
      <div class="hero-card">
        <span>校园微服务</span>
        <strong>轻量闭环</strong>
        <p>创建、报名、签到、统计、反馈一条线完成。</p>
        <div class="hero-stat-grid">
          <div class="hero-stat"><b>3</b><small>实体校区</small></div>
          <div class="hero-stat"><b>H5</b><small>手机端适配</small></div>
        </div>
      </div>
    </div>

    <div class="metric-row">
      <div class="metric metric-accent">
        <span>组织流程</span>
        <strong>创建 · 报名 · 签到</strong>
      </div>
      <div class="metric metric-accent">
        <span>活动工具</span>
        <strong>二维码 · 抽签 · 分组</strong>
      </div>
      <div class="metric">
        <span>数据复盘</span>
        <strong>统计 · Excel · 反馈</strong>
      </div>
    </div>

    <div class="panel">
      <el-steps :active="4" finish-status="success" align-center>
        <el-step title="负责人创建活动" />
        <el-step title="学生扫码报名" />
        <el-step title="现场二维码签到" />
        <el-step title="后台统计复盘" />
      </el-steps>
    </div>
  </section>
</template>

<script setup>
import { Grid, Tickets } from '@element-plus/icons-vue'
</script>
