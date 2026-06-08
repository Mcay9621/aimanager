<template>
  <el-pagination
    v-model:current-page="modelValue.currentPage || modelValue.pageNum"
    v-model:page-size="modelValue.pageSize"
    :total="modelValue.total"
    :page-sizes="pageSizes"
    layout="total, sizes, prev, pager, next, jumper"
    background
    @size-change="handleChange"
    @current-change="handleChange"
  />
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Object,
    required: true
  },
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50, 100]
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const handleChange = () => {
  emit('change')
}
</script>
