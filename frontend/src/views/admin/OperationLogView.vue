<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">操作日志</h1>
        <p class="page-subtitle">记录账号授权、审核处理、人工补签、名单导出和签到二维码刷新等关键操作。</p>
      </div>
      <el-button @click="load">刷新日志</el-button>
    </div>

    <div class="panel operation-filter">
      <el-form inline @submit.prevent>
        <el-form-item label="操作类型">
          <el-select v-model="filters.operation" clearable placeholder="全部操作" style="width: 190px">
            <el-option v-for="item in operationOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人"><el-input v-model="filters.username" clearable placeholder="账号或姓名" /></el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker v-model="filters.timeRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" />
        </el-form-item>
        <el-button type="primary" @click="search">筛选</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" stripe>
        <el-table-column prop="createdAt" label="操作时间" min-width="170" />
        <el-table-column prop="username" label="操作人" min-width="120" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column label="操作类型" min-width="150">
          <template #default="{ row }"><el-tag effect="light">{{ operationText(row.operation) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="targetType" label="目标类型" width="110" />
        <el-table-column prop="targetId" label="目标ID" width="100" />
        <el-table-column prop="detail" label="操作详情" min-width="260" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" min-width="120" />
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" description="暂无符合条件的操作日志" />
      <div class="pager-wrap">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :total="pagination.total" layout="total, prev, pager, next" @current-change="load" />
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listOperationLogs } from '../../api/operationLog'

const rows = ref([])
const loading = ref(false)
const filters = reactive({ operation: '', username: '', timeRange: [] })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const operationOptions = [
  { value: 'create_organizer', label: '创建负责人账号' },
  { value: 'update_user_status', label: '启用/禁用用户' },
  { value: 'submit_activity_review', label: '提交活动审核' },
  { value: 'approve_activity', label: '审核通过活动' },
  { value: 'reject_activity', label: '驳回活动' },
  { value: 'cancel_activity', label: '取消活动' },
  { value: 'manual_checkin', label: '人工补签' },
  { value: 'export_registrations', label: '导出报名名单' },
  { value: 'export_checkins', label: '导出签到名单' },
  { value: 'refresh_checkin_qr', label: '刷新签到二维码' }
]

async function load() {
  loading.value = true
  try {
    const result = await listOperationLogs({
      operation: filters.operation || undefined,
      username: filters.username || undefined,
      startTime: filters.timeRange?.[0],
      endTime: filters.timeRange?.[1],
      page: pagination.page,
      size: pagination.size
    })
    rows.value = result.records || []
    pagination.total = result.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.page = 1
  load()
}

function reset() {
  filters.operation = ''
  filters.username = ''
  filters.timeRange = []
  pagination.page = 1
  load()
}

function operationText(value) {
  return operationOptions.find((item) => item.value === value)?.label || value
}

onMounted(load)
</script>

<style scoped>
.operation-filter {
  margin-bottom: 16px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 18px;
}

@media (max-width: 768px) {
  .operation-filter :deep(.el-form--inline .el-form-item) {
    display: flex;
    margin-right: 0;
  }

  .operation-filter :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
