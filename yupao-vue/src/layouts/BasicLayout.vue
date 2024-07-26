<template>
  <van-nav-bar
      :title="title"
      left-text=""
      left-arrow
      @click-left="onClickLeft"
      @click-right="onClickRight"
  >
    <template #right>
      <van-icon name="search" size="18" />
    </template>
  </van-nav-bar>
<div id="content">
  <router-view/>

</div>

  <van-tabbar route @change="onChange">
    <van-tabbar-item to="/index" icon="home-o" name="index">主页</van-tabbar-item>
    <van-tabbar-item to="/team" icon="search" name="team">队伍</van-tabbar-item>
    <van-tabbar-item to="/user" icon="friends-o" name="user">用户</van-tabbar-item>
  </van-tabbar>


</template>

<script setup lang="ts">
import {ref} from "vue";
import {Toast} from "vant";
import {useRouter} from "vue-router";
import routes from "../config/routes.js";
const router = useRouter();
const DEFAULT_TITLE = '伙伴匹配';
const title = ref(DEFAULT_TITLE);
const onClickLeft = () => router.back();
const onClickRight = () => router.push('/search');


router.beforeEach((to, from) => {
  const toPath = to.path;
  const route = routes.find((route) => {
    return toPath == route.path;
  })
  title.value = route?.title ?? DEFAULT_TITLE;
})
</script>

<style scoped>
#content{
  padding-bottom: 50px;
}
</style>