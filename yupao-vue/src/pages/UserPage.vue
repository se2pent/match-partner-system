<template>
  <template v-if="user">
    <van-cell title="当前用户" :value="user?.userName" />
    <van-cell title="修改信息" is-link to="/user/update" />
    <van-cell title="我创建的队伍" is-link to="/user/team/create" />
    <van-cell title="我加入的队伍" is-link to="/user/team/join" />
    <van-cell title="登出" is-link @click="logout" />
  </template>
</template>

<script setup lang="ts">
import {useRouter} from "vue-router";
import {onMounted, ref} from "vue";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";
import {getCurrentUser} from "../services/user";


const user = ref();

const router = useRouter();

onMounted(async () => {
  user.value = await getCurrentUser();
  console.log("username",user.value)
})

const toEdit = (editKey: string, editName: string, currentValue: string) => {
  router.push({
    path: '/user/edit',
    query: {
      editKey,
      editName,
      currentValue,
    }
  })
}

const logout=async ()=>{
  const logoutUser=await myAxios.post('/user/logout');
  location.href = location.href;
}
</script>

<style scoped>

</style>
