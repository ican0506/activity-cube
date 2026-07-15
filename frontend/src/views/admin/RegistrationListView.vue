<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">报名名单</h1>
        <p class="page-subtitle">查看报名数据，并导出 Excel 名单。</p>
      </div>
      <el-button type="primary" @click="download">导出报名名单</el-button>
    </div>
    <div class="panel" v-loading="loading">
      <el-table :data="rows" stripe>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="studentNo" label="学号" />
        <el-table-column prop="college" label="学院" />
        <el-table-column prop="majorClass" label="专业班级" />
        <el-table-column prop="campus" label="校区" />
        <el-table-column prop="phone" label="手机号" />
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivity } from '../../api/activity'
import { listRegistrations } from '../../api/registration'
import { exportRosterExcel } from '../../utils/excelExport'

const route = useRoute()
const rows = ref([])
const activity = ref(null)
const loading = ref(false)

function download() {
  exportRosterExcel({
    activityName: activity.value?.title,
    suffix: '报名名单',
    registrations: rows.value
  })
  ElMessage.success('报名名单已导出')
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [detail, registrations] = await Promise.all([
      getActivity(route.params.id),
      listRegistrations(route.params.id)
    ])
    activity.value = detail.activity
    rows.value = registrations
  } finally {
    loading.value = false
  }
}
</script>
