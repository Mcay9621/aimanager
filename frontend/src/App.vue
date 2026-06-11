<template>
  <el-config-provider :locale="elLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useLocaleStore } from './stores/locale'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import en from 'element-plus/dist/locale/en.mjs'

const localeStore = useLocaleStore()

const elLocale = computed(() => localeStore.isZhCN ? zhCn : en)
</script>

<style>
@import './assets/styles/theme.css';

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
</style>
