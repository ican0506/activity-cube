<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">报名名单</h1>
        <p class="page-subtitle">查看报名数据，并导出 CSV 名单。</p>
      </div>
      <el-button type="primary" @click="download">导出报名名单</el-button>
    </div>
    <div class="panel">
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
import { listRegistrations } from '../../api/registration'
import { downloadExport } from '../../api/export'

const route = useRoute()
const rows = ref([])

function download() {
  downloadExport(route.params.id, 'registrations')
}

onMounted(async () => {
  rows.value = await listRegistrations(route.params.id)
})
</script>
