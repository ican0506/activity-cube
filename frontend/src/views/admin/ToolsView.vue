<template>
  <section>
    <div class="lite-page-head">
      <div>
        <span class="section-eyebrow">活动工具箱</span>
        <h1>抽签分组工具箱</h1>
        <p>从报名名单或签到名单中抽签，也可以按组数或每组人数随机分组。</p>
      </div>
      <RouterLink to="/admin/activities">
        <el-button>返回活动管理</el-button>
      </RouterLink>
    </div>

    <div class="tool-layout">
      <div class="panel toolbox-card">
        <div class="lottery-badge">
          <el-icon><MagicStick /></el-icon>
          <span>随机抽签</span>
        </div>
        <h2>现场互动抽取</h2>
        <p>适合课堂互动、现场点名、志愿岗位快速抽取，让活动现场更有参与感。</p>
        <div class="toolbar tool-toolbar">
          <el-select v-model="drawForm.source" style="width: 160px">
            <el-option label="报名名单" value="REGISTRATION" />
            <el-option label="已签到名单" value="CHECKIN" />
          </el-select>
          <el-input-number v-model="drawForm.count" :min="1" />
          <el-button type="primary" :loading="drawLoading" @click="runDraw">开始抽取</el-button>
        </div>

        <div v-if="drawRows.length" class="winner-grid">
          <div v-for="member in drawRows" :key="participantKey(member)" class="winner-card highlight-card">
            <span class="wheat-badge">中奖同学</span>
            <strong>{{ participantName(member) }}</strong>
            <p>{{ member.studentNo || '学号待补充' }}</p>
            <small>{{ member.college || '学院待补充' }} · {{ member.campus || '校区待补充' }}</small>
          </div>
        </div>
        <el-empty v-else description="暂无抽签结果" />
      </div>

      <div class="panel toolbox-card">
        <div class="lottery-badge">
          <el-icon><Connection /></el-icon>
          <span>随机分组</span>
        </div>
        <h2>实践协作分组</h2>
        <p>适合实践课、志愿服务、社团活动现场快速组队，减少人工分配成本。</p>
        <div class="toolbar tool-toolbar">
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
          <el-button type="primary" :loading="groupLoading" @click="runGroup">生成分组</el-button>
        </div>
        <div v-if="groupRows.length" class="group-grid">
          <div v-for="(groupItem, index) in groupRows" :key="index" class="group-card">
            <span class="wheat-badge">第 {{ index + 1 }} 组</span>
            <h3>共 {{ groupItem.length }} 人</h3>
            <div class="group-members">
              <span v-for="member in groupItem" :key="participantKey(member)">
                {{ participantName(member) }} · {{ member.studentNo || '学号待补充' }}
              </span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无分组结果" />
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Connection, MagicStick } from '@element-plus/icons-vue'
import { draw, group } from '../../api/tool'

const route = useRoute()
const drawRows = ref([])
const groupRows = ref([])
const drawLoading = ref(false)
const groupLoading = ref(false)
const drawForm = reactive({ source: 'REGISTRATION', count: 3 })
const groupForm = reactive({ source: 'REGISTRATION', mode: 'BY_GROUP_COUNT', groupCount: 4, groupSize: 5 })

function participantName(member) {
  return member.realName || member.name || '同学'
}

function participantKey(member) {
  return member.id || member.userId || member.registrationId || `${participantName(member)}-${member.studentNo || member.phone || member.campus || ''}`
}

async function runDraw() {
  drawLoading.value = true
  try {
    drawRows.value = await draw(route.params.id, drawForm)
  } finally {
    drawLoading.value = false
  }
}

async function runGroup() {
  groupLoading.value = true
  try {
    groupRows.value = await group(route.params.id, groupForm)
  } finally {
    groupLoading.value = false
  }
}
</script>
