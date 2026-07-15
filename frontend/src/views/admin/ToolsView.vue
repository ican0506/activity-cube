<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">抽签分组</h1>
        <p class="page-subtitle">从报名名单或签到名单中随机抽取、随机分组。</p>
      </div>
    </div>
    <div class="grid" style="grid-template-columns: 1fr 1fr">
      <div class="panel">
        <h3>随机抽签</h3>
        <div class="toolbar">
          <el-select v-model="drawForm.source" style="width: 160px">
            <el-option label="报名名单" value="REGISTRATION" />
            <el-option label="已签到名单" value="CHECKIN" />
          </el-select>
          <el-input-number v-model="drawForm.count" :min="1" />
          <el-button type="primary" @click="runDraw">抽取</el-button>
        </div>
        <el-table :data="drawRows" stripe>
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="studentNo" label="学号" />
          <el-table-column prop="campus" label="校区" />
        </el-table>
      </div>

      <div class="panel">
        <h3>随机分组</h3>
        <div class="toolbar">
          <el-select v-model="groupForm.source" style="width: 150px">
            <el-option label="报名名单" value="REGISTRATION" />
            <el-option label="已签到名单" value="CHECKIN" />
          </el-select>
          <el-select v-model="groupForm.mode" style="width: 150px">
            <el-option label="按组数" value="BY_GROUP_COUNT" />
            <el-option label="按每组人数" value="BY_GROUP_SIZE" />
          </el-select>
          <el-input-number v-if="groupForm.mode === 'BY_GROUP_COUNT'" v-model="groupForm.groupCount" :min="1" />
          <el-input-number v-else v-model="groupForm.groupSize" :min="1" />
          <el-button type="primary" @click="runGroup">分组</el-button>
        </div>
        <div class="grid">
          <div v-for="(groupItem, index) in groupRows" :key="index" class="activity-card">
            <h3>第 {{ index + 1 }} 组</h3>
            <p v-for="member in groupItem" :key="member.id">{{ member.name }} · {{ member.studentNo }}</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { draw, group } from '../../api/tool'

const route = useRoute()
const drawRows = ref([])
const groupRows = ref([])
const drawForm = reactive({ source: 'REGISTRATION', count: 3 })
const groupForm = reactive({ source: 'REGISTRATION', mode: 'BY_GROUP_COUNT', groupCount: 4, groupSize: 5 })

async function runDraw() {
  drawRows.value = await draw(route.params.id, drawForm)
}

async function runGroup() {
  groupRows.value = await group(route.params.id, groupForm)
}
</script>
