<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">活动审核</h1>
        <p class="page-subtitle">审核活动内容、时间和组织信息，审核通过后才会进入学生端活动大厅。</p>
      </div>
      <el-button @click="load">刷新列表</el-button>
    </div>

    <div class="metric-row">
      <div class="metric metric-accent"><span>待审核活动</span><strong>{{ rows.length }}</strong></div>
      <div class="metric"><span>当前范围</span><strong>全校区</strong></div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" stripe>
        <el-table-column prop="title" label="活动名称" min-width="210" />
        <el-table-column prop="campus" label="校区" width="120" />
        <el-table-column prop="location" label="地点" min-width="150" />
        <el-table-column prop="startTime" label="活动时间" min-width="170" />
        <el-table-column prop="updatedAt" label="提交时间" min-width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <RouterLink :to="`/activities/${row.id}`"><el-button size="small">查看详情</el-button></RouterLink>
              <el-button size="small" type="success" @click="approve(row)">通过</el-button>
              <el-button size="small" type="danger" plain @click="openReject(row)">驳回</el-button>
            </div>
          </template>
        </el-table-column>
        </el-table>
        <el-empty v-if="!loading && rows.length === 0" description="当前没有待审核活动" />
      </div>

    <el-dialog v-model="rejectVisible" title="驳回活动" width="min(92vw, 520px)">
      <p class="dialog-tip">请写明可执行的修改意见，负责人修改后可以再次提交审核。</p>
      <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="例如：请补充活动场地、联系人或安全说明" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="reject">确认驳回</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveActivityReview, listPendingActivityReviews, rejectActivityReview } from '../../api/activity'

const rows = ref([])
const loading = ref(false)
const submitting = ref(false)
const rejectVisible = ref(false)
const rejectReason = ref('')
const selected = ref(null)

async function load() {
  loading.value = true
  try {
    rows.value = await listPendingActivityReviews()
  } finally {
    loading.value = false
  }
}

async function approve(row) {
  await ElMessageBox.confirm(`确认审核通过“${row.title}”吗？通过后学生即可在活动大厅查看。`, '审核通过确认', {
    confirmButtonText: '确认通过',
    cancelButtonText: '暂不处理',
    type: 'success'
  })
  await approveActivityReview(row.id)
  ElMessage.success('活动已审核通过')
  await load()
}

function openReject(row) {
  selected.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function reject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  submitting.value = true
  try {
    await rejectActivityReview(selected.value.id, { reason: rejectReason.value.trim() })
    ElMessage.success('活动已驳回，负责人可修改后重新提交')
    rejectVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dialog-tip {
  margin: 0 0 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
</style>
