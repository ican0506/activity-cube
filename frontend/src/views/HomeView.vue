<template>
  <section>
    <div class="lite-page-head">
      <div>
        <span class="section-eyebrow">活动魔方</span>
        <h1>活动魔方</h1>
        <p>校园活动的报名、签到、统计和反馈工具。</p>
      </div>
      <div class="button-row">
        <RouterLink to="/activities">
          <el-button type="primary" :icon="Grid">进入活动大厅</el-button>
        </RouterLink>
        <RouterLink to="/my/registrations">
          <el-button :icon="Tickets">我的报名</el-button>
        </RouterLink>
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
